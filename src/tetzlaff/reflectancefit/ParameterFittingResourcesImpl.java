package tetzlaff.reflectancefit;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;

import tetzlaff.gl.core.*;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.imagedata.GraphicsResources;
import tetzlaff.imagedata.SimpleLoadOptionsModel;
import tetzlaff.imagedata.ViewSet;

/**
 * An implementation of the ParameterFittingResources interface.
 * @param <ContextType> The type of the graphics context that will be used in a particular instance.
 */
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class ParameterFittingResourcesImpl<ContextType extends Context<ContextType>> implements AutoCloseable, ParameterFittingResources<ContextType>
{
    private final ContextType context;
    private final ReflectanceDataAccess reflectanceDataAccess;
    private final Options options;

    private GraphicsResources<ContextType> graphicsResources;
    private Program<ContextType> depthRenderingProgram;
    private VertexBuffer<ContextType> positionBuffer;
    private VertexBuffer<ContextType> texCoordBuffer;
    private VertexBuffer<ContextType> normalBuffer;
    private VertexBuffer<ContextType> tangentBuffer;
    private UniformBuffer<ContextType> lightIntensityBuffer;
    private Texture3D<ContextType> viewTextures;
    private Texture3D<ContextType> depthTextures;

    private Program<ContextType> diffuseFitProgram;
    private Program<ContextType> specularFitProgram;
    private Program<ContextType> holeFillProgram;

    private String materialFileName;
    private String materialName;

    /**
     * Creates a new instance of this implementation.
     * @param context The graphics context to be used by this implementation.
     * @param reflectanceDataAccess An implementation of ReflectanceDataAccess that provides access to the images, camera calibration information,
     *                              and geometry of the reflecting surface.
     * @param options Options provided to guide the reflectance estimation process.
     */
    public ParameterFittingResourcesImpl(ContextType context, ReflectanceDataAccess reflectanceDataAccess, Options options)
    {
        this.context = context;
        this.reflectanceDataAccess = reflectanceDataAccess;
        this.options = options;
    }

    @Override
    public String getMaterialFileName()
    {
        return materialFileName;
    }

    @Override
    public String getMaterialName()
    {
        return materialName;
    }

    /**
     * Initializes the graphics context, loads the geometry of the reflecting surface and the camera calibration information onto the graphics card,
     * and compiles the shader programs necessary for the reflectance parameter estimation.
     * This must be called prior to any other method, or the results of that method may be undefined.
     * @throws IOException Thrown if any error occurs while loading the reflecting surface geometry, the view set file, or the source code for the
     *                     shader programs.
     */
    public void initialize() throws IOException
    {
        context.getState().enableDepthTest();
        context.getState().disableBackFaceCulling();
        this.loadMeshAndViewSet();
        this.compileShaders();
    }

    /**
     * Loads all of the images (including masks if present), and also calibrates the intensity of the light source, if this calculation has been
     * requested (using depth images generated while loading the images).
     * This must be called after initialize() and prior to any other method, or the results of that method may be undefined.
     * @throws IOException Thrown if any error occurs while loading the images.
     */
    public void loadImagesAndCalibrateLight() throws IOException
    {
        // Load textures, generate visibility depth textures, estimate light source intensity
        double avgDistance = this.loadTextures();
        this.estimateLightIntensity(avgDistance);
    }

    @Override
    public void close()
    {
        if (viewTextures != null)
        {
            viewTextures.close();
        }

        if (depthTextures != null)
        {
            depthTextures.close();
        }

        if (lightIntensityBuffer != null)
        {
            lightIntensityBuffer.close();
        }

        if (diffuseFitProgram != null)
        {
            diffuseFitProgram.close();
        }

        if (depthRenderingProgram != null)
        {
            depthRenderingProgram.close();
        }

        if (specularFitProgram != null)
        {
            specularFitProgram.close();
        }

        if (holeFillProgram != null)
        {
            holeFillProgram.close();
        }

        if (graphicsResources != null)
        {
            graphicsResources.close();
        }

        if (positionBuffer != null)
        {
            positionBuffer.close();
        }

        if (normalBuffer != null)
        {
            normalBuffer.close();
        }

        if (texCoordBuffer != null)
        {
            texCoordBuffer.close();
        }

        if (tangentBuffer != null)
        {
            tangentBuffer.close();
        }
    }

    private void loadMeshAndViewSet() throws IOException
    {
        System.out.println("Loading mesh...");
        Date timestamp = new Date();

        VertexGeometry mesh = reflectanceDataAccess.retrieveMesh();

        positionBuffer = context.createVertexBuffer().setData(mesh.getVertices());
        texCoordBuffer = context.createVertexBuffer().setData(mesh.getTexCoords());
        normalBuffer = context.createVertexBuffer().setData(mesh.getNormals());
        tangentBuffer = context.createVertexBuffer().setData(mesh.getTangents());
        materialFileName = mesh.getMaterialFileName();

        if (materialFileName == null)
        {
            materialFileName = reflectanceDataAccess.getDefaultMaterialName() + ".mtl";
        }

        if (mesh.getMaterial() == null)
        {
            materialName = materialFileName.split("\\.")[0];
        }
        else
        {
            materialName = mesh.getMaterial().getName();
            if (materialName == null)
            {
                materialName = materialFileName.split("\\.")[0];
            }
        }

        System.out.println("Loading mesh completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

        System.out.println("Loading view set...");
        Date timestamp2 = new Date();

        reflectanceDataAccess.initializeViewSet();

        reflectanceDataAccess.getViewSet().setTonemapping(options.getGamma(), options.getLinearLuminanceValues(), options.getEncodedLuminanceValues());

        // Only generate view set uniform buffers
        graphicsResources = GraphicsResources.getBuilderForContext(context)
            .useExistingViewSet(reflectanceDataAccess.getViewSet())
            .useExistingGeometry(mesh)
            .setLoadOptions(new SimpleLoadOptionsModel()
                .setColorImagesRequested(false)
                .setDepthImagesRequested(false))
            .create();

        System.out.println("Loading view set completed in " + (new Date().getTime() - timestamp2.getTime()) + " milliseconds.");
    }

    @Override
    public Program<ContextType> getHoleFillProgram()
    {
        return holeFillProgram;
    }

    @Override
    public Texture3D<ContextType> getViewTextures()
    {
        return viewTextures;
    }

    @Override
    public Texture3D<ContextType> getDepthTextures()
    {
        return depthTextures;
    }

    @Override
    public DiffuseFit<ContextType> createDiffuseFit(Framebuffer<ContextType> framebuffer, int subdiv)
    {
        Drawable<ContextType> drawable = context.createDrawable(diffuseFitProgram);
        setupCommonShaderInputs(drawable);
        drawable.program().setUniform("delta", options.getDiffuseDelta());
        drawable.program().setUniform("iterations", options.getDiffuseIterations());
        drawable.program().setUniform("fit1Weight", Float.MAX_VALUE);
        drawable.program().setUniform("fit3Weight", options.getDiffuseComputedNormalWeight());
        return new DiffuseFit<>(drawable, framebuffer, subdiv);
    }

    @Override
    public SpecularFit<ContextType> createSpecularFit(Framebuffer<ContextType> framebuffer, int subdiv)
    {
        Drawable<ContextType> drawable = context.createDrawable(specularFitProgram);
        setupCommonShaderInputs(drawable);
        return new SpecularFit<>(drawable, framebuffer, subdiv);
    }

    private void compileShaders() throws IOException
    {
        System.out.println("Loading and compiling shader programs...");
        Date timestamp = new Date();

        depthRenderingProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "depth.vert").toFile())
                .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "common", "depth.frag").toFile())
                .createProgram();

        diffuseFitProgram = graphicsResources.getIBRShaderProgramBuilder()
                .define("VISIBILITY_TEST_ENABLED", options.isCameraVisibilityTestEnabled())
                .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace.vert").toFile())
                .addShader(ShaderType.FRAGMENT, Paths.get("shaders","reflectancefit", "diffusefit_imgspace.frag").toFile())
                .createProgram();

        specularFitProgram = graphicsResources.getIBRShaderProgramBuilder()
                .define("VISIBILITY_TEST_ENABLED", options.isCameraVisibilityTestEnabled())
                .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace.vert").toFile())
                .addShader(ShaderType.FRAGMENT, Paths.get("shaders","reflectancefit", "specularfit_imgspace.frag").toFile())
                .createProgram();

        holeFillProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texture.vert").toFile())
                .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "reflectancefit", "holefill.frag").toFile())
                .createProgram();

        System.out.println("Shader compilation completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    }

    private static double getLinearDepth(double nonLinearDepth, double nearPlane, double farPlane)
    {
        return 2 * nearPlane * farPlane / (farPlane + nearPlane - (2 * nonLinearDepth - 1) * (farPlane - nearPlane));
    }

    private double loadTextures() throws IOException
    {
        System.out.println("Loading images...");
        Date timestamp = new Date();

        ViewSet viewSet = reflectanceDataAccess.getViewSet();

        BufferedImage img = reflectanceDataAccess.retrieveImage(viewSet.getPrimaryViewIndex());

        viewTextures = context.getTextureFactory().build2DColorTextureArray(img.getWidth(), img.getHeight(), viewSet.getCameraPoseCount())
                        .setInternalFormat(CompressionFormat.RGB_PUNCHTHROUGH_ALPHA1_4BPP)
                        .setLinearFilteringEnabled(true)
                        .setMipmapsEnabled(true)
                        .createTexture();

        for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
        {

            viewTextures.loadLayer(i, reflectanceDataAccess.retrieveImage(i), reflectanceDataAccess.retrieveMask(i).orElse(null), true);

            System.out.println((i+1) + "/" + viewSet.getCameraPoseCount() + " images loaded.");
        }

        System.out.println("Image loading completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

        System.out.println("Creating depth maps...");
        Date timestamp2 = new Date();

        // Build depth textures for each view
        int depthWidth = viewTextures.getWidth() / 2;
        int depthHeight = viewTextures.getHeight() / 2;
        depthTextures = context.getTextureFactory().build2DDepthTextureArray(depthWidth, depthHeight, viewSet.getCameraPoseCount()).createTexture();

        // Don't automatically generate any texture attachments for this framebuffer object
        try(FramebufferObject<ContextType> depthRenderingFBO = context.buildFramebufferObject(depthWidth, depthHeight).createFramebufferObject())
        {
            Drawable<ContextType> depthRenderable = context.createDrawable(depthRenderingProgram);
            depthRenderable.addVertexBuffer("position", positionBuffer);

            double minDepth = viewSet.getRecommendedFarPlane();

            // Render each depth texture
            for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
            {
                depthRenderingFBO.setDepthAttachment(depthTextures.getLayerAsFramebufferAttachment(i));
                depthRenderingFBO.clearDepthBuffer();

                depthRenderingProgram.setUniform("model_view", viewSet.getCameraPose(i));
                depthRenderingProgram.setUniform("projection",
                    viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(i))
                        .getProjectionMatrix(
                            viewSet.getRecommendedNearPlane(),
                            viewSet.getRecommendedFarPlane()
                        )
                );

                depthRenderable.draw(PrimitiveMode.TRIANGLES, depthRenderingFBO);

                if (i == viewSet.getPrimaryViewIndex())
                {
                    short[] depthBufferData = depthRenderingFBO.readDepthBuffer();
                    for (short encodedDepth : depthBufferData)
                    {
                        int nonlinearDepth = 0xFFFF & (int) encodedDepth;
                        minDepth = Math.min(minDepth, getLinearDepth((double) nonlinearDepth / 0xFFFF,
                            viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
                    }
                }
            }

            System.out.println("Depth maps created in " + (new Date().getTime() - timestamp2.getTime()) + " milliseconds.");

            return minDepth;
        }
    }

    private void estimateLightIntensity(double avgDistance)
    {
        if (options.isLightIntensityEstimationEnabled())
        {
            Vector3 lightIntensity = new Vector3((float)(avgDistance * avgDistance));
            System.out.println("Using light intensity: " + lightIntensity.x + ' ' + lightIntensity.y + ' ' + lightIntensity.z);

            for (int i = 0; i < reflectanceDataAccess.getViewSet().getLightCount(); i++)
            {
                reflectanceDataAccess.getViewSet().setLightIntensity(i, lightIntensity);
            }

            graphicsResources.updateLightData();
        }
        else
        {
            System.out.println("Skipping light fit.");
        }

        lightIntensityBuffer =  graphicsResources.lightIntensityBuffer;
    }

    private void setupCommonShaderInputs(Drawable<ContextType> drawable)
    {
        drawable.addVertexBuffer("position", positionBuffer);
        drawable.addVertexBuffer("texCoord", texCoordBuffer);
        drawable.addVertexBuffer("normal", normalBuffer);
        drawable.addVertexBuffer("tangent", tangentBuffer);

        drawable.program().setUniformBuffer("CameraPoses", graphicsResources.cameraPoseBuffer);
        drawable.program().setUniformBuffer("CameraWeights", graphicsResources.cameraWeightBuffer);

        drawable.program().setUniformBuffer("CameraProjections", graphicsResources.cameraProjectionBuffer);
        drawable.program().setUniformBuffer("CameraProjectionIndices", graphicsResources.cameraProjectionIndexBuffer);

        drawable.program().setUniform("occlusionBias", options.getCameraVisibilityTestBias());
        drawable.program().setUniform("gamma", options.getGamma());

        if (graphicsResources.getLuminanceMap() == null)
        {
            drawable.program().setTexture("luminanceMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_1D));
        }
        else
        {
            drawable.program().setTexture("luminanceMap", graphicsResources.getLuminanceMap());
        }

        drawable.program().setUniformBuffer("LightIndices", graphicsResources.lightIndexBuffer);

        drawable.program().setUniformBuffer("LightPositions", graphicsResources.lightPositionBuffer);

        if (lightIntensityBuffer != null)
        {
            drawable.program().setUniformBuffer("LightIntensities", lightIntensityBuffer);
        }
        else
        {
            drawable.program().setUniformBuffer("LightIntensities", graphicsResources.lightIntensityBuffer);
        }
    }
}
