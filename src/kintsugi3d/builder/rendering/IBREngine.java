/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.rendering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.builders.framebuffer.ColorAttachmentSpec;
import kintsugi3d.gl.builders.framebuffer.DepthAttachmentSpec;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.geometry.ReadonlyVertexGeometry;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.builder.core.*;
import kintsugi3d.builder.rendering.components.StandardScene;
import kintsugi3d.builder.resources.DynamicResourceLoader;
import kintsugi3d.builder.resources.IBRResourcesImageSpace.Builder;
import kintsugi3d.builder.rendering.components.lightcalibration.LightCalibrationRoot;
import kintsugi3d.builder.rendering.components.lit.LitRoot;
import kintsugi3d.builder.resources.IBRResourcesImageSpace;
import kintsugi3d.gl.interactive.InitializationException;
import kintsugi3d.builder.state.SceneViewport;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public class IBREngine<ContextType extends Context<ContextType>> implements IBRInstance<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(IBREngine.class);

    private final ContextType context;

    private volatile LoadingMonitor loadingMonitor;
    private boolean suppressErrors = false;

    private final Builder<ContextType> resourceBuilder;
    private IBRResourcesImageSpace<ContextType> resources;

    private VertexBuffer<ContextType> rectangleVertices;

    private final String id;

    private final SceneModel sceneModel;

    private ProgramObject<ContextType> simpleTexProgram;
    private Drawable<ContextType> simpleTexDrawable;

    private LightCalibrationRoot<ContextType> lightCalibration;
    private LitRoot<ContextType> litRoot;

    private DynamicResourceLoader<ContextType> dynamicResourceLoader;
    private final SceneViewportModel<ContextType> sceneViewportModel;

    private static final int SHADING_FRAMEBUFFER_COUNT = 2;
    private final Collection<FramebufferObject<ContextType>> shadingFramebuffers = new ArrayList<>(SHADING_FRAMEBUFFER_COUNT);

    IBREngine(String id, ContextType context, Builder<ContextType> resourceBuilder)
    {
        this.id = id;
        this.context = context;
        this.resourceBuilder = resourceBuilder;

        this.sceneModel = new SceneModel();

        this.sceneViewportModel = new SceneViewportModel<>(sceneModel);
        this.sceneViewportModel.addSceneObjectType("SceneObject");
    }

    @Override
    public IBRResourcesImageSpace<ContextType> getIBRResources()
    {
        return this.resources;
    }

    @Override
    public DynamicResourceManager getDynamicResourceManager()
    {
        return this.dynamicResourceLoader;
    }

    @Override
    public void initialize() throws InitializationException
    {
        try
        {
            this.simpleTexProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "texture.frag"))
                    .createProgram();

            this.rectangleVertices = context.createRectangle();

            this.resources = resourceBuilder
                .generateUndistortedPreviewImages()
                .create();

            context.flush();

            if (this.loadingMonitor != null)
            {
                this.loadingMonitor.setMaximum(0.0); // make indeterminate
            }

            this.simpleTexDrawable = context.createDrawable(simpleTexProgram);
            this.simpleTexDrawable.addVertexBuffer("position", this.rectangleVertices);

            lightCalibration = new LightCalibrationRoot<>(resources, sceneModel, sceneViewportModel);
            lightCalibration.initialize();

            litRoot = new LitRoot<>(context, sceneModel);
            litRoot.takeLitContentRoot(new StandardScene<>(resources, sceneModel, sceneViewportModel));
            litRoot.initialize();
            litRoot.setShadowCaster(resources.getGeometryResources().positionBuffer);

            this.dynamicResourceLoader = new DynamicResourceLoader<>(loadingMonitor, resources, litRoot.getLightingResources());

            this.updateWorldSpaceDefinition();

            FramebufferSize framebufferSize = context.getDefaultFramebuffer().getSize();
            FramebufferObject<ContextType> firstShadingFBO =
                context.buildFramebufferObject(framebufferSize.width, framebufferSize.height)
                    .addColorAttachment(
                        ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB8)
                            .setLinearFilteringEnabled(true))
                    .addDepthAttachment()
                    .createFramebufferObject();

            shadingFramebuffers.add(firstShadingFBO);

            // Render an entire frame to an offscreen framebuffer before announcing that loading is complete.
            // TODO break this into blocks just in case there's a GPU timeout?
//            litRoot.draw(firstShadingFBO, sceneModel.getCurrentViewMatrix(), getProjectionMatrix(framebufferSize));
//
//            // Flush to prevent timeout
//            context.flush();

            if (this.loadingMonitor != null)
            {
                this.loadingMonitor.loadingComplete();
            }
        }
        catch (Exception e)
        {
            log.error("Error occurred initializing IBREngine:", e);
            this.close();
            if (this.loadingMonitor != null)
            {
                this.loadingMonitor.loadingFailed(e);
            }
            throw new InitializationException(e);
        }
    }

    @Override
    public void update()
    {
        try
        {
            dynamicResourceLoader.update();
            litRoot.update();
            lightCalibration.update();
        }
        catch (Exception e)
        {
            log.error("Error occurred during update:", e);
        }

        this.updateWorldSpaceDefinition();
    }

    private void updateWorldSpaceDefinition()
    {
        if (resources.getGeometry() != null)
        {
            sceneModel.setScale(resources.getGeometry().getBoundingRadius() * 2);
            sceneModel.setOrientation(resources.getViewSet().getCameraPose(resources.getViewSet().getPrimaryViewIndex()).getUpperLeft3x3());
            sceneModel.setCentroid(resources.getGeometry().getCentroid());
        }
    }

    private Matrix4 getProjectionMatrix(FramebufferSize size)
    {
        float scale = sceneModel.getScale();

        return Matrix4.perspective(sceneModel.getVerticalFieldOfView(size),
                (float)size.width / (float)size.height,
                0.01f * scale, 100.0f * scale);
    }

    private Vector3 calculateClearColor()
    {
        float maxLuminance = (float) resources.getViewSet().getLuminanceEncoding().decodeFunction.applyAsDouble(255.0);
        float gamma = this.sceneModel.getSettingsModel().getFloat("gamma");
        return new Vector3(
                (float) Math.pow(sceneModel.getLightingModel().getBackgroundColor().x / maxLuminance, 1.0 / gamma),
                (float) Math.pow(sceneModel.getLightingModel().getBackgroundColor().y / maxLuminance, 1.0 / gamma),
                (float) Math.pow(sceneModel.getLightingModel().getBackgroundColor().z / maxLuminance, 1.0 / gamma));
    }

    @Override
    public void draw(Framebuffer<ContextType> framebuffer, Matrix4 modelViewOverride, Matrix4 projectionOverride, int subdivWidth, int subdivHeight)
    {
        try
        {
            if(this.sceneModel.getSettingsModel().getBoolean("multisamplingEnabled"))
            {
                context.getState().enableMultisampling();
            }
            else
            {
                context.getState().disableMultisampling();
            }

            context.getState().enableBackFaceCulling();

            FramebufferSize size = framebuffer.getSize();

            Matrix4 projection = projectionOverride != null ? projectionOverride : getProjectionMatrix(size);

            int fboWidth = size.width;
            int fboHeight = size.height;

            if (sceneModel.getSettingsModel().getBoolean("halfResolutionEnabled"))
            {
                fboWidth /= 2;
                fboHeight /= 2;
            }

            try
            (
                FramebufferObject<ContextType> offscreenFBO = context.buildFramebufferObject(fboWidth, fboHeight)
                        .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB8)
                            .setLinearFilteringEnabled(true))
                        .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.R8UI))
                        .addDepthAttachment(DepthAttachmentSpec.createFixedPointWithPrecision(24))
                        .createFramebufferObject()
            )
            {
                offscreenFBO.clearIntegerColorBuffer(1, 0, 0, 0, 0);
                offscreenFBO.clearDepthBuffer();

                // Calculate clear color, clear the offscreen FBO and update the clear color on the scene model
                // for components that reference it (like environment & backplate)
                Vector3 clearColor = calculateClearColor();
                offscreenFBO.clearColorBuffer(0, clearColor.x, clearColor.y, clearColor.z, 1.0f);
                this.sceneModel.setClearColor(clearColor);

                Matrix4 view = modelViewOverride != null ? sceneModel.getViewFromModelViewMatrix(modelViewOverride)
                        : sceneModel.getCurrentViewMatrix();

                if (sceneModel.getSettingsModel().getBoolean("lightCalibrationMode"))
                {
                    lightCalibration.drawInSubdivisions(offscreenFBO, subdivWidth, subdivHeight, view, projection);
                }
                else
                {
                    litRoot.drawInSubdivisions(offscreenFBO, subdivWidth, subdivHeight, view, projection);
                }

                // Second pass at full resolution to default framebuffer
                simpleTexDrawable.program().setTexture("tex", offscreenFBO.getColorAttachmentTexture(0));

                framebuffer.clearDepthBuffer();
                simpleTexDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);

                context.flush();
            }
        }
        catch(RuntimeException e)
        {
            if (!suppressErrors)
            {
                log.error("Error during draw call:", e);
                suppressErrors = true; // Prevent excessive errors
            }
        }
    }

    @Override
    public void close()
    {
        try
        {
            if (resources != null)
            {
                resources.close();
                resources = null;
            }

            if (rectangleVertices != null)
            {
                rectangleVertices.close();
                rectangleVertices = null;
            }

            if (simpleTexProgram != null)
            {
                simpleTexProgram.close();
                simpleTexProgram = null;
            }

            if (lightCalibration != null)
            {
                lightCalibration.close();
                lightCalibration = null;
            }

            if (litRoot != null)
            {
                litRoot.close();
                litRoot = null;
            }

            for (FramebufferObject<ContextType> fbo : shadingFramebuffers)
            {
                fbo.close();
            }

            shadingFramebuffers.clear();
        }
        catch (Exception e)
        {
            log.error("Error closing IBREngine:", e);
        }
    }

    @Override
    public void setLoadingMonitor(LoadingMonitor loadingMonitor)
    {
        this.loadingMonitor = loadingMonitor;
    }

    @Override
    public ReadonlyVertexGeometry getActiveGeometry()
    {
        return this.resources.getGeometry();
    }

    @Override
    public ReadonlyViewSet getActiveViewSet()
    {
        return this.resources.getViewSet();
    }

    @Override
    public String toString()
    {
        return this.id.length() > 32
                ? "..." + this.id.substring(this.id.length()-31)
                : this.id;
    }

    @Override
    public void reloadShaders()
    {
        try
        {
            litRoot.reloadShaders();
            lightCalibration.reloadShaders();

            suppressErrors = false;
        }
        catch (Exception e)
        {
            log.error("Error reloading shaders:", e);
        }
    }

    @Override
    public SceneViewport getSceneViewportModel()
    {
        return sceneViewportModel;
    }

    @Override
    public SceneModel getSceneModel()
    {
        return sceneModel;
    }
}
