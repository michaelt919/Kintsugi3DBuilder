/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.rendering;

import kintsugi3d.builder.core.*;
import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.io.gltf.ModelExporter;
import kintsugi3d.builder.rendering.components.RenderingSubject;
import kintsugi3d.builder.rendering.components.StandardScene;
import kintsugi3d.builder.rendering.components.lightcalibration.LightCalibration3DScene;
import kintsugi3d.builder.rendering.components.lightcalibration.LightCalibrationRoot;
import kintsugi3d.builder.rendering.components.lit.LitRoot;
import kintsugi3d.builder.rendering.components.snap.ViewSelection;
import kintsugi3d.builder.rendering.components.snap.ViewSelectionImpl;
import kintsugi3d.builder.rendering.components.split.SplitScreenComponent;
import kintsugi3d.builder.resources.DynamicResourceLoader;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace.Builder;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.builder.state.SceneViewport;
import kintsugi3d.gl.builders.framebuffer.ColorAttachmentSpec;
import kintsugi3d.gl.builders.framebuffer.DepthAttachmentSpec;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.geometry.ReadonlyVertexGeometry;
import kintsugi3d.gl.interactive.InitializationException;
import kintsugi3d.gl.interactive.InteractiveRenderableBase;
import kintsugi3d.gl.vecmath.Matrix3;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.SRGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class ProjectRenderingEngine<ContextType extends Context<ContextType>>
    extends InteractiveRenderableBase<ContextType> implements ProjectInstance<ContextType>
{
    private static final Logger LOG = LoggerFactory.getLogger(ProjectRenderingEngine.class);

    private final ContextType context;

    private volatile ProgressMonitor progressMonitor;
    private boolean suppressErrors = false;

    private final Builder<ContextType> resourceBuilder;
    private GraphicsResourcesImageSpace<ContextType> resources;
    private final boolean managedResources;

    private VertexBuffer<ContextType> rectangleVertices;

    private final String id;

    private final SceneModel sceneModel;

    private ProgramObject<ContextType> simpleTexProgram;
    private Drawable<ContextType> simpleTexDrawable;

    private SplitScreenComponent<ContextType> lightCalibrationSplitScreen;
    private LightCalibrationRoot<ContextType> lightCalibration;
    private LitRoot<ContextType> litRoot;
    private LitRoot<ContextType> lightCalibration3DRoot;

    private RenderingSubject<ContextType> subject;

    private DynamicResourceLoader<ContextType> dynamicResourceLoader;
    private final SceneViewportModel sceneViewportModel;

    private static final int SHADING_FRAMEBUFFER_COUNT = 2;
    private final Collection<FramebufferObject<ContextType>> shadingFramebuffers = new ArrayList<>(SHADING_FRAMEBUFFER_COUNT);

    private boolean loaded = false;

    ProjectRenderingEngine(String id, ContextType context, Builder<ContextType> resourceBuilder)
    {
        this.id = id;
        this.context = context;
        this.resourceBuilder = resourceBuilder;
        this.managedResources = true;
        this.sceneModel = new SceneModel();
        this.sceneViewportModel = createSceneViewportModel(this.sceneModel);
    }

    ProjectRenderingEngine(String id, ContextType context, GraphicsResourcesImageSpace<ContextType> resources)
    {
        this.id = id;
        this.context = context;
        this.resources = resources;
        this.resourceBuilder = null;
        this.managedResources = false;
        this.sceneModel = new SceneModel();
        this.sceneViewportModel = createSceneViewportModel(this.sceneModel);
    }

    private static SceneViewportModel createSceneViewportModel(SceneModel sceneModel)
    {
        SceneViewportModel sceneViewportModel = new SceneViewportModel(sceneModel);
        sceneViewportModel.addSceneObjectType("SceneObject");
        return sceneViewportModel;
    }

    @Override
    public String getID()
    {
        return id;
    }

    public RenderingSubject<ContextType> getSubject()
    {
        return subject;
    }

    @Override
    public GraphicsResourcesImageSpace<ContextType> getResources()
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

            if (resourceBuilder != null)
            {
                this.resources = resourceBuilder
                    .setProgressMonitor(this.progressMonitor) // Use the progress monitor that offsets the stage count if generating preview images
//                .generateUndistortedPreviewImages()
                    .create();

                context.flush();
            }

            this.simpleTexDrawable = context.createDrawable(simpleTexProgram);
            this.simpleTexDrawable.addVertexBuffer("position", this.rectangleVertices);

            ViewSelection viewSelection = new ViewSelectionImpl(getViewSet(), sceneModel);

            lightCalibration = new LightCalibrationRoot<>(resources, sceneModel, viewSelection, sceneViewportModel);
            lightCalibration.initialize();

            litRoot = new LitRoot<>(context, sceneModel);
            StandardScene<ContextType> scene = new StandardScene<>(resources, sceneModel, sceneViewportModel);
//            scene.setLightVisualsEnabled(true); // Enable light visuals when not in light calibration mode
            litRoot.takeLitContentRoot(scene);
            litRoot.initialize();
            litRoot.setShadowCaster(resources.getGeometryResources().positionBuffer);

            lightCalibration3DRoot = new LitRoot<>(context, sceneModel);
            LightCalibration3DScene<ContextType> lightCalibScene =
                new LightCalibration3DScene<>(resources, sceneModel, sceneViewportModel, viewSelection);
            lightCalibration3DRoot.takeLitContentRoot(lightCalibScene);
            lightCalibration3DRoot.initialize();
            lightCalibration3DRoot.setShadowCaster(resources.getGeometryResources().positionBuffer);

            lightCalibrationSplitScreen = new SplitScreenComponent<>(lightCalibration, lightCalibration3DRoot);

            this.subject = scene.getSubject();
            this.dynamicResourceLoader = new DynamicResourceLoader<>(progressMonitor,
                resources, subject, litRoot.getLightingResources());

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
        }
        catch (UserCancellationException e)
        {
            LOG.error("User cancelled operation while initializing ProjectRenderingEngine:", e);
            this.close();
            if (this.progressMonitor != null)
            {
                this.progressMonitor.cancelComplete(e);
        }
            throw new InitializationException(e);
        }
        catch (RuntimeException|IOException e)
        {
            LOG.error("Error occurred initializing ProjectRenderingEngine:", e);
            this.close();
            if (this.progressMonitor != null)
            {
                this.progressMonitor.fail(e);
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
            lightCalibration3DRoot.update();
        }
        catch (RuntimeException e)
        {
            LOG.error("Error occurred during update:", e);
        }

        this.updateWorldSpaceDefinition();
    }

    private void updateWorldSpaceDefinition()
    {
        if (resources.getGeometry() != null)
        {
            ReadonlyViewSet viewSet = getViewSet();

            if (viewSet != null)
            {
                int referencePoseIndex = viewSet.getOrientationViewIndex();

                if (referencePoseIndex < 0) // check for override
                {
                    // Imported orientation and object center if no override
                    // For now, this is all that we're importing from Metashape;
                    // everything else should be the same as with a reference image override.
                    // This might change in the future.
                    sceneModel.setOrientation(Objects.requireNonNullElse(viewSet.getOrientationMatrix(), Matrix3.IDENTITY));

                    // COMMENTED OUT: Object translation doesn't seem to be that meaningful coming from Metashape.
//                    sceneModel.setCentroid(sceneModel.getOrientation().transpose()
//                        .times(Objects.requireNonNullElse(viewSet.getObjectTranslation(), Vector3.ZERO).negated()));

                    // Just use true centroid instead
                    sceneModel.setCentroid(resources.getGeometry().getCentroid());

                    // TODO figure out if we can use imported scale for user interaction without breaking things.
//                    // "Scene scale" is generally taken by the Kintsugi renderer to be world space to model space
//                    // so we need to invert the imported global scale.
//                    sceneModel.setScale(1.0f / viewSet.getObjectScale());
//                    sceneModel.setScale((resources.getGeometry().getBoundingRadius() + resources.getGeometry().getCentroid().length()) * 2);
                    sceneModel.setScale(resources.getGeometry().getBoundingRadius() * 2);
                }
                else
                {
                    // reference image based override, replaces any imported reference frame
                    // use centroid and scale based on geometry assuming the imported scale and center is invalid
                    Matrix3 referenceCameraPose = viewSet.getCameraPose(referencePoseIndex).getUpperLeft3x3();
                    sceneModel.setOrientation(Matrix3.rotateZ(Math.toRadians(-viewSet.getOrientationViewRotationDegrees()))
                        .times(referenceCameraPose));
                    sceneModel.setCentroid(resources.getGeometry().getCentroid());
                    sceneModel.setScale(resources.getGeometry().getBoundingRadius() * 2);
                }
            }
            else
            {
                // Defaults if there's no view set for some reason (identity for orientation and geometry-based centroid and scale)
                sceneModel.setOrientation(Matrix3.IDENTITY);
                sceneModel.setCentroid(resources.getGeometry().getCentroid());
                sceneModel.setScale(resources.getGeometry().getBoundingRadius() * 2);
            }
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
        float maxLuminance = (float) SRGB.fromLinear(resources.getViewSet().getLuminanceEncoding().decodeFunction.applyAsDouble(255.0));
        return new Vector3(
                sceneModel.getLightingModel().getBackgroundColor().x / maxLuminance,
                sceneModel.getLightingModel().getBackgroundColor().y / maxLuminance,
                sceneModel.getLightingModel().getBackgroundColor().z / maxLuminance);
    }

    @Override
    public void draw(Framebuffer<ContextType> framebuffer, Matrix4 modelViewOverride, Matrix4 projectionOverride,
                     int subdivWidth, int subdivHeight)
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
                    // Split needs to be updated every time as FBO width may have changed.
                    lightCalibrationSplitScreen.setSplit(0.5f, fboWidth);
                    lightCalibrationSplitScreen.drawInSubdivisions(offscreenFBO, subdivWidth, subdivHeight, view, projection);
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

                if (!loaded)
                {
                    // First frame drawn successfully.
                    loaded = true;

                    if (this.progressMonitor != null)
                    {
                        this.progressMonitor.complete();
                    }
                }
            }
        }
        catch(RuntimeException e)
        {
            if (!suppressErrors)
            {
                LOG.error("Error during draw call", e);
                suppressErrors = true; // Prevent excessive errors
            }
        }
        catch (Error e)
        {
            LOG.error("Error during draw call", e);
            //noinspection ProhibitedExceptionThrown
            throw e;
        }
    }

    @Override
    public void close()
    {
        try
        {
            if (managedResources && resources != null)
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

            if (simpleTexDrawable != null)
            {
                simpleTexDrawable.close();
                simpleTexDrawable = null;
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

            if (lightCalibration3DRoot != null)
            {
                lightCalibration3DRoot.close();
                lightCalibration3DRoot = null;
            }

            for (FramebufferObject<ContextType> fbo : shadingFramebuffers)
            {
                fbo.close();
            }

            shadingFramebuffers.clear();
        }
        catch (RuntimeException e)
        {
            LOG.error("Error closing ProjectRenderingEngine:", e);
        }
    }

    @Override
    public void setProgressMonitor(ProgressMonitor progressMonitor)
    {
        this.progressMonitor = progressMonitor;
    }

    @Override
    public ReadonlyVertexGeometry getGeometry()
    {
        return this.resources.getGeometry();
    }

    @Override
    public ViewSet getViewSet()
    {
        return this.resources.getViewSet();
    }

    @Override
    public String toString()
    {
        return this.id.length() > 32
                ? String.format("...%s", this.id.substring(this.id.length() - 31))
                : this.id;
    }

    @Override
    public void reloadShaders()
    {
        try
        {
            litRoot.reloadShaders();
            lightCalibration.reloadShaders();
            lightCalibration3DRoot.reloadShaders();

            suppressErrors = false;
        }
        catch (RuntimeException e)
        {
            LOG.error("Error reloading shaders:", e);
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

    @Override
    public void saveGLTF(File outputDirectory, String filename, ExportSettings settings, Runnable finishedCallback)
    {
        if (outputDirectory != null)
        {
            if (getGeometry() == null)
            {
                throw new IllegalArgumentException("Geometry is null; cannot export GLTF.");
            }

            LOG.info("Starting glTF export...");
            if(progressMonitor != null)
            {
                progressMonitor.setProcessName("glTF Export");
            }

            try
            {
                this.updateWorldSpaceDefinition();

                Matrix4 transform = sceneModel.getFullModelMatrix();

                // Scale to imported scale from the photogrammetry project if that exists, otherwise at the original, raw scale
                // ViewSet should default to scale of 1.0 if nothing was imported.
                ViewSet viewSet = getViewSet();
                if (viewSet != null)
                {
                    transform = Matrix4.scale(viewSet.getObjectScale()).times(transform);
                }

                TextureResources<ContextType> textureResources = resources.getTextureResources();

                ModelExporter exporter = ModelExporter.fromVertexGeometry(getGeometry(), transform);
                settings.applyToExporter(exporter, textureResources, filename);

                File modelFile = new File(outputDirectory, filename);

                if (settings.shouldSaveTextures())
                {
                    exporter.exportWithTextures(modelFile, finishedCallback);
                }
                else  // not saving textures
                {
                    exporter.exportModelOnly(modelFile);

                    if (finishedCallback != null)
                    {
                        finishedCallback.run();
                    }
                }

                LOG.info("DONE!");
            }
            catch (IOException e)
            {
                LOG.error("Error occurred during glTF export:", e);
            }
        }
    }
}
