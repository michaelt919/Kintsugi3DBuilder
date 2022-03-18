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
import java.util.Map.Entry;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthAttachmentSpec;
import tetzlaff.gl.core.*;
import tetzlaff.gl.core.ColorFormat.DataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.*;
import tetzlaff.ibrelight.core.*;
import tetzlaff.ibrelight.rendering.IBRResources.Builder;
import tetzlaff.ibrelight.rendering.components.Grid;
import tetzlaff.ibrelight.rendering.components.LightVisuals;
import tetzlaff.ibrelight.util.KNNViewWeightGenerator;
import tetzlaff.interactive.InitializationException;
import tetzlaff.models.*;
import tetzlaff.util.AbstractImage;
import tetzlaff.util.ArrayBackedImage;
import tetzlaff.util.EnvironmentMap;
import tetzlaff.util.ShadingParameterMode;

public class IBRImplementation<ContextType extends Context<ContextType>> implements IBRRenderable<ContextType>
{
    private final ContextType context;
    private Program<ContextType> program;

    private Program<ContextType> groundPlaneProgram;
    Drawable<ContextType> groundPlaneDrawable;

    private Program<ContextType> shadowProgram;
    private volatile LoadingMonitor loadingMonitor;
    private boolean suppressErrors = false;
    private StandardRenderingMode lastCompiledRenderingMode = StandardRenderingMode.IMAGE_BASED;

    private final Builder<ContextType> resourceBuilder;
    private IBRResources<ContextType> resources;

    private Texture3D<ContextType> shadowMaps;
    private FramebufferObject<ContextType> shadowFramebuffer;
    private Drawable<ContextType> shadowDrawable;

    private VertexBuffer<ContextType> rectangleVertices;

    private final String id;
    private Drawable<ContextType> mainDrawable;

    private final SceneModel sceneModel;

    private Vector3 clearColor;
    private Program<ContextType> simpleTexProgram;
    private Drawable<ContextType> simpleTexDrawable;
    private Program<ContextType> tintedTexProgram;
    private Drawable<ContextType> tintedTexDrawable;

    private boolean newEnvironmentDataAvailable;
    private EnvironmentMap newEnvironmentData;
    private boolean environmentMapUnloadRequested = false;
    private Cubemap<ContextType> environmentMap;
    private File currentEnvironmentFile;
    private long environmentLastModified;
    private final Object loadEnvironmentLock = new Object();

    @SuppressWarnings("FieldCanBeLocal")
    private volatile File desiredEnvironmentFile;

    private boolean newBackplateDataAvailable;
    private BufferedImage newBackplateData;
    private boolean backplateUnloadRequested = false;
    private Texture2D<ContextType> backplateTexture;
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

    private Program<ContextType> environmentBackgroundProgram;
    private Drawable<ContextType> environmentBackgroundDrawable;

    private UniformBuffer<ContextType> weightBuffer;

    private FramebufferObject<ContextType> screenSpaceDepthFBO;

    Grid<ContextType> grid;
    LightVisuals<ContextType> lightVisuals;

    SceneViewportModel<ContextType> sceneViewportModel;

    private static final int SHADING_FRAMEBUFFER_COUNT = 2;
    private final Collection<FramebufferObject<ContextType>> shadingFramebuffers = new ArrayList<>(SHADING_FRAMEBUFFER_COUNT);

    IBRImplementation(String id, ContextType context, Program<ContextType> program, Builder<ContextType> resourceBuilder)
    {
        this.id = id;
        this.context = context;
        this.program = program;
        this.resourceBuilder = resourceBuilder;

        this.clearColor = new Vector3(0.0f);

        this.sceneModel = new SceneModel();

        this.sceneViewportModel = new SceneViewportModel<>(sceneModel);
        this.sceneViewportModel.addSceneObjectType("IBRObject"); // 1
        this.sceneViewportModel.addSceneObjectType("EnvironmentMap"); // 2
        this.sceneViewportModel.addSceneObjectType("SceneObject"); // 3

        this.grid = new Grid<>(context, sceneModel);
        this.lightVisuals = new LightVisuals<>(context, sceneModel, sceneViewportModel);
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

            this.tintedTexProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "texture_tint.frag"))
                    .createProgram();

            this.environmentBackgroundProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "envbackgroundtexture.frag"))
                    .createProgram();

            this.rectangleVertices = context.createRectangle();

            this.resources = resourceBuilder.create();
            context.flush();

            if (this.loadingMonitor != null)
            {
                this.loadingMonitor.setMaximum(0.0); // make indeterminate
            }

            if (this.program == null)
            {
                this.program = loadMainProgram();
            }

            if (this.groundPlaneProgram == null)
            {
                this.groundPlaneProgram = loadMainProgram(getReferenceScenePreprocessorDefines(), StandardRenderingMode.LAMBERTIAN_SHADED);
            }

            groundPlaneDrawable = context.createDrawable(groundPlaneProgram);
            groundPlaneDrawable.addVertexBuffer("position", rectangleVertices);
            groundPlaneDrawable.setVertexAttrib("normal", new Vector3(0, 0, 1));

            this.mainDrawable = context.createDrawable(program);
            this.mainDrawable.addVertexBuffer("position", this.resources.positionBuffer);

            if (this.resources.normalBuffer != null)
            {
                this.mainDrawable.addVertexBuffer("normal", this.resources.normalBuffer);
            }

            if (this.resources.texCoordBuffer != null)
            {
                this.mainDrawable.addVertexBuffer("texCoord", this.resources.texCoordBuffer);
            }

            if (this.resources.tangentBuffer != null)
            {
                this.mainDrawable.addVertexBuffer("tangent", this.resources.tangentBuffer);
            }

            this.simpleTexDrawable = context.createDrawable(simpleTexProgram);
            this.simpleTexDrawable.addVertexBuffer("position", this.rectangleVertices);

            this.tintedTexDrawable = context.createDrawable(tintedTexProgram);
            this.tintedTexDrawable.addVertexBuffer("position", this.rectangleVertices);

            this.environmentBackgroundDrawable = context.createDrawable(environmentBackgroundProgram);
            this.environmentBackgroundDrawable.addVertexBuffer("position", this.rectangleVertices);

            this.screenSpaceDepthFBO = context.buildFramebufferObject(512, 512)
                    .addDepthAttachment(DepthAttachmentSpec.createFixedPointWithPrecision(16).setLinearFilteringEnabled(true))
                    .createFramebufferObject();

            shadowProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "depth.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "depth.frag"))
                    .createProgram();

            shadowDrawable = context.createDrawable(shadowProgram);

            grid.initialize();
            lightVisuals.initialize();

            shadowDrawable.addVertexBuffer("position", resources.positionBuffer);

            shadowMaps = createShadowMaps();
            shadowFramebuffer = context.buildFramebufferObject(2048, 2048)
                .addDepthAttachment()
                .createFramebufferObject();

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

            // Shade the entire first frame before announcing that loading is complete.
            Matrix4 projection = sceneModel.getProjectionMatrix(windowSize);
            // TODO break this into blocks just in case there's a GPU timeout?
            this.setupForDraw(this.program);
            this.program.setUniform("projection", projection);

            // Render to off-screen buffer
            mainDrawable.draw(PrimitiveMode.TRIANGLES, firstShadingFBO, 0, 0, windowSize.width, windowSize.height);

            // Flush to prevent timeout
            context.flush();

            if (this.loadingMonitor != null)
            {
                this.loadingMonitor.loadingComplete();
            }
        }
        catch (RuntimeException|IOException e)
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

    private Texture3D<ContextType> createShadowMaps()
    {
        return context.getTextureFactory().build2DDepthTextureArray(2048, 2048, sceneModel.getLightingModel().getLightCount())
            .setInternalPrecision(32)
            .setFloatingPointEnabled(true)
            .createTexture();
    }

    @Override
    public void update()
    {
        updateCompiledSettings();

        this.updateWorldSpaceDefinition();

        if (this.environmentMapUnloadRequested && this.environmentMap != null)
        {
            this.environmentMap.close();
            this.environmentMap = null;
            this.environmentMapUnloadRequested = false;
        }

        if (this.backplateUnloadRequested && this.backplateTexture != null)
        {
            this.backplateTexture.close();
            this.backplateTexture = null;
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
                    if (this.environmentMap != null)
                    {
                        this.environmentMap.close();
                    }

                    this.environmentMap = newEnvironmentTexture;
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
                    if (this.backplateTexture != null)
                    {
                        this.backplateTexture.close();
                    }

                    this.backplateTexture = newBackplateTexture;
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
        return sceneModel.getLightingModel().isEnvironmentMappingEnabled() ? Optional.ofNullable(environmentMap) : Optional.empty();
    }

    private void setupForDraw(Program<ContextType> program)
    {
        this.resources.setupShaderProgram(program);

        program.setUniform("weightExponent", this.sceneModel.getSettingsModel().getFloat("weightExponent"));
        program.setUniform("isotropyFactor", this.sceneModel.getSettingsModel().getFloat("isotropyFactor"));
        program.setUniform("occlusionBias", this.sceneModel.getSettingsModel().getFloat("occlusionBias"));

        float gamma = this.sceneModel.getSettingsModel().getFloat("gamma");
        program.setUniform("renderGamma", gamma);

        program.setTexture("shadowMaps", shadowMaps);

        if (this.environmentMap == null || !sceneModel.getLightingModel().isEnvironmentMappingEnabled())
        {
            program.setTexture("environmentMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_CUBE_MAP));
        }
        else
        {
            program.setUniform("useEnvironmentMap", true);
            program.setTexture("environmentMap", this.environmentMap);
            program.setUniform("environmentMipMapLevel",
                Math.max(0, Math.min(this.environmentMap.getMipmapLevelCount() - 1,
                    this.sceneModel.getLightingModel().getEnvironmentMapFilteringBias()
                        + (float)(0.5 *
                            Math.log(6 * (double)this.environmentMap.getFaceSize() * (double)this.environmentMap.getFaceSize()
                                / (double)resources.viewSet.getCameraPoseCount() )
                            / Math.log(2.0)))));
            program.setUniform("diffuseEnvironmentMipMapLevel", this.environmentMap.getMipmapLevelCount() - 1);

            Matrix4 envMapMatrix = sceneModel.getEnvironmentMapMatrix();
            program.setUniform("envMapMatrix", envMapMatrix);
        }

        program.setUniform("ambientColor", sceneModel.getLightingModel().getAmbientLightColor());

        float maxLuminance = (float)resources.viewSet.getLuminanceEncoding().decodeFunction.applyAsDouble(255.0);

        this.clearColor = new Vector3(
                (float)Math.pow(sceneModel.getLightingModel().getBackgroundColor().x / maxLuminance, 1.0 / gamma),
                (float)Math.pow(sceneModel.getLightingModel().getBackgroundColor().y / maxLuminance, 1.0 / gamma),
                (float)Math.pow(sceneModel.getLightingModel().getBackgroundColor().z / maxLuminance, 1.0 / gamma));
    }

    private Matrix4 getDefaultCameraPose()
    {
        return resources.viewSet.getCameraPose(resources.viewSet.getPrimaryViewIndex());
    }

    private void updateWorldSpaceDefinition()
    {
        sceneModel.setScale(resources.geometry.getBoundingRadius() * 2);
        sceneModel.setOrientation(getDefaultCameraPose().getUpperLeft3x3());
        sceneModel.setCentroid(resources.geometry.getCentroid());
    }

    private Matrix4 getLightProjection(int lightIndex)
    {
        Matrix4 lightMatrix = sceneModel.getLightMatrix(lightIndex);

        Vector4 lightDisplacement = lightMatrix.times(sceneModel.getCentroid().asPosition());
        float lightDist = lightDisplacement.getXYZ().length();
        float lookAtDist = lightDisplacement.getXY().length();

        float radius = (float)
            (sceneModel.getOrientation()
                .times(new Vector3(0.5f * sceneModel.getScale()))
                .length() / Math.sqrt(3));

        float fov;
        float farPlane;
        float nearPlane;

        if (sceneModel.getLightingModel().isGroundPlaneEnabled())
        {
            fov = 2.0f * (float)Math.asin(Math.min(0.99, (sceneModel.getScale() + lookAtDist) / lightDist));
            farPlane = lightDist + 2 * sceneModel.getScale();
            nearPlane = Math.max((lightDist + radius) / 32.0f, lightDist - 2 * radius);
        }
        else
        {
            fov = 2.0f * (float)Math.asin(Math.min(0.99, (radius + lookAtDist) / lightDist));
            farPlane = lightDist + radius;
            nearPlane = Math.max(farPlane / 1024.0f, lightDist - 2 * radius);
        }

        // Limit fov by the light's spot size.
        float spotFOV = 2.0f * sceneModel.getLightingModel().getLightPrototype(lightIndex).getSpotSize();
        fov = Math.min(fov, spotFOV);

        return Matrix4.perspective(fov, 1.0f, nearPlane, farPlane);
    }

    private void generateShadowMaps(int lightIndex)
    {
        Matrix4 lightProj = getLightProjection(lightIndex);

        shadowProgram.setUniform("projection", lightProj);

        FramebufferAttachment<ContextType> attachment = shadowMaps.getLayerAsFramebufferAttachment(lightIndex);

        shadowFramebuffer.setDepthAttachment(attachment);
        shadowFramebuffer.clearDepthBuffer();

        shadowProgram.setUniform("model_view", sceneModel.getLightMatrix(lightIndex));
        shadowDrawable.draw(PrimitiveMode.TRIANGLES, shadowFramebuffer);
    }

    private void setupLight(Program<ContextType> program, int lightIndex)
    {
        setupLight(program, lightIndex, sceneModel.getLightMatrix(lightIndex));

        // lightMatrix can be hardcoded here (comment out previous line)

        // Contemporary gallery and stonewall
        //Matrix4.rotateY(16 * Math.PI / 16).times(Matrix4.rotateX(0 * Math.PI / 16))

        // Color studio 2:
        //Matrix4.rotateY(6 * Math.PI / 16).times(Matrix4.rotateX(0 * Math.PI / 16))

        // For the synthetic falcon example?
        //Matrix4.rotateY(5 * Math.PI / 4).times(Matrix4.rotateX(-Math.PI / 4))

        // Always end with this when hardcoding:
        //    .times(new Matrix4(new Matrix3(getDefaultCameraPose())));
    }

    private void setupLight(Program<ContextType> program, int lightIndex, Matrix4 lightMatrix)
    {
        Matrix4 lightMatrixInverse = lightMatrix.quickInverse(0.001f);

        Vector3 lightPos = lightMatrixInverse.times(Vector4.ORIGIN).getXYZ();

        program.setUniform("lightPosVirtual[" + lightIndex + ']', lightPos);

        Vector3 controllerLightIntensity = sceneModel.getLightingModel().getLightPrototype(lightIndex).getColor();
        float lightDistance = sceneModel.getLightMatrix(lightIndex).times(sceneModel.getCentroid().asPosition()).getXYZ().length();

        float lightScale = resources.viewSet.areLightSourcesInfinite() ? 1.0f :
                getDefaultCameraPose()
                        .times(resources.geometry.getCentroid().asPosition())
                    .getXYZ().length();

        program.setUniform("lightIntensityVirtual[" + lightIndex + ']',
                controllerLightIntensity.times(lightDistance * lightDistance * resources.viewSet.getLightIntensity(0).y / (lightScale * lightScale)));
        program.setUniform("lightMatrixVirtual[" + lightIndex + ']', getLightProjection(lightIndex).times(lightMatrix));
        program.setUniform("lightOrientationVirtual[" + lightIndex + ']',
            lightMatrixInverse.times(new Vector4(0.0f, 0.0f, -1.0f, 0.0f)).getXYZ().normalized());
        program.setUniform("lightSpotSizeVirtual[" + lightIndex + ']',
            (float)Math.sin(sceneModel.getLightingModel().getLightPrototype(lightIndex).getSpotSize()));
        program.setUniform("lightSpotTaperVirtual[" + lightIndex + ']', sceneModel.getLightingModel().getLightPrototype(lightIndex).getSpotTaper());
    }



    private NativeVectorBuffer generateViewWeights(Matrix4 targetView)
    {
        float[] viewWeights = //new PowerViewWeightGenerator(settings.getWeightExponent())
            new KNNViewWeightGenerator(4)
                .generateWeights(resources,
                    new AbstractList<Integer>()
                    {
                        @Override
                        public Integer get(int index)
                        {
                            return index;
                        }

                        @Override
                        public int size()
                        {
                            return resources.viewSet.getCameraPoseCount();
                        }
                    },
                    targetView);

        return NativeVectorBufferFactory.getInstance().createFromFloatArray(1, viewWeights.length, viewWeights);
    }

    private void setupModelView(Program<ContextType> p, Matrix4 modelView)
    {
        for (int lightIndex = 0; lightIndex < sceneModel.getLightingModel().getLightCount(); lightIndex++)
        {
            setupLight(p, lightIndex);
        }

        p.setUniform("model_view", modelView);
        p.setUniform("viewPos", modelView.quickInverse(0.01f).getColumn(3).getXYZ());

        if (!this.sceneModel.getSettingsModel().getBoolean("relightingEnabled") && !sceneModel.getSettingsModel().getBoolean("lightCalibrationMode")
            && this.sceneModel.getSettingsModel().get("weightMode", ShadingParameterMode.class) == ShadingParameterMode.UNIFORM)
        {
            if (weightBuffer == null)
            {
                weightBuffer = context.createUniformBuffer();
            }
            weightBuffer.setData(this.generateViewWeights(modelView)); // TODO modelView might not be the right matrix?
            p.setUniformBuffer("ViewWeights", weightBuffer);
        }
    }

    private void setupProjection(Program<ContextType> p, int fullWidth, int fullHeight, int x, int y, int width, int height, Matrix4 projection)
    {
        float scaleX = (float)fullWidth / (float)width;
        float scaleY = (float)fullHeight / (float)height;
        float centerX = (2 * x + width - fullWidth) / (float)fullWidth;
        float centerY = (2 * y + height - fullHeight) / (float)fullHeight;

        Matrix4 adjustedProjection = Matrix4.scale(scaleX, scaleY, 1.0f)
            .times(Matrix4.translate(-centerX, -centerY, 0))
            .times(projection);

        p.setUniform("projection", adjustedProjection);
        p.setUniform("fullProjection", projection);
    }

    private void drawGroundPlane(Framebuffer<ContextType> framebuffer, Matrix4 view)
    {
        // Set up camera for ground plane program.
        groundPlaneDrawable.program().setUniform("model_view", view);
        groundPlaneDrawable.program().setUniform("viewPos", view.quickInverse(0.01f).getColumn(3).getXYZ());

        // Disable back face culling since the plane is one-sided.
        context.getState().disableBackFaceCulling();

        // Do first pass at half resolution to off-screen buffer
        groundPlaneDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);

        // Re-enable back face culling
        context.getState().enableBackFaceCulling();
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


    @Override
    public void draw(Framebuffer<ContextType> framebuffer, Matrix4 modelViewOverride, Matrix4 projectionOverride, int subdivWidth, int subdivHeight)
    {
        boolean overriddenViewMatrix = modelViewOverride != null;

        //Matrix4 view = this.getAbsoluteViewMatrix();
        Matrix4 view = sceneModel.getCurrentViewMatrix();

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

            boolean lightCalibrationMode = false;
            int snapViewIndex = -1;

            if (overriddenViewMatrix)
            {
                view = sceneModel.getViewFromModelViewMatrix(modelViewOverride);
            }
            else if (sceneModel.getSettingsModel().getBoolean("lightCalibrationMode"))
            {
                lightCalibrationMode = true;

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
                        view = lightTransform.times(sceneModel.getViewMatrixFromCameraPose(candidateView));
                        snapViewIndex = i;
                    }
                }
            }

            FramebufferSize size = framebuffer.getSize();

            Matrix4 projection = projectionOverride;

            if (projection == null)
            {
                projection = sceneModel.getProjectionMatrix(size);
            }

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
                        .createFramebufferObject();

                UniformBuffer<ContextType> viewIndexBuffer = context.createUniformBuffer()
            )
            {
                offscreenFBO.clearIntegerColorBuffer(1, 0, 0, 0, 0);
                offscreenFBO.clearDepthBuffer();

                if (backplateTexture != null && sceneModel.getLightingModel().getBackgroundMode() == BackgroundMode.IMAGE)
                {
                    tintedTexDrawable.program().setTexture("tex", backplateTexture);
                    tintedTexDrawable.program().setUniform("color", clearColor);

                    context.getState().disableDepthTest();
                    tintedTexDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
                    context.getState().enableDepthTest();

                    // Clear ID buffer again.
                    offscreenFBO.clearIntegerColorBuffer(1, 0, 0, 0, 0);
                }
                else if (environmentMap != null && sceneModel.getLightingModel().getBackgroundMode() == BackgroundMode.ENVIRONMENT_MAP)
                {
                    Matrix4 envMapMatrix = sceneModel.getLightingModel().getEnvironmentMapMatrix();

                    environmentBackgroundProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("EnvironmentMap"));
                    environmentBackgroundProgram.setUniform("useEnvironmentTexture", true);
                    environmentBackgroundProgram.setTexture("env", environmentMap);
                    environmentBackgroundProgram.setUniform("model_view", view);
                    environmentBackgroundProgram.setUniform("projection", projection);
                    environmentBackgroundProgram.setUniform("envMapMatrix", envMapMatrix);
                    environmentBackgroundProgram.setUniform("envMapIntensity", this.clearColor);

                    environmentBackgroundProgram.setUniform("gamma",
                        environmentMap.isInternalFormatCompressed() ||
                            environmentMap.getInternalUncompressedColorFormat().dataType != DataType.FLOATING_POINT
                            ? 1.0f : 2.2f);

                    context.getState().disableDepthTest();
                    environmentBackgroundDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
                    context.getState().enableDepthTest();
                }
                else
                {
                    offscreenFBO.clearColorBuffer(0, clearColor.x, clearColor.y, clearColor.z, 1.0f);
                }

                // Too many lights; need to re-allocate shadow maps
                if (shadowMaps.getDepth() < sceneModel.getLightingModel().getLightCount())
                {
                    shadowMaps.close();
                    shadowMaps = null;
                    shadowMaps = createShadowMaps();
                }

                for (int lightIndex = 0; lightIndex < sceneModel.getLightingModel().getLightCount(); lightIndex++)
                {
                    generateShadowMaps(lightIndex);
                }

                // Draw grid
                grid.draw(offscreenFBO, view);

                if (sceneModel.getLightingModel().isGroundPlaneEnabled())
                {
                    this.groundPlaneProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("SceneObject"));
                    this.groundPlaneProgram.setUniform("defaultDiffuseColor", sceneModel.getLightingModel().getGroundPlaneColor());
                    this.groundPlaneProgram.setUniform("projection", sceneModel.getProjectionMatrix(size));

                    this.setupForDraw(groundPlaneProgram);

                    float scale = sceneModel.getScale();
                    for (int lightIndex = 0; lightIndex < sceneModel.getLightingModel().getLightCount(); lightIndex++)
                    {
                        setupLight(groundPlaneProgram, lightIndex,
                             Matrix4.scale(scale)
                                 .times(sceneModel.getLightingModel().getLightMatrix(lightIndex))
                                 .times(Matrix4.translate(new Vector3(0, sceneModel.getLightingModel().getGroundPlaneHeight(), 0)))
                                 .times(Matrix4.scale(1.0f / scale))
                                 .times(Matrix4.rotateX(Math.PI / 2))
                                 .times(Matrix4.scale(sceneModel.getScale() * sceneModel.getLightingModel().getGroundPlaneSize())));
                    }

                    this.drawGroundPlane(offscreenFBO,
                        view
                            .times(Matrix4.scale(scale))
                            .times(Matrix4.translate(new Vector3(0, sceneModel.getLightingModel().getGroundPlaneHeight(), 0)))
                            .times(Matrix4.scale(1.0f / scale))
                            .times(Matrix4.rotateX(Math.PI / 2))
                            .times(Matrix4.scale(sceneModel.getScale() * sceneModel.getLightingModel().getGroundPlaneSize())));
                }

                // After the ground plane, use a gray color for anything without a texture map.
                this.program.setUniform("defaultDiffuseColor", new Vector3(0.125f));

                context.getState().disableBackFaceCulling();

                setupForDraw(this.program);

                this.program.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("IBRObject"));

                if (lightCalibrationMode)
                {
                    this.program.setUniform("holeFillColor", new Vector3(0.5f));
                    viewIndexBuffer.setData(NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, 1, snapViewIndex));
                    this.program.setUniformBuffer("ViewIndices", viewIndexBuffer);
                }
                else
                {
                    this.program.setUniform("holeFillColor", new Vector3(0.0f));
                }

                shadowProgram.setUniform("projection", projection);
                screenSpaceDepthFBO.clearDepthBuffer();

                setupModelView(shadowProgram, view);
                shadowDrawable.draw(PrimitiveMode.TRIANGLES, screenSpaceDepthFBO);

                context.flush();

                this.program.setTexture("screenSpaceDepthBuffer", screenSpaceDepthFBO.getDepthAttachmentTexture());

                Matrix4 modelView = lightCalibrationMode ? sceneModel.getCameraPoseFromViewMatrix(view) : sceneModel.getModelViewMatrix(view);
                drawModelInSubdivisions(this.mainDrawable, offscreenFBO, subdivWidth, subdivHeight,
                        // Don't use the model matrix when in light calibration mode.
                        modelView, projection);

                context.getState().enableBackFaceCulling();

                if (!sceneModel.getLightingModel().areLightWidgetsEthereal()
                    && IntStream.range(0, sceneModel.getLightingModel().getLightCount()).anyMatch(sceneModel.getLightingModel()::isLightWidgetEnabled))
                {
                    context.flush();

                    // Read buffers here if light widgets are ethereal (i.e. they cannot be clicked and should not be in the ID buffer)
                    sceneViewportModel.refreshBuffers(offscreenFBO);
                }

                lightVisuals.draw(offscreenFBO, view);

                // Finish drawing
                context.flush();

                // Second pass at full resolution to default framebuffer
                simpleTexDrawable.program().setTexture("tex", offscreenFBO.getColorAttachmentTexture(0));

                framebuffer.clearDepthBuffer();
                simpleTexDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);

                context.flush();

                if (!sceneModel.getLightingModel().areLightWidgetsEthereal()
                    && IntStream.range(0, sceneModel.getLightingModel().getLightCount()).anyMatch(sceneModel.getLightingModel()::isLightWidgetEnabled))
                {
                    // Read buffers here if light widgets are not ethereal (i.e. they can be clicked and should be in the ID buffer)
                    sceneViewportModel.refreshBuffers(offscreenFBO);
                }
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

    private void drawModelInSubdivisions(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer,
                                         int subdivWidth, int subdivHeight, Matrix4 view, Matrix4 projection)
    {
        FramebufferSize fullFBOSize = framebuffer.getSize();

        // Optionally render in subdivisions to prevent GPU timeout
        for (int x = 0; x < fullFBOSize.width; x += subdivWidth)
        {
            for (int y = 0; y < fullFBOSize.height; y += subdivHeight)
            {
                int effectiveWidth = Math.min(subdivWidth, fullFBOSize.width - x);
                int effectiveHeight = Math.min(subdivHeight, fullFBOSize.height - y);

                setupModelView(drawable.program(), view);
                setupProjection(drawable.program(), fullFBOSize.width, fullFBOSize.height, x, y, effectiveWidth, effectiveHeight, projection);

                // Render to off-screen buffer
                drawable.draw(PrimitiveMode.TRIANGLES, framebuffer, x, y, effectiveWidth, effectiveHeight);

                // Flush to prevent timeout
                context.flush();
            }
        }
    }


    @Override
    public void close()
    {
        if (this.program != null)
        {
            this.program.close();
            this.program = null;
        }

        if (this.environmentBackgroundProgram != null)
        {
            this.environmentBackgroundProgram.close();
            this.environmentBackgroundProgram = null;
        }

        if (this.environmentMap != null)
        {
            this.environmentMap.close();
            this.environmentMap = null;
        }

        if (this.backplateTexture != null)
        {
            this.backplateTexture.close();
            this.backplateTexture = null;
        }

        if (resources != null)
        {
            resources.close();
            resources = null;
        }

        if (shadowMaps != null)
        {
            shadowMaps.close();
            shadowMaps = null;
        }

        if (shadowFramebuffer != null)
        {
            shadowFramebuffer.close();
            shadowFramebuffer = null;
        }

        if (shadowProgram != null)
        {
            shadowProgram.close();
            shadowProgram = null;
        }

        if (screenSpaceDepthFBO != null)
        {
            screenSpaceDepthFBO.close();
            screenSpaceDepthFBO = null;
        }

        if (rectangleVertices != null)
        {
            rectangleVertices.close();
            rectangleVertices = null;
        }

        if (weightBuffer != null)
        {
            weightBuffer.close();
            weightBuffer = null;
        }

        if (simpleTexProgram != null)
        {
            simpleTexProgram.close();
            simpleTexProgram = null;
        }

        if (tintedTexProgram != null)
        {
            tintedTexProgram.close();
            tintedTexProgram = null;
        }

        if (grid != null)
        {
            grid.close();
        }

        if (lightVisuals != null)
        {
            lightVisuals.close();
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
            //noinspection VariableNotUsedInsideIf
            if (this.environmentMap != null)
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
        if (backplateFile == null && this.backplateTexture != null)
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
            reloadMainProgram();

            Program<ContextType> newEnvironmentBackgroundProgram = resources.getIBRShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "envbackgroundtexture.frag"))
                    .createProgram();

            if (this.environmentBackgroundProgram != null)
            {
                this.environmentBackgroundProgram.close();
            }

            this.environmentBackgroundProgram = newEnvironmentBackgroundProgram;
            this.environmentBackgroundDrawable = context.createDrawable(environmentBackgroundProgram);
            this.environmentBackgroundDrawable.addVertexBuffer("position", rectangleVertices);

            grid.reloadShaders();
            lightVisuals.reloadShaders();
        }
        catch (FileNotFoundException|RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    private void reloadMainProgram(Map<String, Optional<Object>> defineMap, StandardRenderingMode renderingMode) throws FileNotFoundException
    {
        Program<ContextType> newProgram = loadMainProgram(defineMap, renderingMode);

        if (this.program != null)
        {
            this.program.close();
        }

        this.program = newProgram;

        this.lastCompiledRenderingMode = renderingMode;

        this.mainDrawable = context.createDrawable(program);
        this.mainDrawable.addVertexBuffer("position", this.resources.positionBuffer);

        if (this.resources.normalBuffer != null)
        {
            this.mainDrawable.addVertexBuffer("normal", this.resources.normalBuffer);
        }

        if (this.resources.texCoordBuffer != null)
        {
            this.mainDrawable.addVertexBuffer("texCoord", this.resources.texCoordBuffer);
        }

        if (this.resources.tangentBuffer != null)
        {
            this.mainDrawable.addVertexBuffer("tangent", this.resources.tangentBuffer);
        }

        // Also reload ground plane program as it uses the same base shaders.
        if (this.groundPlaneProgram != null)
        {
            this.groundPlaneProgram.close();
            this.groundPlaneProgram = null;
        }

        this.groundPlaneProgram = loadMainProgram(getReferenceScenePreprocessorDefines(), StandardRenderingMode.LAMBERTIAN_SHADED);

        groundPlaneDrawable = context.createDrawable(groundPlaneProgram);
        groundPlaneDrawable.addVertexBuffer("position", rectangleVertices);
        groundPlaneDrawable.setVertexAttrib("normal", new Vector3(0, 0, 1));

        suppressErrors = false;
    }

    private void reloadMainProgram() throws FileNotFoundException
    {
        reloadMainProgram(getPreprocessorDefines(), this.sceneModel.getSettingsModel() == null ?
            StandardRenderingMode.IMAGE_BASED : this.sceneModel.getSettingsModel().get("renderingMode", StandardRenderingMode.class));
    }

    private Map<String, Optional<Object>> getPreprocessorDefines()
    {
        Map<String, Optional<Object>> defineMap = new HashMap<>(256);

        // Initialize to defaults
        defineMap.put("PHYSICALLY_BASED_MASKING_SHADOWING", Optional.empty());
        defineMap.put("FRESNEL_EFFECT_ENABLED", Optional.empty());
        defineMap.put("SHADOWS_ENABLED", Optional.empty());
        defineMap.put("BUEHLER_ALGORITHM", Optional.empty());
        defineMap.put("SORTING_SAMPLE_COUNT", Optional.empty());
        defineMap.put("RELIGHTING_ENABLED", Optional.empty());
        defineMap.put("VISIBILITY_TEST_ENABLED", Optional.empty());
        defineMap.put("SHADOW_TEST_ENABLED", Optional.empty());
        defineMap.put("PRECOMPUTED_VIEW_WEIGHTS_ENABLED", Optional.empty());
        defineMap.put("USE_VIEW_INDICES", Optional.empty());

        defineMap.put("VIEW_COUNT", Optional.empty());
        defineMap.put("VIRTUAL_LIGHT_COUNT", Optional.empty());
        defineMap.put("ENVIRONMENT_ILLUMINATION_ENABLED", Optional.empty());

        defineMap.put("LUMINANCE_MAP_ENABLED", Optional.of(this.resources.viewSet.hasCustomLuminanceEncoding()));
        defineMap.put("INVERSE_LUMINANCE_MAP_ENABLED", Optional.of(false/*this.resources.viewSet.hasCustomLuminanceEncoding()*/));

        defineMap.put("RAY_DEPTH_GRADIENT", Optional.of(0.1 * sceneModel.getScale()));
        defineMap.put("RAY_POSITION_JITTER", Optional.of(0.01 * sceneModel.getScale()));

        if (this.sceneModel.getSettingsModel() != null)
        {
            defineMap.put("PHYSICALLY_BASED_MASKING_SHADOWING",
                Optional.of(this.sceneModel.getSettingsModel().getBoolean("pbrGeometricAttenuationEnabled")));
            defineMap.put("FRESNEL_EFFECT_ENABLED", Optional.of(this.sceneModel.getSettingsModel().getBoolean("fresnelEnabled")));
            defineMap.put("SHADOWS_ENABLED", Optional.of(this.sceneModel.getSettingsModel().getBoolean("shadowsEnabled")));

            defineMap.put("BUEHLER_ALGORITHM", Optional.of(this.sceneModel.getSettingsModel().getBoolean("buehlerAlgorithm")));
            defineMap.put("SORTING_SAMPLE_COUNT", Optional.of(this.sceneModel.getSettingsModel().getInt("buehlerViewCount")));
            defineMap.put("RELIGHTING_ENABLED", Optional.of(this.sceneModel.getSettingsModel().getBoolean("relightingEnabled")
                && !sceneModel.getSettingsModel().getBoolean("lightCalibrationMode") && this.sceneModel.getLightingModel() != null));

            boolean occlusionEnabled = this.sceneModel.getSettingsModel().getBoolean("occlusionEnabled")
                && (this.sceneModel.getSettingsModel().getBoolean("relightingEnabled")
                    || sceneModel.getSettingsModel().getBoolean("lightCalibrationMode")
                    || this.sceneModel.getSettingsModel().get("weightMode", ShadingParameterMode.class) != ShadingParameterMode.UNIFORM);

            defineMap.put("VISIBILITY_TEST_ENABLED", Optional.of(occlusionEnabled && this.resources.depthTextures != null));
            defineMap.put("SHADOW_TEST_ENABLED", Optional.of(occlusionEnabled && this.resources.shadowTextures != null
                && !sceneModel.getSettingsModel().getBoolean("lightCalibrationMode")));

            defineMap.put("PRECOMPUTED_VIEW_WEIGHTS_ENABLED",
                Optional.of(!this.sceneModel.getSettingsModel().getBoolean("relightingEnabled") && !sceneModel.getSettingsModel().getBoolean("lightCalibrationMode")
                    && this.sceneModel.getSettingsModel().get("weightMode", ShadingParameterMode.class) == ShadingParameterMode.UNIFORM));

            if (sceneModel.getSettingsModel().getBoolean("lightCalibrationMode"))
            {
                defineMap.put("USE_VIEW_INDICES", Optional.of(true));
                defineMap.put("VIEW_COUNT", Optional.of(1));
            }

            if (this.sceneModel.getLightingModel() != null && this.sceneModel.getSettingsModel().getBoolean("relightingEnabled"))
            {
                defineMap.put("VIRTUAL_LIGHT_COUNT", Optional.of(sceneModel.getLightingModel().getLightCount()));
                defineMap.put("ENVIRONMENT_ILLUMINATION_ENABLED", Optional.of(!Objects.equals(sceneModel.getLightingModel().getAmbientLightColor(), Vector3.ZERO)));
                defineMap.put("ENVIRONMENT_TEXTURE_ENABLED", Optional.of(this.environmentMap != null && sceneModel.getLightingModel().isEnvironmentMappingEnabled()));
            }
        }

        return defineMap;
    }

    private Map<String, Optional<Object>> getReferenceScenePreprocessorDefines()
    {
        return getPreprocessorDefines();
    }

    private ProgramBuilder<ContextType> getProgramBuilder(Map<String, Optional<Object>> defineMap, StandardRenderingMode renderingMode)
    {
        ProgramBuilder<ContextType> programBuilder = resources.getIBRShaderProgramBuilder(renderingMode);

        for (Entry<String, Optional<Object>> defineEntry : defineMap.entrySet())
        {
            if (defineEntry.getValue().isPresent())
            {
                programBuilder.define(defineEntry.getKey(), defineEntry.getValue().get());
            }
        }

        return programBuilder;
    }

    private Program<ContextType> loadMainProgram(Map<String, Optional<Object>> defineMap, StandardRenderingMode renderingMode) throws FileNotFoundException
    {
        return this.getProgramBuilder(defineMap, renderingMode)
                .define("SPOTLIGHTS_ENABLED", true)
                .addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/relight/relight.frag"))
                .createProgram();
    }

    private Program<ContextType> loadMainProgram() throws FileNotFoundException
    {
        return loadMainProgram(getPreprocessorDefines(), this.sceneModel.getSettingsModel().get("renderingMode", StandardRenderingMode.class));
    }

    private void updateCompiledSettings()
    {
        Map<String, Optional<Object>> defineMap = getPreprocessorDefines();

        StandardRenderingMode renderingMode =
            this.sceneModel.getSettingsModel() == null ? StandardRenderingMode.IMAGE_BASED : this.sceneModel.getSettingsModel().get("renderingMode", StandardRenderingMode.class);

        if (renderingMode != lastCompiledRenderingMode ||
            defineMap.entrySet().stream().anyMatch(
                defineEntry -> !Objects.equals(this.program.getDefine(defineEntry.getKey()), defineEntry.getValue())))
        {
            try
            {
                System.out.println("Updating compiled render settings.");
                this.reloadMainProgram(defineMap, renderingMode);
            }
            catch (RuntimeException|FileNotFoundException e)
            {
                e.printStackTrace();
            }
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
