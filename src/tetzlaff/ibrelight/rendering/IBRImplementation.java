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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthAttachmentSpec;
import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.*;
import tetzlaff.ibrelight.core.*;
import tetzlaff.ibrelight.rendering.IBRResources.Builder;
import tetzlaff.ibrelight.rendering.components.*;
import tetzlaff.interactive.InitializationException;
import tetzlaff.models.*;
import tetzlaff.util.AbstractImage;
import tetzlaff.util.ArrayBackedImage;
import tetzlaff.util.EnvironmentMap;

public class IBRImplementation<ContextType extends Context<ContextType>> implements IBRRenderable<ContextType>
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

    private boolean newEnvironmentDataAvailable;
    private EnvironmentMap newEnvironmentData;
    private boolean environmentMapUnloadRequested = false;
    private File currentEnvironmentFile;
    private long environmentLastModified;
    private final Object loadEnvironmentLock = new Object();

    @SuppressWarnings("FieldCanBeLocal")
    private volatile File desiredEnvironmentFile;

    private boolean newBackplateDataAvailable;
    private BufferedImage newBackplateData;
    private boolean backplateUnloadRequested = false;
    private File currentBackplateFile;
    private long backplateLastModified;
    private final Object loadBackplateLock = new Object();

    private boolean newLuminanceEncodingDataAvailable;
    private double[] newLinearLuminanceValues;
    private byte[] newEncodedLuminanceValues;

    private boolean newLightCalibrationAvailable;
    private Vector3 newLightCalibration;

    @SuppressWarnings("FieldCanBeLocal")
    private volatile File desiredBackplateFile;

    private final LightingResources<ContextType> lightingResources;

    private IBRSubject<ContextType> ibrSubject;
    private LightVisuals<ContextType> lightVisuals;
    private final List<RenderedComponent<ContextType>> otherComponents = new ArrayList<>();

    private final SceneViewportModel<ContextType> sceneViewportModel;

    private static final int SHADING_FRAMEBUFFER_COUNT = 2;
    private final Collection<FramebufferObject<ContextType>> shadingFramebuffers = new ArrayList<>(SHADING_FRAMEBUFFER_COUNT);

    IBRImplementation(String id, ContextType context, Builder<ContextType> resourceBuilder)
    {
        this.id = id;
        this.context = context;
        this.resourceBuilder = resourceBuilder;

        this.sceneModel = new SceneModel();

        this.sceneViewportModel = new SceneViewportModel<>(sceneModel);
        this.sceneViewportModel.addSceneObjectType("SceneObject");

        this.lightingResources = new LightingResources<>(context, sceneModel);
    }

    @Override
    public IBRResources<ContextType> getResources()
    {
        return this.resources;
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

            // i.e. shadow map, environment map, etc.
            lightingResources.initialize();
            lightingResources.setPositionBuffer(resources.positionBuffer);

            // the actual subject for image-based rendering
            ibrSubject = new IBRSubject<>(resources, lightingResources, sceneModel, sceneViewportModel);
            ibrSubject.initialize();

            // graphics resources for depicting the on-screen representation of lights
            lightVisuals = new LightVisuals<>(context, sceneModel, sceneViewportModel);
            lightVisuals.initialize();

            // Backplate and environment must be first since they aren't depth tested.
            otherComponents.add(new Backplate<>(context, lightingResources, sceneModel));
            otherComponents.add(new Environment<>(context, lightingResources, sceneModel, sceneViewportModel));

            // Foreground components that will be depth tested
            otherComponents.add(new Grid<>(context, sceneModel));
            otherComponents.add(new GroundPlane<>(resources, lightingResources, sceneModel, sceneViewportModel));

            // Run initialization for each additional component
            for (RenderedComponent<ContextType> component : otherComponents)
            {
                component.initialize();
            }

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
            Matrix4 projection = getProjectionMatrix(windowSize);
            ibrSubject.draw(firstShadingFBO, true, sceneModel.getCurrentViewMatrix(), projection);

            // Flush to prevent timeout
            context.flush();

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
            ibrSubject.update();
            lightVisuals.update();

            for (RenderedComponent<ContextType> component : otherComponents)
            {
                component.update();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        this.updateWorldSpaceDefinition();

        if (this.environmentMapUnloadRequested)
        {
            lightingResources.takeEnvironmentMap(null);
            this.environmentMapUnloadRequested = false;
        }

        if (this.backplateUnloadRequested)
        {
            lightingResources.takeBackplateTexture(null);
            this.backplateUnloadRequested = false;
        }

        if (this.newEnvironmentDataAvailable)
        {
            try
            {
                Cubemap<ContextType> newEnvironmentTexture = null;

                synchronized(loadEnvironmentLock)
                {
                    if (this.newEnvironmentData != null)
                    {
                        EnvironmentMap environmentData = this.newEnvironmentData;
                        this.newEnvironmentData = null;

                        float[][] sides = environmentData.getData();

                        newEnvironmentTexture = context.getTextureFactory().buildColorCubemap(environmentData.getSide())
                            .loadFace(CubemapFace.POSITIVE_X, NativeVectorBufferFactory.getInstance().createFromFloatArray(3,
                                sides[EnvironmentMap.PX].length / 3, sides[EnvironmentMap.PX]))
                            .loadFace(CubemapFace.NEGATIVE_X, NativeVectorBufferFactory.getInstance().createFromFloatArray(3,
                                sides[EnvironmentMap.NX].length / 3, sides[EnvironmentMap.NX]))
                            .loadFace(CubemapFace.POSITIVE_Y, NativeVectorBufferFactory.getInstance().createFromFloatArray(3,
                                sides[EnvironmentMap.PY].length / 3, sides[EnvironmentMap.PY]))
                            .loadFace(CubemapFace.NEGATIVE_Y, NativeVectorBufferFactory.getInstance().createFromFloatArray(3,
                                sides[EnvironmentMap.NY].length / 3, sides[EnvironmentMap.NY]))
                            .loadFace(CubemapFace.POSITIVE_Z, NativeVectorBufferFactory.getInstance().createFromFloatArray(3,
                                sides[EnvironmentMap.PZ].length / 3, sides[EnvironmentMap.PZ]))
                            .loadFace(CubemapFace.NEGATIVE_Z, NativeVectorBufferFactory.getInstance().createFromFloatArray(3,
                                sides[EnvironmentMap.NZ].length / 3, sides[EnvironmentMap.NZ]))
                            .setInternalFormat(ColorFormat.RGB32F)
                            .setMipmapsEnabled(true)
                            .setLinearFilteringEnabled(true)
                            .createTexture();

                        newEnvironmentTexture.setTextureWrap(TextureWrapMode.Repeat, TextureWrapMode.None);
                    }
                }

                if (newEnvironmentTexture != null)
                {
                    lightingResources.takeEnvironmentMap(newEnvironmentTexture);
                }
            }
            catch (RuntimeException e)
            {
                e.printStackTrace();
            }
            finally
            {
                this.newEnvironmentDataAvailable = false;
                this.loadingMonitor.loadingComplete();
            }
        }

        if (this.newBackplateDataAvailable)
        {
            try
            {
                Texture2D<ContextType> newBackplateTexture = null;

                synchronized(loadBackplateLock)
                {
                    if (this.newBackplateData != null)
                    {
                        BufferedImage backplateData = this.newBackplateData;
                        this.newBackplateData = null;

                        newBackplateTexture = context.getTextureFactory().build2DColorTextureFromImage(backplateData, true)
                            .setInternalFormat(CompressionFormat.RGB_PUNCHTHROUGH_ALPHA1_4BPP)
                            .setLinearFilteringEnabled(true)
                            .setMipmapsEnabled(true)
                            .createTexture();
                    }
                }

                if (newBackplateTexture != null)
                {
                    lightingResources.takeBackplateTexture(newBackplateTexture);
                }
            }
            catch (RuntimeException e)
            {
                e.printStackTrace();
            }
            finally
            {
                this.newBackplateDataAvailable = false;
            }
        }

        if (this.newLuminanceEncodingDataAvailable)
        {
            this.getActiveViewSet().setTonemapping(
                this.getActiveViewSet().getGamma(),
                this.newLinearLuminanceValues,
                this.newEncodedLuminanceValues);

            this.resources.updateLuminanceMap();

            this.newLightCalibrationAvailable = false;
        }

        if (this.newLightCalibrationAvailable)
        {
            for (int i = 0; i < resources.viewSet.getLightCount(); i++)
            {
                this.getActiveViewSet().setLightPosition(i, newLightCalibration);
            }

            this.resources.updateLightData();
            this.newLightCalibrationAvailable = false;
        }
    }

    @Override
    public Optional<Cubemap<ContextType>> getEnvironmentMap()
    {
        return sceneModel.getLightingModel().isEnvironmentMappingEnabled() ? Optional.ofNullable(lightingResources.getEnvironmentMap()) : Optional.empty();
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

    @Override
    public void draw(Framebuffer<ContextType> framebuffer)
    {
        FramebufferSize framebufferSize = framebuffer.getSize();
        draw(framebuffer, null, null, framebufferSize.width, framebufferSize.height);
    }

    @Override
    public void draw(Framebuffer<ContextType> framebuffer, Matrix4 viewOverride, Matrix4 projectionOverride)
    {
        FramebufferSize framebufferSize = framebuffer.getSize();
        this.draw(framebuffer, viewOverride, projectionOverride, framebufferSize.width, framebufferSize.height);
    }

    @Override
    public void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        this.newLinearLuminanceValues = linearLuminanceValues;
        this.newEncodedLuminanceValues = encodedLuminanceValues;
        this.newLuminanceEncodingDataAvailable = true;
    }

    @Override
    public void applyLightCalibration()
    {
        this.newLightCalibration = resources.viewSet.getLightPosition(resources.viewSet.getLightIndex(resources.viewSet.getPrimaryViewIndex()))
            .plus(sceneModel.getSettingsModel().get("currentLightCalibration", Vector2.class).asVector3());
        this.newLightCalibrationAvailable = true;
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
                boolean lightCalibrationMode = sceneModel.getSettingsModel().getBoolean("lightCalibrationMode");
                int snapViewIndex = -1;

                boolean overriddenViewMatrix = modelViewOverride != null;

                Matrix4 lightCalibrationView = null;
                if (lightCalibrationMode)
                {
                    int primaryLightIndex = this.resources.viewSet.getLightIndex(this.resources.viewSet.getPrimaryViewIndex());

                    Vector3 lightPosition = sceneModel.getSettingsModel().get("currentLightCalibration", Vector2.class).asVector3()
                            .plus(resources.viewSet.getLightPosition(primaryLightIndex));
                    Matrix4 lightTransform = Matrix4.translate(lightPosition.negated());

                    Matrix4 viewInverse = sceneModel.getCurrentViewMatrix().quickInverse(0.01f);
                    float maxSimilarity = -1.0f;

                    for(int i = 0; i < this.resources.viewSet.getCameraPoseCount(); i++)
                    {
                        Matrix4 candidateView = this.resources.viewSet.getCameraPose(i);

                        float similarity = viewInverse.times(Vector4.ORIGIN).getXYZ()
                                .dot(sceneModel.getViewMatrixFromCameraPose(candidateView).quickInverse(0.01f).times(Vector4.ORIGIN).getXYZ());

                        if (similarity > maxSimilarity)
                        {
                            maxSimilarity = similarity;
                            lightCalibrationView = lightTransform.times(sceneModel.getViewMatrixFromCameraPose(candidateView));
                            snapViewIndex = i;
                        }
                    }
                }

                Matrix4 view = overriddenViewMatrix ? sceneModel.getViewFromModelViewMatrix(modelViewOverride)
                        : lightCalibrationMode ? lightCalibrationView
                        : sceneModel.getCurrentViewMatrix();

                lightingResources.refreshShadowMaps();

                // Screen space depth buffer for specular shadows
                // Not used for light calibration so assume a model transform should be applied.
                lightingResources.refreshScreenSpaceDepthFBO(true, view, projection);

                context.flush();

                // Draw "other components" first, which includes things that ignore the depth test first
                // (environment or backplate)
                otherComponents.forEach(component -> component.draw(offscreenFBO, true, view, projection));

                // Hole fill color depends on whether in light calibration mode or not.
                if (lightCalibrationMode)
                {
                    ibrSubject.getProgram().setUniform("holeFillColor", new Vector3(0.5f));

                    try(UniformBuffer<ContextType> viewIndexBuffer = context.createUniformBuffer())
                    {
                        viewIndexBuffer.setData(NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, 1, snapViewIndex));
                        ibrSubject.getProgram().setUniformBuffer("ViewIndices", viewIndexBuffer);

                        // Draw the actual object, without model transformation for light calibration
                        drawComponentInSubdivisions(ibrSubject, offscreenFBO, subdivWidth, subdivHeight, false, view, projection);
                    }
                }
                else
                {
                    ibrSubject.getProgram().setUniform("holeFillColor", new Vector3(0.0f));

                    // Draw the actual object with the model transformation
                    drawComponentInSubdivisions(ibrSubject, offscreenFBO, subdivWidth, subdivHeight, true, view, projection);
                }

                if (!sceneModel.getLightingModel().areLightWidgetsEthereal()
                    && IntStream.range(0, sceneModel.getLightingModel().getLightCount()).anyMatch(sceneModel.getLightingModel()::isLightWidgetEnabled))
                {
                    context.flush();

                    // Read buffers here if light widgets are ethereal (i.e. they cannot be clicked and should not be in the ID buffer)
                    sceneViewportModel.refreshBuffers(projection, offscreenFBO);
                }

                lightVisuals.draw(offscreenFBO, true, view, projection);

                // Finish drawing
                context.flush();

                if (!sceneModel.getLightingModel().areLightWidgetsEthereal()
                        && IntStream.range(0, sceneModel.getLightingModel().getLightCount()).anyMatch(sceneModel.getLightingModel()::isLightWidgetEnabled))
                {
                    // Read buffers here if light widgets are not ethereal (i.e. they can be clicked and should be in the ID buffer)
                    sceneViewportModel.refreshBuffers(projection, offscreenFBO);
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

    private void drawComponentInSubdivisions(RenderedComponent<ContextType> component, Framebuffer<ContextType> framebuffer,
                                         int subdivWidth, int subdivHeight, boolean useModel, Matrix4 view, Matrix4 projection)
    {
        FramebufferSize fullFBOSize = framebuffer.getSize();

        // Optionally render in subdivisions to prevent GPU timeout
        for (int x = 0; x < fullFBOSize.width; x += subdivWidth)
        {
            for (int y = 0; y < fullFBOSize.height; y += subdivHeight)
            {
                int effectiveWidth = Math.min(subdivWidth, fullFBOSize.width - x);
                int effectiveHeight = Math.min(subdivHeight, fullFBOSize.height - y);

                float scaleX = (float)fullFBOSize.width / (float)effectiveWidth;
                float scaleY = (float)fullFBOSize.height / (float)effectiveHeight;
                float centerX = (2 * x + effectiveWidth - fullFBOSize.width) / (float)fullFBOSize.width;
                float centerY = (2 * y + effectiveHeight - fullFBOSize.height) / (float)fullFBOSize.height;

                Matrix4 viewportProjection = Matrix4.scale(scaleX, scaleY, 1.0f)
                        .times(Matrix4.translate(-centerX, -centerY, 0))
                        .times(projection);

                // Render to off-screen buffer
                component.draw(framebuffer, new CameraViewport(useModel, view, projection, viewportProjection, x, y, effectiveWidth, effectiveHeight));

                // Flush to prevent timeout
                context.flush();
            }
        }
    }


    @Override
    public void close()
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

        if (ibrSubject != null)
        {
            ibrSubject.close();
        }

        if (lightVisuals != null)
        {
            lightVisuals.close();
        }

        for (RenderedComponent<ContextType> otherComponent : otherComponents)
        {
            try
            {
                otherComponent.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        for (FramebufferObject<ContextType> fbo : shadingFramebuffers)
        {
            fbo.close();
        }

        shadingFramebuffers.clear();
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

    private AbstractImage currentEnvironmentMap;

    @Override
    public Optional<AbstractImage> loadEnvironmentMap(File environmentFile) throws FileNotFoundException
    {
        if (environmentFile == null)
        {
            if (this.lightingResources.getEnvironmentMap() != null)
            {
                this.environmentMapUnloadRequested = true;
            }

            currentEnvironmentMap = null;
            return Optional.empty();
        }
        else if (environmentFile.exists())
        {
            System.out.println("Loading new environment texture.");

            this.desiredEnvironmentFile = environmentFile;
            long lastModified = environmentFile.lastModified();
            boolean readCompleted = false;

            int width = 0;
            int height = 0;
            float[] pixels = null;

            synchronized(loadEnvironmentLock)
            {
                if (Objects.equals(environmentFile, desiredEnvironmentFile) &&
                    (!Objects.equals(environmentFile, currentEnvironmentFile) || lastModified != environmentLastModified))
                {
                    this.loadingMonitor.startLoading();
                    this.loadingMonitor.setMaximum(0.0);

                    try
                    {
                        // Use Michael Ludwig's code to convert to a cube map (supports either cross or panorama input)
                        this.newEnvironmentData = EnvironmentMap.createFromHDRFile(environmentFile);
                        this.currentEnvironmentFile = environmentFile;
                        width = newEnvironmentData.getSide() * 4;
                        height = newEnvironmentData.getSide() * 2;
                        pixels = EnvironmentMap.toPanorama(newEnvironmentData.getData(), newEnvironmentData.getSide(), width, height);
                        readCompleted = true;
                    }
                    catch (FileNotFoundException e)
                    {
                        throw e;
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            this.newEnvironmentDataAvailable = this.newEnvironmentDataAvailable || readCompleted;

            if (readCompleted)
            {
                environmentLastModified = lastModified;
                currentEnvironmentMap = new ArrayBackedImage(width, height, pixels);
            }

            return Optional.ofNullable(currentEnvironmentMap);
        }
        else
        {
            throw new FileNotFoundException(environmentFile.getPath());
        }
    }

    @Override
    public void loadBackplate(File backplateFile) throws FileNotFoundException
    {
        if (backplateFile == null && lightingResources.getBackplateTexture() != null)
        {
            this.backplateUnloadRequested = true;
        }
        else if (backplateFile != null && backplateFile.exists())
        {
            System.out.println("Loading new backplate texture.");

            this.desiredBackplateFile = backplateFile;
            long lastModified = backplateFile.lastModified();
            boolean readCompleted = false;

            synchronized(loadBackplateLock)
            {
                if (Objects.equals(backplateFile, desiredBackplateFile) &&
                    (!Objects.equals(backplateFile, currentBackplateFile) || lastModified != backplateLastModified))
                {
                    try
                    {
                        this.newBackplateData = ImageIO.read(backplateFile);
                        this.currentBackplateFile = backplateFile;
                        readCompleted = true;
                    }
                    catch (FileNotFoundException e)
                    {
                        throw e;
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            this.newBackplateDataAvailable = this.newBackplateDataAvailable || readCompleted;

            if (readCompleted)
            {
                backplateLastModified = lastModified;
            }
        }
        else if (backplateFile != null)
        {
            throw new FileNotFoundException(backplateFile.getPath());
        }
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
            ibrSubject.reloadShaders();
            lightVisuals.reloadShaders();

            for (RenderedComponent<ContextType> otherComponent : otherComponents)
            {
                otherComponent.reloadShaders();
            }

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
