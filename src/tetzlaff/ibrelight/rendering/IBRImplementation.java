package tetzlaff.ibrelight.rendering;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import org.lwjgl.*;
import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthAttachmentSpec;
import tetzlaff.gl.core.*;
import tetzlaff.gl.core.AlphaBlendingFunction.Weight;
import tetzlaff.gl.core.ColorFormat.DataType;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.*;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.RenderingMode;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.ibrelight.rendering.IBRResources.Builder;
import tetzlaff.ibrelight.util.KNNViewWeightGenerator;
import tetzlaff.interactive.InitializationException;
import tetzlaff.models.*;
import tetzlaff.models.impl.DefaultSettingsModel;
import tetzlaff.models.impl.SafeSettingsModelWrapperFactory;
import tetzlaff.util.AbstractImage;
import tetzlaff.util.ArrayBackedImage;
import tetzlaff.util.EnvironmentMap;
import tetzlaff.util.ShadingParameterMode;

public class IBRImplementation<ContextType extends Context<ContextType>> implements IBRRenderable<ContextType>
{
    private final ContextType context;
    private Program<ContextType> program;
    private Program<ContextType> shadowProgram;
    private volatile LoadingMonitor loadingMonitor;
    private boolean suppressErrors = false;
    private SafeReadonlySettingsModel settingsModel;
    private RenderingMode lastCompiledRenderingMode = RenderingMode.IMAGE_BASED;

    private final Builder<ContextType> resourceBuilder;
    private IBRResources<ContextType> resources;

    private Texture3D<ContextType> shadowMaps;
    private FramebufferObject<ContextType> shadowFramebuffer;
    private Drawable<ContextType> shadowDrawable;

    private Program<ContextType> lightProgram;
    private VertexBuffer<ContextType> rectangleVertices;
    private Texture2D<ContextType> lightTexture;
    private Texture2D<ContextType> lightCenterTexture;
    private Drawable<ContextType> lightDrawable;

    private Program<ContextType> solidProgram;
    private VertexBuffer<ContextType> widgetVertices;
    private Drawable<ContextType> widgetDrawable;
    private VertexBuffer<ContextType> gridVertices;
    private Drawable<ContextType> gridDrawable;
    
    private final String id;
    private Drawable<ContextType> mainDrawable;

    private ReadonlyObjectModel objectModel;
    private ReadonlyCameraModel cameraModel;
    private ReadonlyLightingModel lightingModel;

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
    
    private List<Matrix4> multiTransformationModel;
    private Vector3 centroid;
    private float boundingRadius;

    private Program<ContextType> referenceSceneProgram;
    private VertexGeometry referenceScene;
    private boolean referenceSceneChanged = false;
    private VertexBuffer<ContextType> refScenePositions;
    private VertexBuffer<ContextType> refSceneTexCoords;
    private VertexBuffer<ContextType> refSceneNormals;
    private Texture2D<ContextType> refSceneTexture;
    
    private final String[] sceneObjectNameList;
    private final Map<String, Integer> sceneObjectIDLookup;
    private IntBuffer pixelObjectIDBuffer;
    private ShortBuffer pixelDepthBuffer;
    private FramebufferSize fboSize;

    private Program<ContextType> circleProgram;
    private Drawable<ContextType> circleDrawable;

    private Program<ContextType> reprojectProgram;
    private Drawable<ContextType> reprojectDrawable;

    private Program<ContextType> environmentWeightsProgram;
    private Drawable<ContextType> environmentWeightsDrawable;

    private Texture3D<ContextType> environmentWeightsTexture;
    FramebufferObject<ContextType> environmentWeightsFBO;

    private static final int SHADING_FRAMEBUFFER_COUNT = 2;
    private final Collection<FramebufferObject<ContextType>> shadingFramebuffers = new ArrayList<>(SHADING_FRAMEBUFFER_COUNT);

    private Deque<ShadedFrame<ContextType>> shadedFrames;

    ShadedFrame<ContextType> frameInProgress;

    private int shadingX = 0;
    private int shadingY = 0;
    private double shadingWidth = 256;
    private double shadingHeight = 256;
    private Instant lastFrameStart;
    private Instant lastShadingFrameStart;

    private static final int MIN_SHADING_DIMENSION = 256;
    private static final int MAX_SHADING_DIMENSION = 4096;
    private static final int TARGET_FPS = 5;

    private static class ShadedFrame<ContextType extends Context<ContextType>>
    {
        final FramebufferObject<ContextType> framebuffer;
        final Matrix4 modelView;
        final Matrix4 projection;

        ShadedFrame(FramebufferObject<ContextType> framebuffer, Matrix4 modelView, Matrix4 projection)
        {
            this.framebuffer = framebuffer;
            this.modelView = modelView;
            this.projection = projection;
        }
    }

    IBRImplementation(String id, ContextType context, Program<ContextType> program, Builder<ContextType> resourceBuilder)
    {
        this.id = id;
        this.context = context;
        this.program = program;
        this.resourceBuilder = resourceBuilder;

        this.clearColor = new Vector3(0.0f);
        this.multiTransformationModel = new ArrayList<>(1);
        this.multiTransformationModel.add(Matrix4.IDENTITY);
        this.settingsModel = new DefaultSettingsModel();

        this.sceneObjectNameList = new String[256];
        this.sceneObjectIDLookup = new HashMap<>(256);

        this.sceneObjectNameList[0] = null;

        this.sceneObjectNameList[1] = "IBRObject";
        this.sceneObjectIDLookup.put("IBRObject", 1);

        this.sceneObjectNameList[2] = "EnvironmentMap";
        this.sceneObjectIDLookup.put("EnvironmentMap", 2);

        this.sceneObjectNameList[3] = "SceneObject";
        this.sceneObjectIDLookup.put("SceneObject", 3);

        int k = 4;

        for (int i = 0; i < 4; i++)
        {
            this.sceneObjectNameList[k] = "Light." + i;
            this.sceneObjectIDLookup.put("Light." + i, k);
            k++;

            this.sceneObjectNameList[k] = "Light." + i + ".Center";
            this.sceneObjectIDLookup.put("Light." + i + ".Center", k);
            k++;

            this.sceneObjectNameList[k] = "Light." + i + ".Azimuth";
            this.sceneObjectIDLookup.put("Light." + i + ".Azimuth", k);
            k++;

            this.sceneObjectNameList[k] = "Light." + i + ".Inclination";
            this.sceneObjectIDLookup.put("Light." + i + ".Inclination", k);
            k++;

            this.sceneObjectNameList[k] = "Light." + i + ".Distance";
            this.sceneObjectIDLookup.put("Light." + i + ".Distance", k);
            k++;
        }
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
            this.reprojectProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "reproject.frag"))
                    .createProgram();

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

            this.mainDrawable = context.createDrawable(program);
            this.mainDrawable.addVertexBuffer("position", this.resources.positionBuffer);

            if (this.resources.eigentextures != null)
            {
                this.environmentWeightsProgram = getProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "environmentweights.frag"))
                    .createProgram();

                this.environmentWeightsDrawable = context.createDrawable(environmentWeightsProgram);
                this.environmentWeightsDrawable.addVertexBuffer("position", this.rectangleVertices);
            }

            this.reprojectDrawable = context.createDrawable(reprojectProgram);
            this.reprojectDrawable.addVertexBuffer("position", this.resources.positionBuffer);

            if (this.resources.normalBuffer != null)
            {
                this.mainDrawable.addVertexBuffer("normal", this.resources.normalBuffer);
                this.reprojectDrawable.addVertexBuffer("normal", this.resources.normalBuffer);
            }

            if (this.resources.texCoordBuffer != null)
            {
                this.mainDrawable.addVertexBuffer("texCoord", this.resources.texCoordBuffer);
                this.reprojectDrawable.addVertexBuffer("texCoord", this.resources.texCoordBuffer);
            }

            if (this.resources.tangentBuffer != null)
            {
                this.mainDrawable.addVertexBuffer("tangent", this.resources.tangentBuffer);
                this.reprojectDrawable.addVertexBuffer("tangent", this.resources.tangentBuffer);
            }

            this.simpleTexDrawable = context.createDrawable(simpleTexProgram);
            this.simpleTexDrawable.addVertexBuffer("position", this.rectangleVertices);

            this.tintedTexDrawable = context.createDrawable(tintedTexProgram);
            this.tintedTexDrawable.addVertexBuffer("position", this.rectangleVertices);

            this.environmentBackgroundDrawable = context.createDrawable(environmentBackgroundProgram);
            this.environmentBackgroundDrawable.addVertexBuffer("position", this.rectangleVertices);

            this.screenSpaceDepthFBO = context.buildFramebufferObject(512, 512)
                    .addDepthAttachment()
                    .createFramebufferObject();

            shadowProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "depth.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "depth.frag"))
                    .createProgram();

            shadowDrawable = context.createDrawable(shadowProgram);

            this.solidProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "solid.frag"))
                    .createProgram();
            this.widgetVertices = context.createVertexBuffer()
                    .setData(NativeVectorBufferFactory.getInstance()
                        .createFromFloatArray(3, 3, -1, -1, 0, 1, -1, 0, 0, 1, 0));

            this.widgetDrawable = context.createDrawable(this.solidProgram);
            this.widgetDrawable.addVertexBuffer("position", widgetVertices);

            float[] grid = new float[252];
            for (int i = 0; i < 21; i++)
            {
                grid[i * 12] = i * 0.1f - 1.0f;
                grid[i * 12 + 1] = 0;
                grid[i * 12 + 2] = 1;

                grid[i * 12 + 3] = i * 0.1f - 1.0f;
                grid[i * 12 + 4] = 0;
                grid[i * 12 + 5] = -1;

                grid[i * 12 + 6] = 1;
                grid[i * 12 + 7] = 0;
                grid[i * 12 + 8] = i * 0.1f - 1.0f;

                grid[i * 12 + 9] = -1;
                grid[i * 12 + 10] = 0;
                grid[i * 12 + 11] = i * 0.1f - 1.0f;
            }

            this.gridVertices = context.createVertexBuffer()
                .setData(NativeVectorBufferFactory.getInstance()
                    .createFromFloatArray(3, 84, grid));

            this.gridDrawable = context.createDrawable(this.solidProgram);
            this.gridDrawable.addVertexBuffer("position", gridVertices);

            this.lightProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "light.frag"))
                    .createProgram();
            this.lightDrawable = context.createDrawable(this.lightProgram);
            this.lightDrawable.addVertexBuffer("position", rectangleVertices);

            this.circleProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "circle.frag"))
                    .createProgram();
            this.circleDrawable = context.createDrawable(this.circleProgram);
            this.circleDrawable.addVertexBuffer("position", rectangleVertices);

            NativeVectorBuffer lightTextureData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 1, 4096);

            NativeVectorBuffer lightCenterTextureData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 1, 4096);

            int k = 0;
            for (int i = 0; i < 64; i++)
            {
                double x = i * 2.0 / 63.0 - 1.0;

                for (int j = 0; j < 64; j++)
                {
                    double y = j * 2.0 / 63.0 - 1.0;

                    double rSq = x*x + y*y;
                    lightTextureData.set(k, 0, (float)(Math.cos(Math.min(Math.sqrt(rSq), 1.0) * Math.PI) + 1.0) * 0.5f);

                    if (rSq <= 1.0)
                    {
                        lightCenterTextureData.set(k, 0, 1.0f);
                    }

                    k++;
                }
            }

            this.lightTexture = context.getTextureFactory().build2DColorTextureFromBuffer(64, 64, lightTextureData)
                    .setInternalFormat(ColorFormat.R8)
                    .setLinearFilteringEnabled(true)
                    .setMipmapsEnabled(true)
                    .createTexture();

            this.lightCenterTexture = context.getTextureFactory().build2DColorTextureFromBuffer(64, 64, lightCenterTextureData)
                    .setInternalFormat(ColorFormat.R8)
                    .setLinearFilteringEnabled(true)
                    .setMipmapsEnabled(true)
                    .createTexture();

            shadowDrawable.addVertexBuffer("position", resources.positionBuffer);

            shadowMaps = context.getTextureFactory().build2DDepthTextureArray(2048, 2048, lightingModel.getLightCount()).createTexture();
            shadowFramebuffer = context.buildFramebufferObject(2048, 2048)
                .addDepthAttachment()
                .createFramebufferObject();

            this.updateCentroidAndRadius();

            if (this.resources.eigentextures != null)
            {
                IntVector2 environmentWeightsResolution = this.resources.getSVDViewWeightResolution();

                this.environmentWeightsTexture
                    = context.getTextureFactory().build2DColorTextureArray(
                    environmentWeightsResolution.x, environmentWeightsResolution.y, 512)
                    .setInternalFormat(ColorFormat.RGBA16F)
                    .createTexture();

                this.environmentWeightsFBO
                    = context.buildFramebufferObject(environmentWeightsResolution.x, environmentWeightsResolution.y)
                    .addEmptyColorAttachments(8)
                    .createFramebufferObject();
            }

            this.shadedFrames = new LinkedList<>();

            FramebufferSize windowSize = context.getDefaultFramebuffer().getSize();
            FramebufferObject<ContextType> firstShadingFBO =
                context.buildFramebufferObject(windowSize.width, windowSize.height)
                    .addColorAttachment(
                        ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB8)
                            .setLinearFilteringEnabled(true))
                    .addDepthAttachment()
                    .createFramebufferObject();

            shadingFramebuffers.add(firstShadingFBO);

            // Shade the entire first frame.
            Matrix4 view = this.getAbsoluteViewMatrix();
            Matrix4 projection = this.getProjectionMatrix(windowSize);
            // TODO break this into blocks just in case there's a GPU timeout?
            this.setupForDraw(this.program);
            Matrix4 modelView = setupModelView(this.program, 0, view);
            this.program.setUniform("projection", projection);

            // Render to off-screen buffer
            mainDrawable.draw(PrimitiveMode.TRIANGLES, firstShadingFBO, 0, 0, windowSize.width, windowSize.height);

            // Flush to prevent timeout
            context.flush();

            this.shadedFrames.add(new ShadedFrame<>(firstShadingFBO, modelView, projection));

    //        // Make sure that everything is loaded onto the graphics card before announcing that loading is complete.
    //        this.draw(context.getDefaultFramebuffer());

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

    @Override
    public void update()
    {
        updateCompiledSettings();

        this.updateCentroidAndRadius();

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

        if (this.referenceSceneChanged && this.referenceScene != null)
        {
            this.referenceSceneChanged = false;

            try
            {
                System.out.println("Using new reference scene.");

                if (this.refScenePositions != null)
                {
                    this.refScenePositions.close();
                    this.refScenePositions = null;
                }

                if (this.refSceneTexCoords != null)
                {
                    this.refSceneTexCoords.close();
                    this.refSceneTexCoords = null;
                }

                if (this.refSceneNormals != null)
                {
                    this.refSceneNormals.close();
                    this.refSceneNormals = null;
                }

                if (this.refSceneTexture != null)
                {
                    this.refSceneTexture.close();
                    this.refSceneTexture = null;
                }

                this.refScenePositions = context.createVertexBuffer().setData(referenceScene.getVertices());
                this.refSceneTexCoords = context.createVertexBuffer().setData(referenceScene.getTexCoords());
                this.refSceneNormals = context.createVertexBuffer().setData(referenceScene.getNormals());
                this.refSceneTexture = context.getTextureFactory().build2DColorTextureFromFile(
                        new File(referenceScene.getFilename().getParentFile(), referenceScene.getMaterial().getDiffuseMap().getMapName()), true)
                    .setMipmapsEnabled(true)
                    .setLinearFilteringEnabled(true)
                    .createTexture();
            }
            catch (IOException|RuntimeException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void setupForDraw(Program<ContextType> program)
    {
        this.resources.setupShaderProgram(program);

        program.setUniform("weightExponent", this.settingsModel.getFloat("weightExponent"));
        program.setUniform("isotropyFactor", this.settingsModel.getFloat("isotropyFactor"));
        program.setUniform("occlusionBias", this.settingsModel.getFloat("occlusionBias"));

        float gamma = this.settingsModel.getFloat("gamma");
        program.setUniform("renderGamma", gamma);

        program.setTexture("shadowMaps", shadowMaps);

        if (this.environmentMap == null || !lightingModel.isEnvironmentMappingEnabled())
        {
            program.setTexture("environmentMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_CUBE_MAP));
        }
        else
        {
            program.setUniform("useEnvironmentMap", true);
            program.setTexture("environmentMap", this.environmentMap);
            program.setUniform("environmentMipMapLevel",
                Math.max(0, Math.min(this.environmentMap.getMipmapLevelCount() - 1,
                    this.lightingModel.getEnvironmentMapFilteringBias()
                        + (int)Math.ceil(0.5 *
                            Math.log(6 * (double)this.environmentMap.getFaceSize() * (double)this.environmentMap.getFaceSize()
                                / (double)resources.viewSet.getCameraPoseCount() )
                            / Math.log(2.0)))));
            program.setUniform("diffuseEnvironmentMipMapLevel", this.environmentMap.getMipmapLevelCount() - 1);

            Matrix4 envMapMatrix = this.getEnvironmentMapMatrix();
            program.setUniform("envMapMatrix", envMapMatrix);
        }

        program.setUniform("ambientColor", lightingModel.getAmbientLightColor());

        float maxLuminance = (float)resources.viewSet.getLuminanceEncoding().decodeFunction.applyAsDouble(255.0);

        this.clearColor = new Vector3(
                (float)Math.pow(lightingModel.getBackgroundColor().x / maxLuminance, 1.0 / gamma),
                (float)Math.pow(lightingModel.getBackgroundColor().y / maxLuminance, 1.0 / gamma),
                (float)Math.pow(lightingModel.getBackgroundColor().z / maxLuminance, 1.0 / gamma));
    }

    private void updateCentroidAndRadius()
    {
        Vector4 sumPositions = new Vector4(0.0f);
        this.boundingRadius = resources.geometry.getBoundingRadius();
        this.centroid = resources.geometry.getCentroid();

        if (multiTransformationModel != null)
        {
            for (Matrix4 m : multiTransformationModel)
            {
                Vector4 position = m.times(resources.geometry.getCentroid().asPosition());
                sumPositions = sumPositions.plus(position);
            }

            this.centroid = sumPositions.getXYZ().dividedBy(sumPositions.w);

            for(Matrix4 m : multiTransformationModel)
            {
                float distance = m.times(resources.geometry.getCentroid().asPosition()).getXYZ().distance(this.centroid);
                this.boundingRadius = Math.max(this.boundingRadius, distance + resources.geometry.getBoundingRadius());
            }
        }
    }

    private Matrix4 getDefaultCameraPose()
    {
        return resources.viewSet.getCameraPose(resources.viewSet.getPrimaryViewIndex());
    }


    private float getScale()
    {
        return this.boundingRadius * 2;
//         return getDefaultCameraPose()
//                 .times(resources.geometry.getCentroid().asPosition())
//             .getXYZ().length()
//             * this.boundingRadius / this.resources.geometry.getBoundingRadius();
    }

    private Matrix4 getLightMatrix(int lightIndex)
    {
        float scale = getScale();
        return Matrix4.scale(scale)
            .times(lightingModel.getLightMatrix(lightIndex))
            .times(objectModel.getTransformationMatrix())
            .times(Matrix4.scale(1.0f / scale))
            .times(getDefaultCameraPose().getUpperLeft3x3().asMatrix4())
            .times(Matrix4.translate(this.centroid.negated()));
    }

    private Matrix4 getEnvironmentMapMatrix()
    {
        float scale = getScale();
        return Matrix4.scale(scale)
            .times(lightingModel.getEnvironmentMapMatrix())
            .times(objectModel.getTransformationMatrix())
            .times(Matrix4.scale(1.0f / scale))
            .times(getDefaultCameraPose().getUpperLeft3x3().asMatrix4())
            .times(Matrix4.translate(this.centroid.negated()));
    }

    private Matrix4 getLightProjection(int lightIndex)
    {
        Matrix4 lightMatrix = getLightMatrix(lightIndex);

        Vector4 lightDisplacement = lightMatrix.times(this.centroid.asPosition());
        float lightDist = lightDisplacement.getXYZ().length();
        float lookAtDist = lightDisplacement.getXY().length();

        float radius = (float)
            (getDefaultCameraPose().getUpperLeft3x3()
                .times(new Vector3(this.boundingRadius))
                .length() / Math.sqrt(3));

        float fov = 2.0f * (float)Math.asin(Math.min(0.99, (radius + lookAtDist) / lightDist));

        float farPlane = lightDist + radius;
        float nearPlane = Math.max(farPlane / 1024.0f, lightDist - radius);

        return Matrix4.perspective(fov, 1.0f, nearPlane, farPlane);
    }

    private void generateShadowMaps(int lightIndex)
    {
        Matrix4 lightProj = getLightProjection(lightIndex);

        shadowProgram.setUniform("projection", lightProj);

        FramebufferAttachment<ContextType> attachment = shadowMaps.getLayerAsFramebufferAttachment(lightIndex);

        shadowFramebuffer.setDepthAttachment(attachment);
        shadowFramebuffer.clearDepthBuffer();

        for (Matrix4 m : this.multiTransformationModel)
        {
            shadowProgram.setUniform("model_view", getLightMatrix(lightIndex).times(m));
            shadowDrawable.draw(PrimitiveMode.TRIANGLES, shadowFramebuffer);
        }
    }

    private void setupLight(Program<ContextType> program, int lightIndex, int modelInstance)
    {
        Matrix4 lightMatrix = this.multiTransformationModel.get(modelInstance < 0 ? 0 : modelInstance).quickInverse(0.01f)
            .times(getLightMatrix(lightIndex));

        // lightMatrix can be hardcoded here (comment out previous line)

            // Contemporary gallery and stonewall
            //Matrix4.rotateY(16 * Math.PI / 16).times(Matrix4.rotateX(0 * Math.PI / 16))

            // Color studio 2:
            //Matrix4.rotateY(6 * Math.PI / 16).times(Matrix4.rotateX(0 * Math.PI / 16))

            // For the synthetic falcon example?
            //Matrix4.rotateY(5 * Math.PI / 4).times(Matrix4.rotateX(-Math.PI / 4))

            // Always end with this when hardcoding:
            //    .times(new Matrix4(new Matrix3(getDefaultCameraPose())));

        Matrix4 lightMatrixInverse = lightMatrix.quickInverse(0.001f);

        Vector3 lightPos = lightMatrixInverse.times(Vector4.ORIGIN).getXYZ();

        program.setUniform("lightPosVirtual[" + lightIndex + ']', lightPos);

        Vector3 controllerLightIntensity = lightingModel.getLightPrototype(lightIndex).getColor();
        float lightDistance = getLightMatrix(lightIndex).times(this.centroid.asPosition()).getXYZ().length();

        float lightScale = resources.viewSet.areLightSourcesInfinite() ? 1.0f :
                getDefaultCameraPose()
                        .times(resources.geometry.getCentroid().asPosition())
                    .getXYZ().length();

        program.setUniform("lightIntensityVirtual[" + lightIndex + ']',
                controllerLightIntensity.times(lightDistance * lightDistance * resources.viewSet.getLightIntensity(0).y / (lightScale * lightScale)));
        program.setUniform("lightMatrixVirtual[" + lightIndex + ']', getLightProjection(lightIndex).times(lightMatrix));
        program.setUniform("lightOrientationVirtual[" + lightIndex + ']',
            lightMatrixInverse.times(new Vector4(0.0f, 0.0f, -1.0f, 0.0f)).getXYZ());
        program.setUniform("lightSpotSizeVirtual[" + lightIndex + ']',
            (float)Math.sin(lightingModel.getLightPrototype(lightIndex).getSpotSize()));
        program.setUniform("lightSpotTaperVirtual[" + lightIndex + ']', lightingModel.getLightPrototype(lightIndex).getSpotTaper());
    }

    private Matrix4 getPartialViewMatrix()
    {
        float scale = getScale();

        return Matrix4.scale(scale)
                .times(cameraModel.getLookMatrix())
                .times(Matrix4.scale(1.0f / scale));
    }

    private Matrix4 getPartialViewMatrix(Matrix4 absoluteViewMatrix)
    {
        return absoluteViewMatrix
                .times(Matrix4.translate(this.centroid))
                .times(getDefaultCameraPose().getUpperLeft3x3().transpose().asMatrix4());
    }

    private Matrix4 getAbsoluteViewMatrix()
    {
        return getPartialViewMatrix()
                .times(getDefaultCameraPose().getUpperLeft3x3().asMatrix4())
                .times(Matrix4.translate(this.centroid.negated()));
    }

    @Override
    public Matrix4 getAbsoluteViewMatrix(Matrix4 relativeViewMatrix)
    {
        float scale = getScale();

        return Matrix4.scale(scale)
            .times(relativeViewMatrix)
            .times(Matrix4.scale(1.0f / scale))
            .times(getDefaultCameraPose().getUpperLeft3x3().asMatrix4())
            .times(Matrix4.translate(this.centroid.negated()));
    }

    public Matrix4 getFullViewMatrix(Matrix4 partialViewMatrix)
    {
        return partialViewMatrix
            .times(getDefaultCameraPose().getUpperLeft3x3().asMatrix4())
            .times(Matrix4.translate(this.centroid.negated()));
    }

    private Matrix4 getModelViewMatrix(Matrix4 partialViewMatrix, int modelInstance)
    {
        float scale = getScale();

        return partialViewMatrix
                .times(Matrix4.scale(scale))
                .times(this.objectModel.getTransformationMatrix())
                .times(Matrix4.scale(1.0f / scale))
                .times(getDefaultCameraPose().getUpperLeft3x3().asMatrix4())
                .times(Matrix4.translate(this.centroid.negated()))
                .times(multiTransformationModel.get(modelInstance));
    }

    private float getVerticalFieldOfView(FramebufferSize size)
    {
//        return resources.viewSet.getCameraProjection(
//                resources.viewSet.getCameraProjectionIndex(resources.viewSet.getPrimaryViewIndex()))
//            .getVerticalFieldOfView();
        return 2 * (float)Math.atan(Math.tan(cameraModel.getHorizontalFOV() / 2) * size.height / size.width);
    }

    private Matrix4 getProjectionMatrix(FramebufferSize size)
    {
        float scale = getScale();

        return Matrix4.perspective(getVerticalFieldOfView(size),
                (float)size.width / (float)size.height,
                0.01f * scale, 100.0f * scale);
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

    private float computeLightWidgetScale(Matrix4 partialViewMatrix, FramebufferSize size)
    {
        float cameraDistance = partialViewMatrix
            .times(this.cameraModel.getTarget().times(this.getScale()).asPosition())
            .getXYZ().length();
        return cameraDistance * Math.min(cameraModel.getHorizontalFOV(), getVerticalFieldOfView(size)) / 4;
    }

    private Matrix4 setupModelView(Program<ContextType> p, int modelInstance, Matrix4 view)
    {
        Matrix4 partialViewMatrix = getPartialViewMatrix(view);

        for (int lightIndex = 0; lightIndex < lightingModel.getLightCount(); lightIndex++)
        {
            setupLight(p, lightIndex, modelInstance);
        }

        // Draw instance
        Matrix4 modelView = modelInstance < 0 ? view : getModelViewMatrix(partialViewMatrix, modelInstance);

        p.setUniform("model_view", modelView);
        p.setUniform("viewPos", modelView.quickInverse(0.01f).getColumn(3).getXYZ());

        if (!this.settingsModel.getBoolean("relightingEnabled") && !settingsModel.getBoolean("lightCalibrationMode")
            && this.settingsModel.get("weightMode", ShadingParameterMode.class) == ShadingParameterMode.UNIFORM)
        {
            if (weightBuffer == null)
            {
                weightBuffer = context.createUniformBuffer();
            }
            weightBuffer.setData(this.generateViewWeights(view)); // TODO should this use modelView instead?
            program.setUniformBuffer("ViewWeights", weightBuffer);
        }

        return modelView;
    }

    private Matrix4 setupProjection(Program<ContextType> p, int fullWidth, int fullHeight, int x, int y, int width, int height, Matrix4 projection)
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

        return adjustedProjection;
    }

    private void drawReferenceScene(Framebuffer<ContextType> framebuffer, Matrix4 view, Matrix4 projection)
    {
        if (referenceSceneProgram != null && referenceScene != null && refScenePositions != null && refSceneNormals != null)
        {
            setupForDraw(referenceSceneProgram);
            referenceSceneProgram.setUniform("projection", projection);
            referenceSceneProgram.setUniform("objectID", this.sceneObjectIDLookup.get("SceneObject"));

            Drawable<ContextType> drawable = context.createDrawable(referenceSceneProgram);
            drawable.addVertexBuffer("position", refScenePositions);
            drawable.addVertexBuffer("normal", refSceneNormals);

            if (refSceneTexture != null && refSceneTexCoords != null)
            {
                drawable.addVertexBuffer("texCoord", refSceneTexCoords);
                referenceSceneProgram.setTexture("diffuseMap", refSceneTexture);
            }

            referenceSceneProgram.setUniform("model_view", view);
            referenceSceneProgram.setUniform("viewPos", view.quickInverse(0.01f).getColumn(3).getXYZ());

            // Do first pass at half resolution to off-screen buffer
            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
        }
    }

    @Override
    public void draw(Framebuffer<ContextType> framebuffer)
    {
        FramebufferSize framebufferSize = framebuffer.getSize();
        draw(framebuffer, null, null, framebufferSize.width, framebufferSize.height,
            true, true);
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
            .plus(settingsModel.get("currentLightCalibration", Vector2.class).asVector3());
        this.newLightCalibrationAvailable = true;
    }


    @Override
    public void draw(Framebuffer<ContextType> framebuffer, Matrix4 viewOverride, Matrix4 projectionOverride, int subdivWidth, int subdivHeight)
    {
        this.draw(framebuffer, viewOverride, projectionOverride, subdivWidth, subdivHeight,
            false, false);
    }

    private void draw(Framebuffer<ContextType> framebuffer, Matrix4 viewOverride, Matrix4 projectionOverride,
        int subdivWidth, int subdivHeight, boolean useShadingFramebuffers, boolean progressShadingFramebuffers)
    {
        Instant frameStart = Instant.now();

        boolean overriddenViewMatrix = viewOverride != null;

        Matrix4 view = this.getAbsoluteViewMatrix();

        try
        {
            if(this.settingsModel.getBoolean("multisamplingEnabled"))
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
                view = viewOverride;
            }
            else if (settingsModel.getBoolean("lightCalibrationMode"))
            {
                lightCalibrationMode = true;
                overriddenViewMatrix = true;

                int primaryLightIndex = this.resources.viewSet.getLightIndex(this.resources.viewSet.getPrimaryViewIndex());

                Vector3 lightPosition = settingsModel.get("currentLightCalibration", Vector2.class).asVector3()
                                            .plus(resources.viewSet.getLightPosition(primaryLightIndex));
                Matrix4 lightTransform = Matrix4.translate(lightPosition.negated());

                Matrix4 partialViewInverse = getPartialViewMatrix().quickInverse(0.01f);
                float maxSimilarity = -1.0f;

                for(int i = 0; i < this.resources.viewSet.getCameraPoseCount(); i++)
                {
                    Matrix4 candidateView = this.resources.viewSet.getCameraPose(i);

                    float similarity = partialViewInverse.times(Vector4.ORIGIN).getXYZ()
                        .dot(getPartialViewMatrix(candidateView).quickInverse(0.01f).times(Vector4.ORIGIN).getXYZ());

                    if (similarity > maxSimilarity)
                    {
                        maxSimilarity = similarity;
                        view = lightTransform.times(candidateView);
                        snapViewIndex = i;
                    }
                }
            }

            Matrix4 partialViewMatrix;

            if (overriddenViewMatrix)
            {
                partialViewMatrix = getPartialViewMatrix(view);

                if (lightingModel instanceof CameraBasedLightingModel)
                {
                    float scale = getDefaultCameraPose()
                        .times(resources.geometry.getCentroid().asPosition())
                        .getXYZ().length();

                    ((CameraBasedLightingModel) lightingModel).overrideCameraPose(
                        Matrix4.scale(1.0f / scale)
                            .times(view)
                            .times(Matrix4.translate(resources.geometry.getCentroid()))
                            .times(getDefaultCameraPose().transpose().getUpperLeft3x3().asMatrix4())
                            .times(Matrix4.scale(scale)));
                }
            }
            else
            {
                partialViewMatrix = getPartialViewMatrix();
            }

            FramebufferSize size = framebuffer.getSize();

            Matrix4 projection = projectionOverride;

            if (projection == null)
            {
                projection = this.getProjectionMatrix(size);
            }

            int fboWidth = size.width;
            int fboHeight = size.height;

            if (settingsModel.getBoolean("halfResolutionEnabled"))
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

                if (backplateTexture != null && lightingModel.getBackgroundMode() == BackgroundMode.IMAGE)
                {
                    tintedTexDrawable.program().setTexture("tex", backplateTexture);
                    tintedTexDrawable.program().setUniform("color", clearColor);

                    context.getState().disableDepthTest();
                    tintedTexDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
                    context.getState().enableDepthTest();

                    // Clear ID buffer again.
                    offscreenFBO.clearIntegerColorBuffer(1, 0, 0, 0, 0);
                }
                else if (environmentMap != null && lightingModel.getBackgroundMode() == BackgroundMode.ENVIRONMENT_MAP)
                {
                    float scale = getScale();
                    Matrix4 envMapMatrix = Matrix4.scale(scale)
                        .times(lightingModel.getEnvironmentMapMatrix())
                        .times(Matrix4.scale(1.0f / scale))
                        .times(getDefaultCameraPose().getUpperLeft3x3().asMatrix4())
                        .times(Matrix4.translate(this.centroid.negated()));

                    environmentBackgroundProgram.setUniform("objectID", this.sceneObjectIDLookup.get("EnvironmentMap"));
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

                if (shadowMaps.getDepth() < lightingModel.getLightCount())
                {
                    shadowMaps.close();
                    shadowMaps = null;
                    shadowMaps = context.getTextureFactory().build2DDepthTextureArray(2048, 2048, lightingModel.getLightCount()).createTexture();
                }

                for (int lightIndex = 0; lightIndex < lightingModel.getLightCount(); lightIndex++)
                {
                    generateShadowMaps(lightIndex);
                }

                // Draw grid
                if (settingsModel.getBoolean("is3DGridEnabled"))
                {
                    this.solidProgram.setUniform("projection", this.getProjectionMatrix(size));
                    this.solidProgram.setUniform("model_view", partialViewMatrix.times(Matrix4.scale(this.getScale())));
                    this.solidProgram.setUniform("color", new Vector4(0.5f, 0.5f, 0.5f, 1.0f));
                    this.solidProgram.setUniform("objectID", 0);
                    this.gridDrawable.draw(PrimitiveMode.LINES, offscreenFBO);
                }

                context.getState().disableBackFaceCulling();

                drawReferenceScene(offscreenFBO, view, projection);

                if (false && useShadingFramebuffers && multiTransformationModel.size() == 1)
                {
                    if (progressShadingFramebuffers)
                    {
                        if (frameInProgress == null)
                        {
                            FramebufferObject<ContextType> fboToUse = null;

                            if (shadedFrames.size() == SHADING_FRAMEBUFFER_COUNT)
                            {
                                ShadedFrame<ContextType> frameToDiscard = shadedFrames.poll();
                                fboToUse = frameToDiscard.framebuffer;

                                if (!Objects.equals(fboToUse.getSize(), size))
                                {
                                    shadingFramebuffers.remove(fboToUse);
                                    fboToUse.close();
                                    fboToUse = null;
                                }
                            }

                            if (fboToUse == null)
                            {
                                fboToUse =
                                    context.buildFramebufferObject(size.width, size.height)
                                        .addColorAttachment(
                                            ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB8)
                                                .setLinearFilteringEnabled(true))
                                        .addDepthAttachment()
                                        .createFramebufferObject();

                                shadingFramebuffers.add(fboToUse);
                            }

                            Matrix4 modelView = getModelViewMatrix(partialViewMatrix, 0);

                            float distance = modelView.times(resources.geometry.getCentroid().asPosition()).getXYZ().length();

                            float radius = modelView.times(new Vector3(1).normalized()
                                .times(resources.geometry.getBoundingRadius()).asDirection())
                                .getXYZ().length();

                            Matrix4 shadingProjection = Matrix4.perspective(getVerticalFieldOfView(size),
                                (float) size.width / (float) size.height,
                                distance - radius, distance + radius);

                            frameInProgress = new ShadedFrame<>(fboToUse, modelView, shadingProjection); // TODO predict future view matrix

                            fboToUse.clearColorBuffer(0, 0, 0, 0, 0);
                            fboToUse.clearDepthBuffer();

                            shadingX = 0;
                            shadingY = 0;

                            Instant currentTime = Instant.now();

                            int currentSubdivCount = (int)(Math.ceil((float)size.width / Math.round(shadingWidth))
                                                            * Math.ceil((float)size.height / Math.round(shadingHeight)));

                            if (lastShadingFrameStart != null &&
                                Duration.between(lastShadingFrameStart, currentTime)
                                    .compareTo(Duration.ofMillis(1000L * currentSubdivCount / (4 * TARGET_FPS))) < 0)
                            {
                                // Bump up the block size
                                shadingWidth *= 2;
                                shadingHeight *= 2;

                                if (shadingWidth > MAX_SHADING_DIMENSION)
                                {
                                    shadingWidth = MAX_SHADING_DIMENSION;
                                }

                                if (shadingHeight > MAX_SHADING_DIMENSION)
                                {
                                    shadingHeight = MAX_SHADING_DIMENSION;
                                }
                            }

                            System.out.println(shadingWidth + "x" + shadingHeight);

                            lastShadingFrameStart = currentTime;
                        }
                        else if (lastFrameStart != null &&
                            Duration.between(lastFrameStart, frameStart).compareTo(Duration.ofMillis(1000 / TARGET_FPS)) > 0)
                        {
                            // Emergency block size reduction
                            shadingWidth /= 2;
                            shadingHeight /= 2;

                            if (shadingWidth < MIN_SHADING_DIMENSION)
                            {
                                shadingWidth = MIN_SHADING_DIMENSION;
                            }

                            if (shadingHeight < MIN_SHADING_DIMENSION)
                            {
                                shadingHeight = MIN_SHADING_DIMENSION;
                            }

                            //System.out.println(shadingWidth + "x" + shadingHeight);
                        }

                        setupForDraw(this.program);
                        setupModelView(this.program, -1, frameInProgress.modelView);
                        setupProjection(this.program, size.width, size.height, shadingX, shadingY,
                            (int) Math.round(shadingWidth), (int) Math.round(shadingHeight), frameInProgress.projection);

                        // Render to off-screen buffer
                        mainDrawable.draw(PrimitiveMode.TRIANGLES, framebuffer, shadingX, shadingY,
                            (int) Math.round(shadingWidth), (int) Math.round(shadingHeight));

                        // Flush to prevent timeout
                        context.flush();

                        FramebufferSize sizeInProgress = frameInProgress.framebuffer.getSize();
                        shadingX += Math.round(shadingWidth);
                        if (shadingX >= sizeInProgress.width)
                        {
                            shadingX = 0;
                            shadingY += Math.round(shadingHeight);
                        }

                        if (shadingY >= sizeInProgress.height)
                        {
                            shadedFrames.add(frameInProgress);
                            frameInProgress = null;
                        }
                    }

                    ShadedFrame<ContextType> primaryFrame = shadedFrames.peekLast();

                    this.reprojectProgram.setUniform("objectID", this.sceneObjectIDLookup.get("IBRObject"));
                    this.reprojectProgram.setUniform("depthTestingEnabled", true);
                    this.reprojectProgram.setUniform("depthTestBias", 0.0025f);
                    this.reprojectProgram.setUniform("prerenderedSecondaryWeight", 0.0f); // For now, TODO use secondary image?
                    this.reprojectProgram.setUniform("prerenderedModelViewPrimary", primaryFrame.modelView);
                    this.reprojectProgram.setUniform("prerenderedProjectionPrimary", primaryFrame.projection);
                    this.reprojectProgram.setTexture("prerenderedImagePrimary", primaryFrame.framebuffer.getColorAttachmentTexture(0));
                    this.reprojectProgram.setTexture("prerenderedDepthImagePrimary", primaryFrame.framebuffer.getDepthAttachmentTexture());

                    setupForDraw(this.reprojectProgram);
                    setupModelView(this.reprojectProgram, 0, view);
                    this.reprojectProgram.setUniform("projection", projection);

                    // Render to off-screen buffer
                    reprojectDrawable.draw(PrimitiveMode.TRIANGLES, offscreenFBO, 0, 0, fboWidth, fboHeight);

                    // Flush to prevent timeout
                    context.flush();
                }
                else
                {
                    setupForDraw(this.program);

                    this.program.setUniform("objectID", this.sceneObjectIDLookup.get("IBRObject"));

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

                    for (int i = 0; i < this.multiTransformationModel.size(); i++)
                    {
                        setupModelView(shadowProgram, i, view);
                        shadowDrawable.draw(PrimitiveMode.TRIANGLES, screenSpaceDepthFBO);
                    }

                    context.flush();

                    this.program.setTexture("screenSpaceDepthBuffer", screenSpaceDepthFBO.getDepthAttachmentTexture());

                    if (this.resources.eigentextures != null && lightingModel != null && !Objects.equals(lightingModel.getAmbientLightColor(), Vector3.ZERO))
                    {
                        this.environmentWeightsProgram.setTexture("screenSpaceDepthBuffer", screenSpaceDepthFBO.getDepthAttachmentTexture());
                        setupForDraw(this.environmentWeightsProgram);
                        setupModelView(this.environmentWeightsProgram, 0, view);
                        this.environmentWeightsProgram.setUniform("fullProjection", projection);
                        this.environmentWeightsProgram.setTexture("positionMap", this.resources.blockPositionTexture);
                        this.environmentWeightsProgram.setTexture("normalMap", this.resources.blockNormalTexture);

                        FramebufferSize environmentWeightsSize = environmentWeightsFBO.getSize();

                        for (int k = 0; k < 4; k++)
                        {
                            environmentWeightsProgram.setUniform("startingSVIndex", k * 4 - 1);

                            for (int i = 0; i < 8; i++)
                            {
                                environmentWeightsFBO.setColorAttachment(i,
                                    environmentWeightsTexture.getLayerAsFramebufferAttachment(i + 8 * k));
                                environmentWeightsFBO.clearColorBuffer(i, 0.0f, 0.0f, 0.0f, 0.0f);
                            }

                            environmentWeightsProgram.setUniform("blockOffset", new IntVector2(0, 0));
                            environmentWeightsDrawable.draw(PrimitiveMode.TRIANGLE_FAN, environmentWeightsFBO, 0, 0,
                                environmentWeightsSize.width, environmentWeightsSize.height);

                            for (int i = 0; i < 8; i++)
                            {
                                environmentWeightsFBO.setColorAttachment(i,
                                    environmentWeightsTexture.getLayerAsFramebufferAttachment(i + 8 * k + 32));
                                environmentWeightsFBO.clearColorBuffer(i, 0.0f, 0.0f, 0.0f, 0.0f);
                            }

                            environmentWeightsProgram.setUniform("blockOffset", new IntVector2(0, 1));
                            environmentWeightsDrawable.draw(PrimitiveMode.TRIANGLE_FAN, environmentWeightsFBO, 0, 0,
                                environmentWeightsSize.width, environmentWeightsSize.height);

                            for (int i = 0; i < 8; i++)
                            {
                                environmentWeightsFBO.setColorAttachment(i,
                                    environmentWeightsTexture.getLayerAsFramebufferAttachment(i + 8 * k + 64));
                                environmentWeightsFBO.clearColorBuffer(i, 0.0f, 0.0f, 0.0f, 0.0f);
                            }

                            environmentWeightsProgram.setUniform("blockOffset", new IntVector2(1, 0));
                            environmentWeightsDrawable.draw(PrimitiveMode.TRIANGLE_FAN, environmentWeightsFBO, 0, 0,
                                environmentWeightsSize.width, environmentWeightsSize.height);

                            for (int i = 0; i < 8; i++)
                            {
                                environmentWeightsFBO.setColorAttachment(i,
                                    environmentWeightsTexture.getLayerAsFramebufferAttachment(i + 8 * k + 96));
                                environmentWeightsFBO.clearColorBuffer(i, 0.0f, 0.0f, 0.0f, 0.0f);
                            }

                            environmentWeightsProgram.setUniform("blockOffset", new IntVector2(1, 1));
                            environmentWeightsDrawable.draw(PrimitiveMode.TRIANGLE_FAN, environmentWeightsFBO, 0, 0,
                                environmentWeightsSize.width, environmentWeightsSize.height);
                        }

                        // Flush to prevent timeout
                        context.flush();

                        this.program.setTexture("environmentWeightsTexture", environmentWeightsTexture);
                        drawModelInSubdivisions(this.mainDrawable, offscreenFBO, subdivWidth, subdivHeight,
                            lightCalibrationMode ? -1 : 0, view, projection);
                    }
                    else
                    {
                        for (int modelInstance = 0; modelInstance < (lightCalibrationMode ? 1 : multiTransformationModel.size()); modelInstance++)
                        {
                            drawModelInSubdivisions(this.mainDrawable, offscreenFBO, subdivWidth, subdivHeight,
                                lightCalibrationMode ? -1 : modelInstance, view, projection);
                        }
                    }
                }

                context.getState().enableBackFaceCulling();

                if (!lightingModel.areLightWidgetsEthereal()
                    && IntStream.range(0, lightingModel.getLightCount()).anyMatch(lightingModel::isLightWidgetEnabled))
                {
                    context.flush();

                    // Read buffers here if light widgets are ethereal (i.e. they cannot be clicked and should not be in the ID buffer)
                    fboSize = offscreenFBO.getSize();

                    if (pixelObjectIDBuffer == null || pixelObjectIDBuffer.capacity() != 4 * fboSize.width * fboSize.height)
                    {
                        pixelObjectIDBuffer = BufferUtils.createIntBuffer(4 * fboSize.width * fboSize.height);
                    }
                    else
                    {
                        pixelObjectIDBuffer.clear();
                    }

                    if (pixelDepthBuffer == null || pixelDepthBuffer.capacity() != fboSize.width * fboSize.height)
                    {
                        pixelDepthBuffer = BufferUtils.createShortBuffer(fboSize.width * fboSize.height);
                    }
                    else
                    {
                        pixelDepthBuffer.clear();
                    }

                    offscreenFBO.readIntegerColorBufferRGBA(1, pixelObjectIDBuffer);
                    offscreenFBO.readDepthBuffer(pixelDepthBuffer);
                }

                drawLights(offscreenFBO, view, partialViewMatrix);

                // Finish drawing
                context.flush();

                // Second pass at full resolution to default framebuffer
                simpleTexDrawable.program().setTexture("tex", offscreenFBO.getColorAttachmentTexture(0));

                framebuffer.clearDepthBuffer();
                simpleTexDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);

                context.flush();

                if (!lightingModel.areLightWidgetsEthereal()
                    && IntStream.range(0, lightingModel.getLightCount()).anyMatch(lightingModel::isLightWidgetEnabled))
                {
                    // Read buffers here if light widgets are not ethereal (i.e. they can be clicked and should be in the ID buffer)
                    fboSize = offscreenFBO.getSize();

                    if (pixelObjectIDBuffer == null || pixelObjectIDBuffer.capacity() != 4 * fboSize.width * fboSize.height)
                    {
                        pixelObjectIDBuffer = BufferUtils.createIntBuffer(4 * fboSize.width * fboSize.height);
                    }
                    else
                    {
                        pixelObjectIDBuffer.clear();
                    }

                    if (pixelDepthBuffer == null || pixelDepthBuffer.capacity() != fboSize.width * fboSize.height)
                    {
                        pixelDepthBuffer = BufferUtils.createShortBuffer(fboSize.width * fboSize.height);
                    }
                    else
                    {
                        pixelDepthBuffer.clear();
                    }

                    offscreenFBO.readIntegerColorBufferRGBA(1, pixelObjectIDBuffer);
                    offscreenFBO.readDepthBuffer(pixelDepthBuffer);
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
        finally
        {
            if (overriddenViewMatrix && lightingModel instanceof CameraBasedLightingModel)
            {
                ((CameraBasedLightingModel)lightingModel).removeCameraPoseOverride();
            }

            lastFrameStart = frameStart;
        }
    }

    private void drawModelInSubdivisions(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int subdivWidth, int subdivHeight,
        int modelInstance, Matrix4 view, Matrix4 projection)
    {
        FramebufferSize fullFBOSize = framebuffer.getSize();

        // Optionally render in subdivisions to prevent GPU timeout
        for (int x = 0; x < fullFBOSize.width; x += subdivWidth)
        {
            for (int y = 0; y < fullFBOSize.height; y += subdivHeight)
            {
                int effectiveWidth = Math.min(subdivWidth, fullFBOSize.width - x);
                int effectiveHeight = Math.min(subdivHeight, fullFBOSize.height - y);

                setupModelView(drawable.program(), modelInstance, view);
                setupProjection(drawable.program(), fullFBOSize.width, fullFBOSize.height, x, y, effectiveWidth, effectiveHeight, projection);

                // Render to off-screen buffer
                drawable.draw(PrimitiveMode.TRIANGLES, framebuffer, x, y, effectiveWidth, effectiveHeight);

                // Flush to prevent timeout
                context.flush();
            }
        }
    }

    private void drawLights(Framebuffer<ContextType> framebuffer, Matrix4 viewMatrix, Matrix4 partialViewMatrix)
    {
        FramebufferSize size = framebuffer.getSize();

        if (this.settingsModel.getBoolean("relightingEnabled") && this.settingsModel.getBoolean("visibleLightsEnabled")
            && !settingsModel.getBoolean("lightCalibrationMode"))
        {
            this.context.getState().disableDepthWrite();

            // Draw lights
            for (int i = 0; i < lightingModel.getLightCount(); i++)
            {
                this.context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
                this.context.getState().enableDepthTest();

                if (settingsModel.getBoolean("lightWidgetsEnabled") && lightingModel.isLightWidgetEnabled(i)
                    && lightingModel.getLightWidgetModel(i).isCenterWidgetVisible())
                {
                    this.lightProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light." + i + ".Center"));

                    Vector3 lightCenter = partialViewMatrix.times(this.lightingModel.getLightCenter(i).times(this.getScale()).asPosition()).getXYZ();

                    this.lightProgram.setUniform("model_view",
                        Matrix4.translate(lightCenter)
                            .times(Matrix4.scale(
                                -lightCenter.z * getVerticalFieldOfView(size) / 64.0f,
                                -lightCenter.z * getVerticalFieldOfView(size) / 64.0f,
                                1.0f)));
                    this.lightProgram.setUniform("projection", this.getProjectionMatrix(size));

                    this.lightProgram.setTexture("lightTexture", this.lightCenterTexture);

                    this.context.getState().disableDepthTest();
                    this.lightProgram.setUniform("color",
                        new Vector3(this.lightingModel.getLightWidgetModel(i).isCenterWidgetSelected() ? 1.0f : 0.5f));
                    this.lightDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
                }

                float scale = getScale();
                Matrix4 widgetTransformation = viewMatrix
                    .times(Matrix4.translate(this.centroid))
                    .times(getDefaultCameraPose().getUpperLeft3x3().transpose().asMatrix4())
                    .times(Matrix4.scale(scale))
                    .times(lightingModel.getLightMatrix(i).quickInverse(0.01f))
                    .times(Matrix4.scale(1.0f / scale));

                if (lightingModel.isLightVisualizationEnabled(i))
                {
                    this.context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
                    this.lightProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light." + i));
                    this.lightProgram.setUniform("color", lightingModel.getLightPrototype(i).getColor().times((float)Math.PI));

                    Vector3 lightPosition = widgetTransformation.getColumn(3).getXYZ();

                    this.lightProgram.setUniform("model_view",
                        Matrix4.translate(lightPosition)
                            .times(Matrix4.scale(-lightPosition.z / 32.0f, -lightPosition.z / 32.0f, 1.0f)));
                    this.lightProgram.setUniform("projection", this.getProjectionMatrix(size));
                    this.lightProgram.setTexture("lightTexture", this.lightTexture);
                    this.lightDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
                }

                if (settingsModel.getBoolean("lightWidgetsEnabled") && lightingModel.isLightWidgetEnabled(i))
                {
                    this.solidProgram.setUniform("projection", this.getProjectionMatrix(size));

                    float lightWidgetScale = computeLightWidgetScale(partialViewMatrix, size);
                    Vector3 lightCenter = partialViewMatrix.times(this.lightingModel.getLightCenter(i).times(this.getScale()).asPosition()).getXYZ();
                    Vector3 widgetPosition = widgetTransformation.getColumn(3).getXYZ()
                        .minus(lightCenter)
                        .normalized()
                        .times(lightWidgetScale)
                        .plus(lightCenter);
                    Vector3 widgetDisplacement = widgetPosition.minus(lightCenter);
                    float widgetDistance = widgetDisplacement.length();

                    Vector3 distanceWidgetPosition = widgetTransformation.getColumn(3).getXYZ()
                        .minus(lightCenter)
                        .times(Math.min(1, computeLightWidgetScale(partialViewMatrix, size) /
                            widgetTransformation.getColumn(3).getXYZ().distance(lightCenter)))
                        .plus(lightCenter);

                    float perspectiveWidgetScale = -widgetPosition.z * getVerticalFieldOfView(size) / 128;

                    this.context.getState().disableDepthTest();
                    this.context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));

                    if (lightingModel.getLightWidgetModel(i).isDistanceWidgetVisible() || lightingModel.getLightWidgetModel(i).isCenterWidgetVisible())
                    {
                        Vector3 lineEndpoint = widgetPosition.minus(lightCenter)
                            .times(0.5f / widgetPosition.getXY().distance(lightCenter.getXY()))
                            .minus(lightCenter);

                        try
                            (
                                VertexBuffer<ContextType> line =
                                    context.createVertexBuffer()
                                        .setData(NativeVectorBufferFactory.getInstance()
                                            .createFromFloatArray(3, 2, lineEndpoint.x, lineEndpoint.y, lineEndpoint.z, lightCenter.x, lightCenter.y, lightCenter.z))
                            )
                        {
                            Drawable<ContextType> lineRenderable = context.createDrawable(this.solidProgram);
                            lineRenderable.addVertexBuffer("position", line);
                            this.solidProgram.setUniform("model_view", Matrix4.IDENTITY);
                            this.solidProgram.setUniform("color",
                                new Vector3(lightingModel.getLightWidgetModel(i).isDistanceWidgetSelected()
                                    || lightingModel.getLightWidgetModel(i).isCenterWidgetSelected() ? 1.0f : 0.5f)
                                    .asVector4(1));
                            this.solidProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light." + i + ".Distance"));
                            lineRenderable.draw(PrimitiveMode.LINES, framebuffer);
                        }
                    }

                    if (lightingModel.getLightWidgetModel(i).isInclinationWidgetVisible()
                        && lightingModel.getLightWidgetModel(i).isInclinationWidgetSelected())
                    {
                        Vector3 lineEndpoint1 = lightCenter
                            .plus(partialViewMatrix.times(new Vector4(0,widgetDistance,0,0)).getXYZ());
                        Vector3 lineEndpoint2 = lightCenter
                            .plus(partialViewMatrix.times(new Vector4(0,-widgetDistance,0,0)).getXYZ());

                        try
                            (
                                VertexBuffer<ContextType> line =
                                    context.createVertexBuffer()
                                        .setData(NativeVectorBufferFactory.getInstance()
                                            .createFromFloatArray(3, 2, lineEndpoint1.x, lineEndpoint1.y, lineEndpoint1.z, lineEndpoint2.x, lineEndpoint2.y, lineEndpoint2.z))
                            )
                        {
                            Drawable<ContextType> lineRenderable = context.createDrawable(this.solidProgram);
                            lineRenderable.addVertexBuffer("position", line);
                            this.solidProgram.setUniform("model_view", Matrix4.IDENTITY);
                            this.solidProgram.setUniform("color",
                                new Vector3(lightingModel.getLightWidgetModel(i).isDistanceWidgetSelected()
                                    || lightingModel.getLightWidgetModel(i).isCenterWidgetSelected() ? 1.0f : 0.5f)
                                    .asVector4(1));
                            this.solidProgram.setUniform("objectID", 0);
                            lineRenderable.draw(PrimitiveMode.LINES, framebuffer);
                        }
                    }

                    Vector3 azimuthRotationAxis = partialViewMatrix.times(new Vector4(0,1,0,0)).getXYZ();

                    this.circleProgram.setUniform("color", new Vector3(1));
                    this.circleProgram.setUniform("projection", this.getProjectionMatrix(size));
                    this.circleProgram.setUniform("width", 1 / 128.0f);
                    this.circleProgram.setUniform("maxAngle", (float)Math.PI / 4);
                    this.circleProgram.setUniform("threshold", 0.005f);

                    Vector3 lightDisplacementAtInclination = widgetDisplacement
                        .minus(azimuthRotationAxis.times(widgetDisplacement.dot(azimuthRotationAxis)));
                    float lightDistanceAtInclination = lightDisplacementAtInclination.length();

                    context.getState().disableBackFaceCulling();

                    Vector3 lightDisplacementWorld = partialViewMatrix.quickInverse(0.01f)
                        .times(widgetDisplacement.asDirection()).getXYZ();

                    double azimuth = Math.atan2(lightDisplacementWorld.x, lightDisplacementWorld.z);
                    double inclination = Math.asin(lightDisplacementWorld.normalized().y);

                    float cosineLightToPole = widgetDisplacement.normalized().dot(azimuthRotationAxis);
                    double azimuthArrowRotation = Math.min(Math.PI / 4,
                        16 * perspectiveWidgetScale / (widgetDistance * Math.sqrt(1 - cosineLightToPole * cosineLightToPole)));

                    double inclinationArrowRotation = Math.min(Math.PI / 4, 16 * perspectiveWidgetScale / widgetDistance);

                    if (lightingModel.getLightWidgetModel(i).isAzimuthWidgetVisible() &&
                        (Math.abs(lightDisplacementWorld.x) > 0.001f || Math.abs(lightDisplacementWorld.z) > 0.001f))
                    {
                        // Azimuth circle
                        this.circleProgram.setUniform("maxAngle",
                            (float) (lightingModel.getLightWidgetModel(i).isAzimuthWidgetSelected() ?
                                Math.PI : azimuthArrowRotation));
                        this.circleProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light." + i + ".Azimuth"));
                        this.circleProgram.setUniform("color",
                            new Vector3(lightingModel.getLightWidgetModel(i).isAzimuthWidgetSelected() ? 1.0f :0.5f));
                        this.circleProgram.setUniform("model_view",
                            Matrix4.translate(lightCenter.plus(azimuthRotationAxis.times(widgetDisplacement.dot(azimuthRotationAxis))))
                                .times(partialViewMatrix.getUpperLeft3x3().asMatrix4())
                                .times(Matrix4.scale(2 * lightDistanceAtInclination))
                                .times(Matrix4.rotateX(-Math.PI / 2))
                                .times(Matrix4.rotateZ(azimuth - Math.PI / 2)));
                        this.circleDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
                    }

                    if (lightingModel.getLightWidgetModel(i).isInclinationWidgetVisible())
                    {
                        // Inclination circle
                        this.circleProgram.setUniform("maxAngle",
                            (float) (lightingModel.getLightWidgetModel(i).isInclinationWidgetSelected() ?
                                Math.PI / 2 : inclinationArrowRotation));
                        this.circleProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light." + i + ".Inclination"));
                        this.circleProgram.setUniform("color",
                            new Vector3(lightingModel.getLightWidgetModel(i).isInclinationWidgetSelected() ? 1.0f : 0.5f));
                        this.circleProgram.setUniform("model_view",
                            Matrix4.translate(lightCenter)
                                .times(Matrix4.scale(2 * widgetDistance))
                                .times(widgetTransformation.getUpperLeft3x3()
                                    .times(Matrix3.rotateY(-Math.PI / 2))
                                    .times(lightingModel.getLightWidgetModel(i).isInclinationWidgetSelected() ?
                                        Matrix3.rotateZ(-inclination) : Matrix3.IDENTITY)
                                    .asMatrix4()));
                        this.circleDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
                    }

                    context.getState().enableBackFaceCulling();

                    Vector3 arrow1PositionR = Matrix3.rotateAxis(azimuthRotationAxis, azimuthArrowRotation)
                        .times(widgetPosition.minus(lightCenter))
                        .plus(lightCenter);

                    Vector3 arrow1PositionL = Matrix3.rotateAxis(azimuthRotationAxis, -azimuthArrowRotation)
                        .times(widgetPosition.minus(lightCenter))
                        .plus(lightCenter);

                    Vector3 arrow2PositionR = widgetTransformation.getUpperLeft3x3()
                        .times(Matrix3.rotateX(-inclinationArrowRotation))
                        .times(widgetTransformation.quickInverse(0.01f).getUpperLeft3x3())
                        .times(widgetPosition.minus(lightCenter))
                        .plus(lightCenter);

                    Vector3 arrow2PositionL = widgetTransformation.getUpperLeft3x3()
                        .times(Matrix3.rotateX(inclinationArrowRotation))
                        .times(widgetTransformation.quickInverse(0.01f).getUpperLeft3x3())
                        .times(widgetPosition.minus(lightCenter))
                        .plus(lightCenter);

                    Vector4 arrow1RDirectionY =  Matrix3.rotateAxis(azimuthRotationAxis, azimuthArrowRotation)
                        .times(widgetTransformation.getUpperLeft3x3())
                        .times(new Vector3(1,0,0))
                        .getXY().normalized().asDirection();

                    Vector4 arrow1LDirectionY =  Matrix3.rotateAxis(azimuthRotationAxis, -azimuthArrowRotation)
                        .times(widgetTransformation.getUpperLeft3x3())
                        .times(new Vector3(1,0,0))
                        .getXY().normalized().asDirection();

                    Vector4 arrow2RDirectionY = widgetTransformation.getUpperLeft3x3()
                        .times(Matrix3.rotateX(-inclinationArrowRotation))
                        .times(new Vector3(0,1,0))
                        .getXY().normalized().asDirection();

                    Vector4 arrow2LDirectionY = widgetTransformation.getUpperLeft3x3()
                        .times(Matrix3.rotateX(inclinationArrowRotation))
                        .times(new Vector3(0,1,0))
                        .getXY().normalized().asDirection();

                    // TODO account for perspective distortion in arrow orientation
                    Vector4 arrow1RDirectionX = new Vector4(arrow1RDirectionY.y, -arrow1RDirectionY.x, 0, 0).normalized();
                    Vector4 arrow1LDirectionX = new Vector4(arrow1LDirectionY.y, -arrow1LDirectionY.x, 0, 0).normalized();
                    Vector4 arrow2RDirectionX = new Vector4(arrow2RDirectionY.y, -arrow2RDirectionY.x, 0, 0).normalized();
                    Vector4 arrow2LDirectionX = new Vector4(arrow2LDirectionY.y, -arrow2LDirectionY.x, 0, 0).normalized();


                    Vector4 arrow3DirectionY = lightCenter.minus(widgetPosition).getXY().normalized().asDirection();
                    Vector4 arrow3DirectionX = new Vector4(arrow3DirectionY.y, -arrow3DirectionY.x, 0, 0).normalized();

                    Vector3 arrow3PositionR = distanceWidgetPosition.minus(widgetDisplacement.times(0.5f));
                    Vector3 arrow3PositionL = distanceWidgetPosition.plus(widgetDisplacement.times(0.5f));

//                            Vector3 arrow1PositionR = widgetTransformation.times(new Vector4(1,0,0,1)).getXYZ();
//                            Vector3 arrow1PositionL = widgetTransformation.times(new Vector4(-1,0,0,1)).getXYZ();
//                            Vector3 arrow2PositionR = widgetTransformation.times(new Vector4(0,1,0,1)).getXYZ();
//                            Vector3 arrow2PositionL = widgetTransformation.times(new Vector4(0,-1,0,1)).getXYZ();
//                            Vector3 arrow3PositionR = widgetTransformation.times(new Vector4(0,0,1,1)).getXYZ();
//                            Vector3 arrow3PositionL = widgetTransformation.times(new Vector4(0,0,-1,1)).getXYZ();

                    this.context.getState().disableDepthTest();
                    this.context.getState().disableAlphaBlending();
                    this.solidProgram.setUniform("color", new Vector4(1));

                    if (lightingModel.getLightWidgetModel(i).isAzimuthWidgetVisible() &&
                        (Math.abs(lightDisplacementWorld.x) > 0.001f || Math.abs(lightDisplacementWorld.z) > 0.001f))
                    {
                        this.solidProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light." + i + ".Azimuth"));

                        this.solidProgram.setUniform("model_view",
                            Matrix4.translate(arrow1PositionR)
                                .times(Matrix4.scale(perspectiveWidgetScale, perspectiveWidgetScale, 1.0f))
                                .times(Matrix4.fromColumns(
                                    arrow1RDirectionX,
                                    arrow1RDirectionY,
                                    new Vector4(0, 0, 1, 0),
                                    new Vector4(0, 0, 0, 1))));

                        this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);

                        this.solidProgram.setUniform("model_view",
                            Matrix4.translate(arrow1PositionL)
                                .times(Matrix4.scale(perspectiveWidgetScale, perspectiveWidgetScale, 1.0f))
                                .times(Matrix4.fromColumns(
                                    arrow1LDirectionX.negated(),
                                    arrow1LDirectionY.negated(),
                                    new Vector4(0, 0, 1, 0),
                                    new Vector4(0, 0, 0, 1))));

                        this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
                    }

                    if (lightingModel.getLightWidgetModel(i).isInclinationWidgetVisible())
                    {
                        this.solidProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light." + i + ".Inclination"));

                        if (Math.PI / 2 - inclination > 0.01f)
                        {
                            this.solidProgram.setUniform("model_view",
                                Matrix4.translate(arrow2PositionR)
                                    .times(Matrix4.scale(perspectiveWidgetScale, perspectiveWidgetScale, 1.0f))
                                    .times(Matrix4.fromColumns(
                                        arrow2RDirectionX,
                                        arrow2RDirectionY,
                                        new Vector4(0, 0, 1, 0),
                                        new Vector4(0, 0, 0, 1))));
                            this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
                        }

                        if (Math.PI / 2 + inclination > 0.01f)
                        {
                            this.solidProgram.setUniform("model_view",
                                Matrix4.translate(arrow2PositionL)
                                    .times(Matrix4.scale(perspectiveWidgetScale, perspectiveWidgetScale, 1.0f))
                                    .times(Matrix4.fromColumns(
                                        arrow2LDirectionX.negated(),
                                        arrow2LDirectionY.negated(),
                                        new Vector4(0, 0, 1, 0),
                                        new Vector4(0, 0, 0, 1))));
                            this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
                        }
                    }

                    if (lightingModel.getLightWidgetModel(i).isDistanceWidgetVisible())
                    {
                        this.solidProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light." + i + ".Distance"));

                        this.solidProgram.setUniform("model_view",
                            Matrix4.translate(arrow3PositionL)
                                .times(Matrix4.scale(perspectiveWidgetScale, perspectiveWidgetScale, 1.0f))
                                .times(Matrix4.fromColumns(
                                    arrow3DirectionX.negated(),
                                    arrow3DirectionY.negated(),
                                    new Vector4(0, 0, 1, 0),
                                    new Vector4(0, 0, 0, 1))));

                        this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);

                        if (widgetTransformation.getColumn(3).getXYZ().distance(lightCenter) > 0.01f)
                        {
                            this.solidProgram.setUniform("model_view",
                                Matrix4.translate(arrow3PositionR)
                                    .times(Matrix4.scale(perspectiveWidgetScale, perspectiveWidgetScale, 1.0f))
                                    .times(Matrix4.fromColumns(
                                        arrow3DirectionX,
                                        arrow3DirectionY,
                                        new Vector4(0, 0, 1, 0),
                                        new Vector4(0, 0, 0, 1))));
                            this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
                        }
                    }
                }
            }

            context.getState().disableAlphaBlending();
            context.getState().enableDepthWrite();
            this.context.getState().enableDepthTest();
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

        if (this.environmentWeightsProgram != null)
        {
            this.environmentWeightsProgram.close();
            this.environmentWeightsProgram = null;
        }

        if (this.refScenePositions != null)
        {
            this.refScenePositions.close();
            this.refScenePositions = null;
        }

        if (this.refSceneTexCoords != null)
        {
            this.refSceneTexCoords.close();
            this.refSceneTexCoords = null;
        }

        if (this.refSceneNormals != null)
        {
            this.refSceneNormals.close();
            this.refSceneNormals = null;
        }

        if (this.refSceneTexture != null)
        {
            this.refSceneTexture.close();
            this.refSceneTexture = null;
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

        if (lightProgram != null)
        {
            lightProgram.close();
            lightProgram = null;
        }

        if (rectangleVertices != null)
        {
            rectangleVertices.close();
            rectangleVertices = null;
        }

        if (lightTexture != null)
        {
            lightTexture.close();
            lightTexture = null;
        }

        if (lightCenterTexture != null)
        {
            lightCenterTexture.close();
            lightCenterTexture = null;
        }

        if (solidProgram != null)
        {
            solidProgram.close();
            solidProgram = null;
        }

        if (widgetVertices != null)
        {
            widgetVertices.close();
            widgetVertices = null;
        }

        if (circleProgram != null)
        {
            circleProgram.close();
            circleProgram = null;
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

        if (gridVertices != null)
        {
            gridVertices.close();
            gridVertices = null;
        }

        if (tintedTexProgram != null)
        {
            tintedTexProgram.close();
            tintedTexProgram = null;
        }

        if (reprojectProgram != null)
        {
            reprojectProgram.close();
            reprojectProgram = null;
        }

        if (environmentWeightsTexture != null)
        {
            environmentWeightsTexture.close();
            environmentWeightsTexture = null;
        }

        if (environmentWeightsFBO != null)
        {
            environmentWeightsFBO.close();
            environmentWeightsFBO = null;
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

    @Override
    public SafeReadonlySettingsModel getSettingsModel()
    {
        return this.settingsModel;
    }

    @Override
    public void setSettingsModel(ReadonlySettingsModel settingsModel)
    {
        this.settingsModel = SafeSettingsModelWrapperFactory.getInstance().wrapUnsafeModel(settingsModel);
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
                ? "..." + this.id.substring(this.id.length()-31, this.id.length())
                : this.id;
    }

    @Override
    public void reloadShaders()
    {
        try
        {
            reloadMainProgram();

            Program<ContextType> newReferenceSceneProgram =
                loadMainProgram(getReferenceScenePreprocessorDefines(),
                    refSceneTexture != null && refSceneTexCoords != null ? RenderingMode.LAMBERTIAN_DIFFUSE_TEXTURED : RenderingMode.SPECULAR_SHADED);

            if (this.referenceSceneProgram != null)
            {
                this.referenceSceneProgram.close();
                this.referenceSceneProgram = null;
            }

            this.referenceSceneProgram = newReferenceSceneProgram;

            Program<ContextType> newReprojectProgram = resources.getIBRShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "reproject.frag"))
                .createProgram();

            if (this.reprojectProgram != null)
            {
                this.reprojectProgram.close();
                this.reprojectProgram = null;
            }

            this.reprojectProgram = newReprojectProgram;
            this.reprojectDrawable = context.createDrawable(reprojectProgram);
            this.reprojectDrawable.addVertexBuffer("position", this.resources.positionBuffer);

            if (this.resources.normalBuffer != null)
            {
                this.reprojectDrawable.addVertexBuffer("normal", this.resources.normalBuffer);
            }

            if (this.resources.texCoordBuffer != null)
            {
                this.reprojectDrawable.addVertexBuffer("texCoord", this.resources.texCoordBuffer);
            }

            if (this.resources.tangentBuffer != null)
            {
                this.reprojectDrawable.addVertexBuffer("tangent", this.resources.tangentBuffer);
            }

            Program<ContextType> newEnvironmentBackgroundProgram = resources.getIBRShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "envbackgroundtexture.frag"))
                    .createProgram();

            if (this.environmentBackgroundProgram != null)
            {
                this.environmentBackgroundProgram.close();
                this.environmentBackgroundProgram = null;
            }

            this.environmentBackgroundProgram = newEnvironmentBackgroundProgram;
            this.environmentBackgroundDrawable = context.createDrawable(environmentBackgroundProgram);
            this.environmentBackgroundDrawable.addVertexBuffer("position", rectangleVertices);


            Program<ContextType> newLightProgram = resources.getIBRShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "light.frag"))
                    .createProgram();

            if (this.lightProgram != null)
            {
                this.lightProgram.close();
                this.lightProgram = null;
            }

            this.lightProgram = newLightProgram;
            this.lightDrawable = context.createDrawable(this.lightProgram);
            this.lightDrawable.addVertexBuffer("position", rectangleVertices);

            Program<ContextType> newWidgetProgram = resources.getIBRShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "solid.frag"))
                    .createProgram();

            if (this.solidProgram != null)
            {
                this.solidProgram.close();
                this.solidProgram = null;
            }

            this.solidProgram = newWidgetProgram;

            this.widgetDrawable = context.createDrawable(this.solidProgram);
            this.widgetDrawable.addVertexBuffer("position", widgetVertices);

            this.gridDrawable = context.createDrawable(this.solidProgram);
            this.gridDrawable.addVertexBuffer("position", gridVertices);

            Program<ContextType> newCircleProgram = resources.getIBRShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "circle.frag"))
                .createProgram();

            if (this.circleProgram != null)
            {
                this.circleProgram.close();
                this.circleProgram = null;
            }

            this.circleProgram = newCircleProgram;

            this.circleDrawable = context.createDrawable(this.circleProgram);
            this.circleDrawable.addVertexBuffer("position", rectangleVertices);
        }
        catch (FileNotFoundException|RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    private void reloadMainProgram(Map<String, Optional<Object>> defineMap, RenderingMode renderingMode) throws FileNotFoundException
    {
        Program<ContextType> newProgram = loadMainProgram(defineMap, renderingMode);

        Program<ContextType> newEnvironmentWeightsProgram = null;

        if (this.resources.eigentextures != null)
        {
            newEnvironmentWeightsProgram = getProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "environmentweights.frag"))
                .createProgram();
        }

        if (this.program != null)
        {
            this.program.close();
        }

        this.program = newProgram;

        this.lastCompiledRenderingMode = renderingMode;

        this.mainDrawable = context.createDrawable(program);
        this.mainDrawable.addVertexBuffer("position", this.resources.positionBuffer);

        if (this.environmentWeightsProgram != null)
        {
            this.environmentWeightsProgram.close();
        }

        if (newEnvironmentWeightsProgram != null)
        {
            this.environmentWeightsProgram = newEnvironmentWeightsProgram;

            this.environmentWeightsDrawable = context.createDrawable(environmentWeightsProgram);
            this.environmentWeightsDrawable.addVertexBuffer("position", this.rectangleVertices);
        }

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

        suppressErrors = false;
    }

    private void reloadMainProgram() throws FileNotFoundException
    {
        reloadMainProgram(getPreprocessorDefines(), this.settingsModel == null ?
            RenderingMode.IMAGE_BASED : this.settingsModel.get("renderingMode", RenderingMode.class));
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

        defineMap.put("RAY_DEPTH_GRADIENT", Optional.of(0.1 * this.getScale()));
        defineMap.put("RAY_POSITION_JITTER", Optional.of(0.01 * this.getScale()));

        if (this.settingsModel != null)
        {
            defineMap.put("PHYSICALLY_BASED_MASKING_SHADOWING",
                Optional.of(this.settingsModel.getBoolean("pbrGeometricAttenuationEnabled")));
            defineMap.put("FRESNEL_EFFECT_ENABLED", Optional.of(this.settingsModel.getBoolean("fresnelEnabled")));
            defineMap.put("SHADOWS_ENABLED", Optional.of(this.settingsModel.getBoolean("shadowsEnabled")));

            defineMap.put("BUEHLER_ALGORITHM", Optional.of(this.settingsModel.getBoolean("buehlerAlgorithm")));
            defineMap.put("SORTING_SAMPLE_COUNT", Optional.of(this.settingsModel.getInt("buehlerViewCount")));
            defineMap.put("RELIGHTING_ENABLED", Optional.of(this.settingsModel.getBoolean("relightingEnabled")
                && !settingsModel.getBoolean("lightCalibrationMode") && this.lightingModel != null));

            boolean occlusionEnabled = this.settingsModel.getBoolean("occlusionEnabled")
                && (this.settingsModel.getBoolean("relightingEnabled")
                    || settingsModel.getBoolean("lightCalibrationMode")
                    || this.settingsModel.get("weightMode", ShadingParameterMode.class) != ShadingParameterMode.UNIFORM);

            defineMap.put("VISIBILITY_TEST_ENABLED", Optional.of(occlusionEnabled && this.resources.depthTextures != null));
            defineMap.put("SHADOW_TEST_ENABLED", Optional.of(occlusionEnabled && this.resources.shadowTextures != null
                && !settingsModel.getBoolean("lightCalibrationMode")));

            defineMap.put("PRECOMPUTED_VIEW_WEIGHTS_ENABLED",
                Optional.of(!this.settingsModel.getBoolean("relightingEnabled") && !settingsModel.getBoolean("lightCalibrationMode")
                    && this.settingsModel.get("weightMode", ShadingParameterMode.class) == ShadingParameterMode.UNIFORM));

            if (settingsModel.getBoolean("lightCalibrationMode"))
            {
                defineMap.put("USE_VIEW_INDICES", Optional.of(true));
                defineMap.put("VIEW_COUNT", Optional.of(1));
            }

            if (this.lightingModel != null && this.settingsModel.getBoolean("relightingEnabled"))
            {
                defineMap.put("VIRTUAL_LIGHT_COUNT", Optional.of(lightingModel.getLightCount()));
                defineMap.put("ENVIRONMENT_ILLUMINATION_ENABLED", Optional.of(!Objects.equals(lightingModel.getAmbientLightColor(), Vector3.ZERO)));
                defineMap.put("ENVIRONMENT_TEXTURE_ENABLED", Optional.of(this.environmentMap != null && lightingModel.isEnvironmentMappingEnabled()));
            }
        }

        return defineMap;
    }

    private Map<String, Optional<Object>> getReferenceScenePreprocessorDefines()
    {
        return getPreprocessorDefines();
    }

    private ProgramBuilder<ContextType> getProgramBuilder(Map<String, Optional<Object>> defineMap, RenderingMode renderingMode)
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

    private ProgramBuilder<ContextType> getProgramBuilder()
    {
        return getProgramBuilder(getPreprocessorDefines(), this.settingsModel.get("renderingMode", RenderingMode.class));
    }

    private Program<ContextType> loadMainProgram(Map<String, Optional<Object>> defineMap, RenderingMode renderingMode) throws FileNotFoundException
    {
        return this.getProgramBuilder(defineMap, renderingMode)
                .define("SPOTLIGHTS_ENABLED", true)
                .addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/relight/relight.frag"))
                .createProgram();
    }

    private Program<ContextType> loadMainProgram() throws FileNotFoundException
    {
        return loadMainProgram(getPreprocessorDefines(), this.settingsModel.get("renderingMode", RenderingMode.class));
    }

    private void updateCompiledSettings()
    {
        Map<String, Optional<Object>> defineMap = getPreprocessorDefines();

        RenderingMode renderingMode =
            this.settingsModel == null ? RenderingMode.IMAGE_BASED : this.settingsModel.get("renderingMode", RenderingMode.class);

        if (renderingMode != lastCompiledRenderingMode ||
            defineMap.entrySet().stream().anyMatch(
                defineEntry -> !Objects.equals(this.program.getDefine(defineEntry.getKey()), defineEntry.getValue())
                    || (this.environmentWeightsProgram != null
                        && !Objects.equals(this.environmentWeightsProgram.getDefine(defineEntry.getKey()), defineEntry.getValue()))))
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

        //noinspection VariableNotUsedInsideIf
        if (this.referenceScene != null)
        {
            Map<String, Optional<Object>> refSceneDefineMap = getReferenceScenePreprocessorDefines();

            if (this.referenceSceneProgram == null
                || !Objects.equals(this.program.getDefine("DIFFUSE_TEXTURE_ENABLED").orElse(false),
                    refSceneTexture != null && refSceneTexCoords != null)
                || refSceneDefineMap.entrySet().stream().anyMatch(
                    defineEntry -> !Objects.equals(this.program.getDefine(defineEntry.getKey()), defineEntry.getValue())))
            {
                try
                {
                    System.out.println("Updating compiled render settings for reference scene.");

                    Program<ContextType> newReferenceSceneProgram =
                        loadMainProgram(refSceneDefineMap, refSceneTexture != null && refSceneTexCoords != null ?
                            RenderingMode.LAMBERTIAN_DIFFUSE_TEXTURED : RenderingMode.SPECULAR_SHADED);

                    if (this.referenceSceneProgram != null)
                    {
                        this.referenceSceneProgram.close();
                        this.referenceSceneProgram = null;
                    }

                    this.referenceSceneProgram = newReferenceSceneProgram;
                }
                catch (RuntimeException|FileNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void setMultiTransformationModel(List<Matrix4> multiTransformationModel)
    {
        if (multiTransformationModel != null)
        {
            this.multiTransformationModel = multiTransformationModel;
        }
    }

    @Override
    public void setReferenceScene(VertexGeometry scene)
    {
        this.referenceScene = scene;
        this.referenceSceneChanged = true;
    }

    @Override
    public SceneViewport getSceneViewportModel()
    {
        return new SceneViewport()
        {
            @Override
            public Object getObjectAtCoordinates(double x, double y)
            {
                if (pixelObjectIDBuffer != null)
                {
                    double xRemapped = Math.min(Math.max(x, 0), 1);
                    double yRemapped = 1.0 - Math.min(Math.max(y, 0), 1);

                    int index = 4 * (int)(Math.round((fboSize.height-1) * yRemapped) * fboSize.width + Math.round((fboSize.width-1) * xRemapped));
                    return sceneObjectNameList[pixelObjectIDBuffer.get(index)];
                }
                else
                {
                    return null;
                }
            }


            private Matrix4 getProjectionInverse()
            {
                Matrix4 projection = getProjectionMatrix(fboSize);
                return  Matrix4.fromRows(
                    new Vector4(1.0f / projection.get(0, 0), 0, 0, 0),
                    new Vector4(0, 1.0f / projection.get(1, 1), 0, 0),
                    new Vector4(0, 0, 0, -1),
                    new Vector4(0, 0, 1.0f, projection.get(2, 2))
                        .dividedBy(projection.get(2, 3)));
            }

            @Override
            public Vector3 get3DPositionAtCoordinates(double x, double y)
            {
                if (pixelDepthBuffer != null)
                {
                    double xRemapped = Math.min(Math.max(x, 0), 1);
                    double yRemapped = 1.0 - Math.min(Math.max(y, 0), 1);

                    int index = (int)(Math.round((fboSize.height-1) * yRemapped) * fboSize.width + Math.round((fboSize.width-1) * xRemapped));

                    Matrix4 projectionInverse = getProjectionInverse();

                    // Transform from screen space into camera space
                    Vector4 unscaledPosition = projectionInverse
                        .times(new Vector4((float)(2 * x - 1), (float)(1 - 2 * y), 2 * (float)(0x0000FFFF & pixelDepthBuffer.get(index)) / (float)0xFFFF - 1, 1.0f));

                    // Transform from camera space into world space.
                    return getPartialViewMatrix().quickInverse(0.01f)
                            .times(unscaledPosition.getXYZ().dividedBy(unscaledPosition.w).asPosition())
                            .getXYZ().dividedBy(getScale());
                }
                else
                {
                    return null;
                }
            }

            @Override
            public Vector3 getViewingDirection(double x, double y)
            {
                Matrix4 projectionInverse = getProjectionInverse();

                // Take the position the pixel would have at the far clipping plane.
                // Transform from screen space into world space.
                Vector4 unscaledPosition = projectionInverse
                    .times(new Vector4((float)(2 * x - 1), (float)(1 - 2 * y), 1.0f, 1.0f));

                // Transform from camera space into world space.
                // Interpret the vector as the direction from the origin (0,0,0) for this pixel.
                return getPartialViewMatrix().quickInverse(0.01f)
                    .times(unscaledPosition.getXYZ().dividedBy(unscaledPosition.w).asDirection())
                    .getXYZ().normalized();
            }

            @Override
            public Vector3 getViewportCenter()
            {
                return getPartialViewMatrix().quickInverse(0.01f)
                    .getColumn(3)
                    .getXYZ().dividedBy(getScale());
            }

            @Override
            public Vector2 projectPoint(Vector3 point)
            {
                Vector4 projectedPoint = getProjectionMatrix(fboSize)
                    .times(getPartialViewMatrix())
                    .times(point.times(getScale()).asPosition());

                return new Vector2(0.5f + projectedPoint.x / (2 * projectedPoint.w), 0.5f - projectedPoint.y / (2 * projectedPoint.w));
            }

            @Override
            public float getLightWidgetScale()
            {
                return computeLightWidgetScale(getPartialViewMatrix(), fboSize) / getScale();
            }
        };
    }

    @Override
    public ReadonlyObjectModel getObjectModel()
    {
        return objectModel;
    }

    @Override
    public ReadonlyCameraModel getCameraModel()
    {
        return cameraModel;
    }

    @Override
    public ReadonlyLightingModel getLightingModel()
    {
        return lightingModel;
    }

    @Override
    public void setObjectModel(ReadonlyObjectModel objectModel)
    {
        this.objectModel = objectModel;
    }

    @Override
    public void setCameraModel(ReadonlyCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
    }

    @Override
    public void setLightingModel(ReadonlyLightingModel lightingModel)
    {
        this.lightingModel = lightingModel;
    }
}
