package tetzlaff.ibrelight.rendering;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;

import tetzlaff.gl.*;
import tetzlaff.gl.AlphaBlendingFunction.Weight;
import tetzlaff.gl.ColorFormat.DataType;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthAttachmentSpec;
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
import tetzlaff.models.*;
import tetzlaff.models.impl.DefaultSettingsModel;
import tetzlaff.models.impl.SafeSettingsModelWrapperFactory;
import tetzlaff.util.EnvironmentMap;
import tetzlaff.util.ShadingParameterMode;

public class IBRImplementation<ContextType extends Context<ContextType>> implements IBRRenderable<ContextType>
{
    private final ContextType context;
    private Program<ContextType> program;
    private Program<ContextType> shadowProgram;
    private LoadingMonitor loadingMonitor;
    private boolean suppressErrors = false;
    private SafeReadonlySettingsModel settingsModel;

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

    private boolean newEnvironmentDataAvailable;
    private EnvironmentMap newEnvironmentData;
    private boolean environmentMapUnloadRequested = false;
    private Cubemap<ContextType> environmentMap;
    private File currentEnvironmentFile;
    private volatile File desiredEnvironmentFile;
    private final Object loadEnvironmentLock = new Object();

    private boolean newBackplateDataAvailable;
    private BufferedImage newBackplateData;
    private boolean backplateUnloadRequested = false;
    private Texture2D<ContextType> backplateTexture;
    private File currentBackplateFile;
    private File desiredBackplateFile;
    private final Object loadBackplateLock = new Object();

    private Program<ContextType> environmentBackgroundProgram;
    private Drawable<ContextType> environmentBackgroundDrawable;
    
    private UniformBuffer<ContextType> weightBuffer;
    
    private List<Matrix4> multiTransformationModel;
    private Vector3 centroid;
    private float boundingRadius;
    
    private VertexGeometry referenceScene;
    private boolean referenceSceneChanged = false;
    private VertexBuffer<ContextType> refScenePositions;
    private VertexBuffer<ContextType> refSceneTexCoords;
    private VertexBuffer<ContextType> refSceneNormals;
    private Texture2D<ContextType> refSceneTexture;
    
    private final List<String> sceneObjectNameList;
    private final Map<String, Integer> sceneObjectIDLookup;
    private int[] pixelObjectIDs;
    private short[] pixelDepths;
    private FramebufferSize fboSize;

    private Program<ContextType> circleProgram;
    private Drawable<ContextType> circleDrawable;

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

        this.sceneObjectNameList = new ArrayList<>(64);
        this.sceneObjectIDLookup = new HashMap<>(64);

        this.sceneObjectNameList.add(null);

        int k = 1;

        this.sceneObjectNameList.add("IBRObject");
        this.sceneObjectIDLookup.put("IBRObject", k);
        k++;

        this.sceneObjectNameList.add("EnvironmentMap");
        this.sceneObjectIDLookup.put("EnvironmentMap", k);
        k++;

        this.sceneObjectNameList.add("SceneObject");
        this.sceneObjectIDLookup.put("SceneObject", k);
        k++;

        for (int i = 0; i < 4; i++)
        {
            this.sceneObjectNameList.add("Light." + i);
            this.sceneObjectIDLookup.put("Light." + i, k);
            k++;

            this.sceneObjectNameList.add("Light." + i + ".Center");
            this.sceneObjectIDLookup.put("Light." + i + ".Center", k);
            k++;

            this.sceneObjectNameList.add("Light." + i + ".Azimuth");
            this.sceneObjectIDLookup.put("Light." + i + ".Azimuth", k);
            k++;

            this.sceneObjectNameList.add("Light." + i + ".Inclination");
            this.sceneObjectIDLookup.put("Light." + i + ".Inclination", k);
            k++;

            this.sceneObjectNameList.add("Light." + i + ".Distance");
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
    public void initialize()
    {
        if (this.program == null)
        {
            try
            {
                this.program = context.getShaderProgramBuilder()
                        .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                        .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "relight.frag"))
                        .createProgram();
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }

        try
        {
            this.simpleTexProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "texture.frag"))
                    .createProgram();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        try
        {
            this.environmentBackgroundProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "envbackgroundtexture.frag"))
                    .createProgram();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        try
        {
            this.resources = resourceBuilder.create();

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
            this.simpleTexDrawable.addVertexBuffer("position", context.createRectangle());

            this.environmentBackgroundDrawable = context.createDrawable(environmentBackgroundProgram);
            this.environmentBackgroundDrawable.addVertexBuffer("position", context.createRectangle());

            context.flush();

            if (this.loadingMonitor != null)
            {
                this.loadingMonitor.setMaximum(0.0); // make indeterminate
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        try
        {
            shadowProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "depth.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "depth.frag"))
                    .createProgram();

            shadowDrawable = context.createDrawable(shadowProgram);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            throw new IllegalStateException("The shader program could not be initialized.", e);
        }

        try
        {
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
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        this.rectangleVertices = context.createRectangle();

        try
        {
            this.lightProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "light.frag"))
                    .createProgram();
            this.lightDrawable = context.createDrawable(this.lightProgram);
            this.lightDrawable.addVertexBuffer("position", rectangleVertices);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        try
        {
            this.circleProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "circle.frag"))
                .createProgram();
            this.circleDrawable = context.createDrawable(this.circleProgram);
            this.circleDrawable.addVertexBuffer("position", rectangleVertices);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

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

        this.lightTexture = context.build2DColorTextureFromBuffer(64, 64, lightTextureData)
                .setInternalFormat(ColorFormat.R8)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(true)
                .createTexture();

        this.lightCenterTexture = context.build2DColorTextureFromBuffer(64, 64, lightCenterTextureData)
                .setInternalFormat(ColorFormat.R8)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(true)
                .createTexture();

        shadowDrawable.addVertexBuffer("position", resources.positionBuffer);

        shadowMaps = context.build2DDepthTextureArray(2048, 2048, lightingModel.getLightCount()).createTexture();
        shadowFramebuffer = context.buildFramebufferObject(2048, 2048)
            .addDepthAttachment()
            .createFramebufferObject();

        this.updateCentroidAndRadius();

        // Make sure that everything is loaded onto the graphics card before announcing that loading is complete.
        this.draw(context.getDefaultFramebuffer());

        if (this.loadingMonitor != null)
        {
            this.loadingMonitor.loadingComplete();
        }
    }

    @Override
    public void update()
    {
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

                        newEnvironmentTexture = context.buildColorCubemap(environmentData.getSide())
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

                        newBackplateTexture = context.build2DColorTextureFromImage(backplateData, true)
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
                this.refSceneTexture = context.build2DColorTextureFromFile(
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

    private void setupForDraw(Matrix4 view)
    {
        this.setupForDraw(this.mainDrawable.program(), view);
    }

    private void setupForDraw(Program<ContextType> program, Matrix4 view)
    {
        this.resources.setupShaderProgram(program, this.settingsModel.get("renderingMode", RenderingMode.class));

        if (!this.settingsModel.getBoolean("relightingEnabled")
            && this.settingsModel.get("weightMode", ShadingParameterMode.class) == ShadingParameterMode.UNIFORM)
        {
            program.setUniform("perPixelWeightsEnabled", false);
            if (weightBuffer != null)
            {
                weightBuffer.close();
                weightBuffer = null;
            }
            weightBuffer = context.createUniformBuffer().setData(this.generateViewWeights(view));
            program.setUniformBuffer("ViewWeights", weightBuffer);
            program.setUniform("occlusionEnabled", false);
            program.setUniform("shadowTestEnabled", false);
        }
        else
        {
            program.setUniform("perPixelWeightsEnabled", true);
            program.setUniform("weightExponent", this.settingsModel.getFloat("weightExponent"));
            program.setUniform("isotropyFactor", this.settingsModel.getFloat("isotropyFactor"));
            program.setUniform("occlusionEnabled",
                this.resources.depthTextures != null && this.settingsModel.getBoolean("occlusionEnabled"));
            program.setUniform("shadowTestEnabled",
                this.resources.shadowTextures != null && this.settingsModel.getBoolean("occlusionEnabled"));
            program.setUniform("occlusionBias", this.settingsModel.getFloat("occlusionBias"));
        }

        float gamma = this.settingsModel.getFloat("gamma");
        program.setUniform("renderGamma", gamma);

        program.setUniform("imageBasedRenderingEnabled", this.settingsModel.get("renderingMode", RenderingMode.class).isImageBased());
        program.setUniform("relightingEnabled", this.settingsModel.getBoolean("relightingEnabled"));
        program.setUniform("pbrGeometricAttenuationEnabled", this.settingsModel.getBoolean("pbrGeometricAttenuationEnabled"));
        program.setUniform("fresnelEnabled", this.settingsModel.getBoolean("fresnelEnabled"));
        program.setUniform("shadowsEnabled", this.settingsModel.getBoolean("shadowsEnabled"));

        program.setTexture("shadowMaps", shadowMaps);

        if (this.environmentMap == null || !lightingModel.isEnvironmentMappingEnabled())
        {
            program.setUniform("useEnvironmentMap", false);
            program.setTexture("environmentMap", null);
        }
        else
        {
            program.setUniform("useEnvironmentMap", true);
            program.setTexture("environmentMap", this.environmentMap);
            program.setUniform("environmentMipMapLevel", Math.max(0, Math.min(this.environmentMap.getMipmapLevelCount() - 2,
                    (int)Math.ceil(0.5 *
                        Math.log(6 * (double)this.environmentMap.getFaceSize() *
                                (double)this.environmentMap.getFaceSize() / (double)resources.viewSet.getCameraPoseCount() )
                            / Math.log(2.0)))));
            program.setUniform("diffuseEnvironmentMipMapLevel", this.environmentMap.getMipmapLevelCount() - 1);
        }

        program.setUniform("virtualLightCount", Math.min(4, lightingModel.getLightCount()));

        program.setUniform("ambientColor", lightingModel.getAmbientLightColor());

        this.clearColor = new Vector3(
                (float)Math.pow(lightingModel.getBackgroundColor().x, 1.0 / gamma),
                (float)Math.pow(lightingModel.getBackgroundColor().y, 1.0 / gamma),
                (float)Math.pow(lightingModel.getBackgroundColor().z, 1.0 / gamma));
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

    private float getScale()
    {
         return resources.viewSet.getCameraPose(0)
                 .times(resources.geometry.getCentroid().asPosition())
             .getXYZ().length()
             * this.boundingRadius / this.resources.geometry.getBoundingRadius();
    }

    private Matrix4 getLightMatrix(int lightIndex)
    {
        float scale = getScale();
        return Matrix4.scale(scale)
            .times(lightingModel.getLightMatrix(lightIndex))
            .times(objectModel.getTransformationMatrix())
            .times(Matrix4.scale(1.0f / scale))
            .times(resources.viewSet.getCameraPose(0).getUpperLeft3x3().asMatrix4())
            .times(Matrix4.translate(this.centroid.negated()));
    }

    private Matrix4 getEnvironmentMapMatrix()
    {
        float scale = getScale();
        return Matrix4.scale(scale)
            .times(lightingModel.getEnvironmentMapMatrix())
            .times(objectModel.getTransformationMatrix())
            .times(Matrix4.scale(1.0f / scale))
            .times(resources.viewSet.getCameraPose(0).getUpperLeft3x3().asMatrix4())
            .times(Matrix4.translate(this.centroid.negated()));
    }

    private Matrix4 getLightProjection(int lightIndex)
    {
        Matrix4 lightMatrix = getLightMatrix(lightIndex);

        float lightDist = lightMatrix.times(this.centroid.asPosition()).getXYZ().length();

        float radius = (float)
            (resources.viewSet.getCameraPose(0).getUpperLeft3x3()
                .times(new Vector3(this.boundingRadius))
                .length() / Math.sqrt(3));

        return Matrix4.perspective(2.0f * (float)Math.atan(radius / lightDist) /*1.5f*/, 1.0f,
                lightDist - radius,
                lightDist + radius);
    }

    private void generateShadowMaps(int lightIndex)
    {
        Matrix4 lightProj = getLightProjection(lightIndex);

        shadowProgram.setUniform("projection", lightProj);


        FramebufferAttachment<ContextType> attachment =shadowMaps.getLayerAsFramebufferAttachment(lightIndex);

        shadowFramebuffer.setDepthAttachment(attachment);
        shadowFramebuffer.clearDepthBuffer();

        for (Matrix4 m : this.multiTransformationModel)
        {
            shadowProgram.setUniform("model_view", getLightMatrix(lightIndex).times(m));
            shadowDrawable.draw(PrimitiveMode.TRIANGLES, shadowFramebuffer);
        }
    }

    private void setupLight(int lightIndex, int modelInstance)
    {
        Matrix4 lightMatrix = this.multiTransformationModel.get(modelInstance).quickInverse(0.01f).times(getLightMatrix(lightIndex));

        // lightMatrix can be hardcoded here (comment out previous line)

            // Contemporary gallery and stonewall
            //Matrix4.rotateY(16 * Math.PI / 16).times(Matrix4.rotateX(0 * Math.PI / 16))

            // Color studio 2:
            //Matrix4.rotateY(6 * Math.PI / 16).times(Matrix4.rotateX(0 * Math.PI / 16))

            // For the synthetic falcon example?
            //Matrix4.rotateY(5 * Math.PI / 4).times(Matrix4.rotateX(-Math.PI / 4))

            // Always end with this when hardcoding:
            //    .times(new Matrix4(new Matrix3(resources.viewSet.getCameraPose(0))));

        Vector3 lightPos = lightMatrix.quickInverse(0.001f).times(Vector4.ORIGIN).getXYZ();

        program.setUniform("lightPosVirtual[" + lightIndex + ']', lightPos);

        Vector3 controllerLightIntensity = lightingModel.getLightColor(lightIndex);
        float lightDistance = getLightMatrix(lightIndex).times(this.centroid.asPosition()).getXYZ().length();

        float lightScale = resources.viewSet.areLightSourcesInfinite() ? 1.0f :
                resources.viewSet.getCameraPose(0)
                        .times(resources.geometry.getCentroid().asPosition())
                    .getXYZ().length();

        program.setUniform("lightIntensityVirtual[" + lightIndex + ']',
                controllerLightIntensity.times(lightDistance * lightDistance * resources.viewSet.getLightIntensity(0).y / (lightScale * lightScale)));
        program.setUniform("lightMatrixVirtual[" + lightIndex + ']', getLightProjection(lightIndex).times(lightMatrix));
    }

    private Matrix4 getPartialViewMatrix()
    {
        float scale = getScale();

        return Matrix4.scale(scale)
                .times(cameraModel.getLookMatrix())
                .times(Matrix4.scale(1.0f / scale));
    }

    private Matrix4 getPartialViewMatrix(Matrix4 viewMatrix)
    {
        return viewMatrix
                .times(Matrix4.translate(this.centroid))
                .times(resources.viewSet.getCameraPose(0).getUpperLeft3x3().transpose().asMatrix4());
    }

    private Matrix4 getViewMatrix()
    {
        return getPartialViewMatrix()
                .times(resources.viewSet.getCameraPose(0).getUpperLeft3x3().asMatrix4())
                .times(Matrix4.translate(this.centroid.negated()));
    }

    private Matrix4 getModelViewMatrix(Matrix4 partialViewMatrix, int modelInstance)
    {
        float scale = getScale();

        return partialViewMatrix
                .times(Matrix4.scale(scale))
                .times(this.objectModel.getTransformationMatrix())
                .times(Matrix4.scale(1.0f / scale))
                .times(resources.viewSet.getCameraPose(0).getUpperLeft3x3().asMatrix4())
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

    private void drawReferenceScene(Framebuffer<ContextType> framebuffer, Matrix4 view)
    {
        if (referenceScene != null && refScenePositions != null && refSceneNormals != null)
        {
            Drawable<ContextType> drawable = context.createDrawable(program);
            drawable.addVertexBuffer("position", refScenePositions);
            drawable.addVertexBuffer("normal", refSceneNormals);

            if (refSceneTexture != null && refSceneTexCoords != null)
            {
                drawable.addVertexBuffer("texCoord", refSceneTexCoords);
                program.setTexture("diffuseMap", refSceneTexture);
                program.setUniform("useDiffuseTexture", true);
            }
            else
            {
                program.setUniform("useDiffuseTexture", false);
            }
            program.setUniform("model_view", view);
            program.setUniform("viewPos", view.quickInverse(0.01f).getColumn(3).getXYZ());

            // Do first pass at half resolution to off-screen buffer
            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
        }
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
        return cameraDistance * getVerticalFieldOfView(size) / 4;
    }

    @Override
    public void draw(Framebuffer<ContextType> framebuffer, Matrix4 viewParam, Matrix4 projectionParam)
    {
        boolean customViewMatrix = viewParam != null;

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

            Matrix4 view = viewParam;

            Matrix4 partialViewMatrix;

            if (customViewMatrix)
            {
                partialViewMatrix = getPartialViewMatrix(view);

                if (lightingModel instanceof CameraBasedLightingModel)
                {
                    float scale = resources.viewSet.getCameraPose(0)
                        .times(resources.geometry.getCentroid().asPosition())
                        .getXYZ().length();

                    ((CameraBasedLightingModel) lightingModel).overrideCameraPose(
                        Matrix4.scale(1.0f / scale)
                            .times(view)
                            .times(Matrix4.translate(resources.geometry.getCentroid()))
                            .times(resources.viewSet.getCameraPose(0).transpose().getUpperLeft3x3().asMatrix4())
                            .times(Matrix4.scale(scale)));
                }
            }
            else
            {
                view = this.getViewMatrix();
                partialViewMatrix = getPartialViewMatrix();
            }

            FramebufferSize size = framebuffer.getSize();

            Matrix4 projection = projectionParam;

            if (projection == null)
            {
                projection = this.getProjectionMatrix(size);
            }

            this.setupForDraw(view);

            mainDrawable.program().setUniform("projection", projection);

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
                        .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.R32I))
                        .addDepthAttachment(DepthAttachmentSpec.createFixedPointWithPrecision(32))
                        .createFramebufferObject()
            )
            {
                offscreenFBO.clearIntegerColorBuffer(1, 0, 0, 0, 0);
                offscreenFBO.clearDepthBuffer();

                if (backplateTexture != null && lightingModel.getBackgroundMode() == BackgroundMode.IMAGE)
                {
                    // Second pass at full resolution to default framebuffer
                    simpleTexDrawable.program().setTexture("tex", backplateTexture);

                    context.getState().disableDepthTest();
                    simpleTexDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
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
                        .times(resources.viewSet.getCameraPose(0).getUpperLeft3x3().asMatrix4())
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
                    shadowMaps = context.build2DDepthTextureArray(2048, 2048, lightingModel.getLightCount()).createTexture();
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

                this.program.setUniform("imageBasedRenderingEnabled", false);
                this.program.setUniform("objectID", this.sceneObjectIDLookup.get("SceneObject"));
                this.drawReferenceScene(offscreenFBO, view);

                this.program.setUniform("imageBasedRenderingEnabled", this.settingsModel.get("renderingMode", RenderingMode.class).isImageBased());
                setupForDraw(view); // in case anything changed when drawing the reference scene.
                this.program.setUniform("objectID", this.sceneObjectIDLookup.get("IBRObject"));

                Matrix4 envMapMatrix = this.getEnvironmentMapMatrix();
                this.program.setUniform("envMapMatrix", envMapMatrix);

                for (int modelInstance = 0; modelInstance < multiTransformationModel.size(); modelInstance++)
                {
                    for (int lightIndex = 0; lightIndex < lightingModel.getLightCount(); lightIndex++)
                    {
                        setupLight(lightIndex, modelInstance);
                    }

                    // Draw instance
                    Matrix4 modelView = getModelViewMatrix(partialViewMatrix, modelInstance);
                    this.program.setUniform("model_view", modelView);
                    this.program.setUniform("viewPos", modelView.quickInverse(0.01f).getColumn(3).getXYZ());

                    // Render to off-screen buffer
                    mainDrawable.draw(PrimitiveMode.TRIANGLES, offscreenFBO);
                }

                if (!lightingModel.areLightWidgetsEthereal())
                {
                    context.flush();

                    // Read buffers here if light widgets are ethereal (i.e. they cannot be clicked and should not be in the ID buffer)
                    pixelObjectIDs = offscreenFBO.readIntegerColorBufferRGBA(1);
                    pixelDepths = offscreenFBO.readDepthBuffer();
                    fboSize = offscreenFBO.getSize();
                }

                drawLights(offscreenFBO, view, partialViewMatrix);

                // Finish drawing
                context.flush();

                // Second pass at full resolution to default framebuffer
                simpleTexDrawable.program().setTexture("tex", offscreenFBO.getColorAttachmentTexture(0));

                framebuffer.clearDepthBuffer();
                simpleTexDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);

                context.flush();

                if (!lightingModel.areLightWidgetsEthereal())
                {
                    // Read buffers here if light widgets are not ethereal (i.e. they can be clicked and should be in the ID buffer)
                    pixelObjectIDs = offscreenFBO.readIntegerColorBufferRGBA(1);
                    pixelDepths = offscreenFBO.readDepthBuffer();
                    fboSize = offscreenFBO.getSize();
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
            if (customViewMatrix && lightingModel instanceof CameraBasedLightingModel)
            {
                ((CameraBasedLightingModel)lightingModel).removeCameraPoseOverride();
            }
        }
    }

    private void drawLights(Framebuffer<ContextType> framebuffer, Matrix4 viewMatrix, Matrix4 partialViewMatrix)
    {
        FramebufferSize size = framebuffer.getSize();

        if (this.settingsModel.getBoolean("relightingEnabled") && this.settingsModel.getBoolean("visibleLightsEnabled"))
        {
            this.context.getState().disableDepthWrite();

            // Draw lights
            for (int i = 0; i < lightingModel.getLightCount(); i++)
            {
                this.context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
                this.context.getState().enableDepthTest();

                if (settingsModel.getBoolean("lightWidgetsEnabled") && lightingModel.getLightWidgetModel(i).areWidgetsEnabled()
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
                    .times(resources.viewSet.getCameraPose(0).getUpperLeft3x3().transpose().asMatrix4())
                    .times(Matrix4.scale(scale))
                    .times(lightingModel.getLightMatrix(i).quickInverse(0.01f))
                    .times(Matrix4.scale(1.0f / scale));

                if (lightingModel.isLightVisualizationEnabled(i))
                {
                    this.context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
                    this.lightProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light." + i));
                    this.lightProgram.setUniform("color", lightingModel.getLightColor(i));

                    Vector3 lightPosition = widgetTransformation.getColumn(3).getXYZ();

                    this.lightProgram.setUniform("model_view",
                        Matrix4.translate(lightPosition)
                            .times(Matrix4.scale(-lightPosition.z / 32.0f, -lightPosition.z / 32.0f, 1.0f)));
                    this.lightProgram.setUniform("projection", this.getProjectionMatrix(size));
                    this.lightProgram.setTexture("lightTexture", this.lightTexture);
                    this.lightDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
                }

                if (settingsModel.getBoolean("lightWidgetsEnabled") && lightingModel.getLightWidgetModel(i).areWidgetsEnabled())
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

                    float perspectiveWidgetScale = -widgetPosition.z * getVerticalFieldOfView(size) / 64;

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
                    this.circleProgram.setUniform("width", 1 / 64.0f);
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
                        8 * perspectiveWidgetScale / (widgetDistance * Math.sqrt(1 - cosineLightToPole * cosineLightToPole)));

                    double inclinationArrowRotation = Math.min(Math.PI / 4, 8 * perspectiveWidgetScale / widgetDistance);

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
                    context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));

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

                        this.solidProgram.setUniform("color",
                            new Vector3(lightingModel.getLightWidgetModel(i).isAzimuthWidgetSelected() ? 1.0f : 0.5f)
                                .asVector4(1));
                        this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);

                        this.solidProgram.setUniform("model_view",
                            Matrix4.translate(arrow1PositionL)
                                .times(Matrix4.scale(perspectiveWidgetScale, perspectiveWidgetScale, 1.0f))
                                .times(Matrix4.fromColumns(
                                    arrow1LDirectionX.negated(),
                                    arrow1LDirectionY.negated(),
                                    new Vector4(0, 0, 1, 0),
                                    new Vector4(0, 0, 0, 1))));

                        this.solidProgram.setUniform("color",
                            new Vector3(lightingModel.getLightWidgetModel(i).isAzimuthWidgetSelected() ? 1.0f : 0.5f)
                                .asVector4(1));
                        this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
                    }

                    if (lightingModel.getLightWidgetModel(i).isInclinationWidgetVisible())
                    {
                        this.solidProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light." + i + ".Inclination"));

                        this.solidProgram.setUniform("color",
                            new Vector3(lightingModel.getLightWidgetModel(i).isInclinationWidgetSelected() ? 1.0f : 0.5f)
                                .asVector4(1));

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

                        this.solidProgram.setUniform("color",
                            new Vector3(lightingModel.getLightWidgetModel(i).isDistanceWidgetSelected() ? 1.0f : 0.5f)
                                .asVector4(1));

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

    @Override
    public void setProgram(Program<ContextType> program)
    {
        this.program = program;

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

        suppressErrors = false;
    }

    @Override
    public void loadEnvironmentMap(File environmentFile) throws FileNotFoundException
    {
        if (environmentFile == null && this.environmentMap != null)
        {
            this.environmentMapUnloadRequested = true;
        }
        else if (environmentFile != null && environmentFile.exists())
        {
            System.out.println("Loading new environment texture.");

            this.desiredEnvironmentFile = environmentFile;
            boolean readCompleted = false;

            synchronized(loadEnvironmentLock)
            {
                if (Objects.equals(environmentFile, desiredEnvironmentFile) && !Objects.equals(environmentFile, currentEnvironmentFile))
                {
                    try
                    {
                        // Use Michael Ludwig's code to convert to a cube map (supports either cross or panorama input)
                        this.newEnvironmentData = EnvironmentMap.createFromHDRFile(environmentFile);
                        this.currentEnvironmentFile = environmentFile;
                        readCompleted = true;

//                        // Uncomment to save the panorama as an image (i.e. for a figure in a paper)
//                        float[] pixels = EnvironmentMap.toPanorama(envMap.getData(), envMap.getSide(), envMap.getSide() * 4, envMap.getSide() * 2);
//                        BufferedImage img = new BufferedImage(envMap.getSide() * 4, envMap.getSide() * 2, BufferedImage.TYPE_3BYTE_BGR);
//                        int k = 0;
//
//                        for (int j = 0; j < envMap.getSide() * 2; j++)
//                        {
//                            for (int i = 0; i < envMap.getSide() * 4; i++)
//                            {
//                                img.setRGB(i,  j, ((int)(Math.pow(pixels[3 * k + 0], 1.0 / 2.2) * 255) << 16)
//                                        | ((int)(Math.pow(pixels[3 * k + 1], 1.0 / 2.2) * 255) << 8)
//                                        | (int)(Math.pow(pixels[3 * k + 2], 1.0 / 2.2) * 255));
//                                k++;
//                            }
//                        }
//                        ImageIO.write(img, "PNG", new File(environmentFile.getParentFile(), environmentFile.getName().replace("_zvc.hdr", "_IBRelight_pan.hdr")));
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
            boolean readCompleted = false;

            synchronized(loadBackplateLock)
            {
                if (Objects.equals(backplateFile, desiredBackplateFile) && !Objects.equals(backplateFile, currentBackplateFile))
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
    public void reloadHelperShaders()
    {
        try
        {
            Program<ContextType> newEnvironmentBackgroundProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "envbackgroundtexture.frag"))
                    .createProgram();

            if (this.environmentBackgroundProgram != null)
            {
                this.environmentBackgroundProgram.close();
            }

            this.environmentBackgroundProgram = newEnvironmentBackgroundProgram;
            this.environmentBackgroundDrawable = context.createDrawable(environmentBackgroundProgram);
            this.environmentBackgroundDrawable.addVertexBuffer("position", context.createRectangle());


            Program<ContextType> newLightProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "light.frag"))
                    .createProgram();

            if (this.lightProgram != null)
            {
                this.lightProgram.close();
            }

            this.lightProgram = newLightProgram;
            this.lightDrawable = context.createDrawable(this.lightProgram);
            this.lightDrawable.addVertexBuffer("position", rectangleVertices);

            Program<ContextType> newWidgetProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "solid.frag"))
                    .createProgram();

            if (this.solidProgram != null)
            {
                this.solidProgram.close();
            }

            this.solidProgram = newWidgetProgram;

            this.widgetDrawable = context.createDrawable(this.solidProgram);
            this.widgetDrawable.addVertexBuffer("position", widgetVertices);

            Program<ContextType> newCircleProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "circle.frag"))
                .createProgram();

            if (this.circleProgram != null)
            {
                this.circleProgram.close();
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
                double xRemapped = Math.min(Math.max(x, 0), 1);
                double yRemapped = 1.0 - Math.min(Math.max(y, 0), 1);

                int index = 4 * (int)(Math.round((fboSize.height-1) * yRemapped) * fboSize.width + Math.round((fboSize.width-1) * xRemapped));
                return sceneObjectNameList.get(pixelObjectIDs[index]);
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
                double xRemapped = Math.min(Math.max(x, 0), 1);
                double yRemapped = 1.0 - Math.min(Math.max(y, 0), 1);

                int index = (int)(Math.round((fboSize.height-1) * yRemapped) * fboSize.width + Math.round((fboSize.width-1) * xRemapped));

                Matrix4 projectionInverse = getProjectionInverse();

                // Transform from screen space into camera space
                Vector4 unscaledPosition = projectionInverse
                    .times(new Vector4((float)(2 * x - 1), (float)(1 - 2 * y), 2 * (float)(0x0000FFFF & pixelDepths[index]) / (float)0xFFFF - 1, 1.0f));

                // Transform from camera space into world space.
                return getPartialViewMatrix().quickInverse(0.01f)
                        .times(unscaledPosition.getXYZ().dividedBy(unscaledPosition.w).asPosition())
                        .getXYZ().dividedBy(getScale());
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