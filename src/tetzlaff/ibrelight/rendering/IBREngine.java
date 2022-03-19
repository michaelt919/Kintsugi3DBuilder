/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.rendering;

import java.io.File;
import java.util.*;

import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthAttachmentSpec;
import tetzlaff.gl.core.*;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.*;
import tetzlaff.ibrelight.core.*;
import tetzlaff.ibrelight.rendering.IBRResources.Builder;
import tetzlaff.ibrelight.rendering.components.*;
import tetzlaff.ibrelight.rendering.components.lit.LitRoot;
import tetzlaff.interactive.InitializationException;
import tetzlaff.models.*;

public class IBREngine<ContextType extends Context<ContextType>> implements IBRInstance<ContextType>
{
    private final ContextType context;

    private volatile LoadingMonitor loadingMonitor;
    private boolean suppressErrors = false;

    private final Builder<ContextType> resourceBuilder;
    private IBRResources<ContextType> resources;

    private VertexBuffer<ContextType> rectangleVertices;

    private final String id;

    private final SceneModel sceneModel;

    private Program<ContextType> simpleTexProgram;
    private Drawable<ContextType> simpleTexDrawable;

    private LightCalibration<ContextType> lightCalibration;
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
    public IBRResources<ContextType> getIBRResources()
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

            this.resources = resourceBuilder.create();
            context.flush();

            if (this.loadingMonitor != null)
            {
                this.loadingMonitor.setMaximum(0.0); // make indeterminate
            }

            this.simpleTexDrawable = context.createDrawable(simpleTexProgram);
            this.simpleTexDrawable.addVertexBuffer("position", this.rectangleVertices);

            lightCalibration = new LightCalibration<>(resources, sceneModel, sceneViewportModel);
            lightCalibration.initialize();

            litRoot = new LitRoot<>(context, sceneModel);
            litRoot.takeLitContentRoot(new StandardScene<>(resources, sceneModel, sceneViewportModel));
            litRoot.initialize();
            litRoot.setShadowCaster(resources.positionBuffer);

            this.dynamicResourceLoader = new DynamicResourceLoader<>(loadingMonitor, resources, litRoot.getLightingResources());

            this.updateWorldSpaceDefinition();

            FramebufferSize windowSize = context.getDefaultFramebuffer().getSize();
            FramebufferObject<ContextType> firstShadingFBO =
                context.buildFramebufferObject(windowSize.width, windowSize.height)
                    .addColorAttachment(
                        ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB8)
                            .setLinearFilteringEnabled(true))
                    .addDepthAttachment()
                    .createFramebufferObject();

            shadingFramebuffers.add(firstShadingFBO);

            // Render an entire frame to an offscreen framebuffer before announcing that loading is complete.
            // TODO break this into blocks just in case there's a GPU timeout?
//            litRoot.draw(firstShadingFBO, sceneModel.getCurrentViewMatrix(), getProjectionMatrix(windowSize));
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
            e.printStackTrace();
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
            e.printStackTrace();
        }

        this.updateWorldSpaceDefinition();
    }

    private void updateWorldSpaceDefinition()
    {
        sceneModel.setScale(resources.geometry.getBoundingRadius() * 2);
        sceneModel.setOrientation(resources.viewSet.getCameraPose(resources.viewSet.getPrimaryViewIndex()).getUpperLeft3x3());
        sceneModel.setCentroid(resources.geometry.getCentroid());
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
        float maxLuminance = (float)resources.viewSet.getLuminanceEncoding().decodeFunction.applyAsDouble(255.0);
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
                e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    @Override
    public void setLoadingMonitor(LoadingMonitor loadingMonitor)
    {
        this.loadingMonitor = loadingMonitor;
    }

    @Override
    public VertexGeometry getActiveGeometry()
    {
        return this.resources.geometry;
    }

    @Override
    public ViewSet getActiveViewSet()
    {
        return this.resources.viewSet;
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
            e.printStackTrace();
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
