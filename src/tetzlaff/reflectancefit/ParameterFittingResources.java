package tetzlaff.reflectancefit;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Paths;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;

import tetzlaff.gl.builders.TextureBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.SimpleLoadOptionsModel;
import tetzlaff.ibrelight.ViewSet;
import tetzlaff.ibrelight.GraphicsResources;

@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class ParameterFittingResources<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private final ContextType context;
    private final File vsetFile;
    private final File objFile;
    private final Options param;

    private File imageDir;
    private File maskDir;
    private File rescaleDir;

    private String materialFileName;
    private String materialName;

    private ViewSet viewSet;
    private GraphicsResources<ContextType> graphicsResources;

    private Program<ContextType> depthRenderingProgram;
    private Program<ContextType> diffuseFitProgram;
    private Program<ContextType> specularFitProgram;
    private Program<ContextType> textureRectProgram;
    private Program<ContextType> holeFillProgram;

    private VertexBuffer<ContextType> positionBuffer;
    private VertexBuffer<ContextType> texCoordBuffer;
    private VertexBuffer<ContextType> normalBuffer;
    private VertexBuffer<ContextType> tangentBuffer;

    private UniformBuffer<ContextType> lightIntensityBuffer;

    private Texture3D<ContextType> viewTextures;
    private Texture3D<ContextType> depthTextures;

    public ParameterFittingResources(ContextType context, File vsetFile, File objFile, File imageDir, File maskDir, File rescaleDir, Options param)
    {
        this.context = context;
        this.vsetFile = vsetFile;
        this.objFile = objFile;
        this.imageDir = imageDir;
        this.maskDir = maskDir;
        this.rescaleDir = rescaleDir;
        this.param = param;
    }

    public ViewSet getViewSet()
    {
        return viewSet;
    }

    public String getMaterialFileName()
    {
        return materialFileName;
    }

    public String getMaterialName()
    {
        return materialName;
    }

    Program<ContextType> getHoleFillProgram()
    {
        return holeFillProgram;
    }

    Texture3D<ContextType> getViewTextures()
    {
        return viewTextures;
    }

    Texture3D<ContextType> getDepthTextures()
    {
        return depthTextures;
    }

    public void compileShaders() throws IOException
    {
        System.out.println("Loading and compiling shader programs...");
        Date timestamp = new Date();

        depthRenderingProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "depth.vert").toFile())
                .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "common", "depth.frag").toFile())
                .createProgram();

        diffuseFitProgram = graphicsResources.getIBRShaderProgramBuilder()
                .define("VISIBILITY_TEST_ENABLED", param.isCameraVisibilityTestEnabled())
                .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace.vert").toFile())
                .addShader(ShaderType.FRAGMENT, Paths.get("shaders","reflectancefit", "diffusefit_imgspace.frag").toFile())
                .createProgram();

        specularFitProgram = graphicsResources.getIBRShaderProgramBuilder()
                .define("VISIBILITY_TEST_ENABLED", param.isCameraVisibilityTestEnabled())
                .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace.vert").toFile())
                .addShader(ShaderType.FRAGMENT, Paths.get("shaders","reflectancefit", "specularfit_imgspace.frag").toFile())
                .createProgram();

        textureRectProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texture.vert").toFile())
                .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "common", "texture.frag").toFile())
                .createProgram();

        holeFillProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texture.vert").toFile())
                .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "reflectancefit", "holefill.frag").toFile())
                .createProgram();

        System.out.println("Shader compilation completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    }

    private VertexGeometry loadMesh() throws IOException
    {
        VertexGeometry mesh = VertexGeometry.createFromOBJFile(objFile);
        positionBuffer = context.createVertexBuffer().setData(mesh.getVertices());
        texCoordBuffer = context.createVertexBuffer().setData(mesh.getTexCoords());
        normalBuffer = context.createVertexBuffer().setData(mesh.getNormals());
        tangentBuffer = context.createVertexBuffer().setData(mesh.getTangents());
        materialFileName = mesh.getMaterialFileName();

        if (materialFileName == null)
        {
            materialFileName = objFile.getName().split("\\.")[0] + ".mtl";
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

        return mesh;
    }

    public void loadMeshAndViewSet() throws IOException, XMLStreamException
    {
        System.out.println("Loading mesh...");
        Date timestamp = new Date();
        VertexGeometry mesh = loadMesh();

        System.out.println("Loading mesh completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

        System.out.println("Loading view set...");
        Date timestamp2 = new Date();

        String[] vsetFileNameParts = vsetFile.getName().split("\\.");
        String fileExt = vsetFileNameParts[vsetFileNameParts.length-1];
        if ("vset".equalsIgnoreCase(fileExt))
        {
            System.out.println("Loading from VSET file.");

            viewSet = ViewSet.loadFromVSETFile(vsetFile);
        }
        else if ("xml".equalsIgnoreCase(fileExt))
        {
            System.out.println("Loading from Agisoft Photoscan XML file.");
            viewSet = ViewSet.loadFromAgisoftXMLFile(vsetFile);
            viewSet.setInfiniteLightSources(false);
        }
        else
        {
            throw new IllegalStateException("Unrecognized file type.");
        }

        viewSet.setTonemapping(param.getGamma(), param.getLinearLuminanceValues(), param.getEncodedLuminanceValues());

        // Only generate view set uniform buffers
        graphicsResources = GraphicsResources.getBuilderForContext(context)
            .useExistingViewSet(viewSet)
            .useExistingGeometry(mesh)
            .setLoadOptions(new SimpleLoadOptionsModel()
                .setColorImagesRequested(false)
                .setDepthImagesRequested(false))
            .create();

        System.out.println("Loading view set completed in " + (new Date().getTime() - timestamp2.getTime()) + " milliseconds.");
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

        drawable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
        drawable.program().setUniform("gamma", param.getGamma());

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

    DiffuseFit<ContextType> createDiffuseFit(Framebuffer<ContextType> framebuffer, int subdiv)
    {
        Drawable<ContextType> drawable = context.createDrawable(diffuseFitProgram);
        setupCommonShaderInputs(drawable);
        drawable.program().setUniform("delta", param.getDiffuseDelta());
        drawable.program().setUniform("iterations", param.getDiffuseIterations());
        drawable.program().setUniform("fit1Weight", Float.MAX_VALUE);
        drawable.program().setUniform("fit3Weight", param.getDiffuseComputedNormalWeight());
        return new DiffuseFit<>(drawable, framebuffer, subdiv);
    }

    SpecularFit<ContextType> createSpecularFit(Framebuffer<ContextType> framebuffer, int subdiv)
    {
        Drawable<ContextType> drawable = context.createDrawable(specularFitProgram);
        setupCommonShaderInputs(drawable);
        return new SpecularFit<>(drawable, framebuffer, subdiv);
    }

    private static double getLinearDepth(double nonLinearDepth, double nearPlane, double farPlane)
    {
        return 2 * nearPlane * farPlane / (farPlane + nearPlane - (2 * nonLinearDepth - 1) * (farPlane - nearPlane));
    }

    public double loadTextures() throws IOException
    {
        if (param.isImageRescalingEnabled())
        {
            System.out.println("Rescaling images...");
            Date timestamp = new Date();

            Drawable<ContextType> downsampleRenderable = context.createDrawable(textureRectProgram);

            try
            (
                // Create an FBO for downsampling
                FramebufferObject<ContextType> downsamplingFBO =
                context.buildFramebufferObject(param.getImageWidth(), param.getImageHeight())
                    .addColorAttachment()
                    .createFramebufferObject();

                VertexBuffer<ContextType> rectBuffer = context.createRectangle()
            )
            {
                downsampleRenderable.addVertexBuffer("position", rectBuffer);

                // Downsample and store each image
                for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
                {
                    File imageFile = GraphicsResources.findImageFile(new File(imageDir, viewSet.getImageFileName(i)));

                    TextureBuilder<ContextType, ? extends Texture2D<ContextType>> fullSizeImageBuilder;

                    if (maskDir == null)
                    {
                        fullSizeImageBuilder = context.getTextureFactory().build2DColorTextureFromFile(imageFile, true);
                    }
                    else
                    {
                        File maskFile = GraphicsResources.findImageFile(new File(maskDir, viewSet.getImageFileName(0)));

                        fullSizeImageBuilder = context.getTextureFactory().build2DColorTextureFromFileWithMask(imageFile, maskFile, true);
                    }

                    try(Texture2D<ContextType> fullSizeImage = fullSizeImageBuilder
                            .setLinearFilteringEnabled(true)
                            .setMipmapsEnabled(true)
                            .createTexture())
                    {
                        downsamplingFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

                        textureRectProgram.setTexture("tex", fullSizeImage);

                        downsampleRenderable.draw(PrimitiveMode.TRIANGLE_FAN, downsamplingFBO);
                        context.finish();

                        if (rescaleDir != null)
                        {
                            String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
                            filenameParts[filenameParts.length - 1] = "png";
                            String pngFileName = String.join(".", filenameParts);
                            downsamplingFBO.saveColorBufferToFile(0, "PNG", new File(rescaleDir, pngFileName));
                        }
                    }

                    System.out.println((i+1) + "/" + viewSet.getCameraPoseCount() + " images rescaled.");
                }
            }

            System.out.println("Rescaling completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

            // Use rescale directory in the future
            imageDir = rescaleDir;
            rescaleDir = null;
            maskDir = null;
        }

        System.out.println("Loading images...");
        Date timestamp = new Date();

        // Read a single image to get the dimensions for the texture array
        File imageFile = GraphicsResources.findImageFile(new File(imageDir, viewSet.getImageFileName(0)));
        BufferedImage img = ImageIO.read(new FileInputStream(imageFile));
        viewTextures = context.getTextureFactory().build2DColorTextureArray(img.getWidth(), img.getHeight(), viewSet.getCameraPoseCount())
                        .setInternalFormat(CompressionFormat.RGB_PUNCHTHROUGH_ALPHA1_4BPP)
                        .setLinearFilteringEnabled(true)
                        .setMipmapsEnabled(true)
                        .createTexture();

        for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
        {
            imageFile = GraphicsResources.findImageFile(new File(imageDir, viewSet.getImageFileName(i)));

            if (maskDir == null)
            {
                viewTextures.loadLayer(i, imageFile, true);
            }
            else
            {
                File maskFile = GraphicsResources.findImageFile(new File(maskDir, viewSet.getImageFileName(i)));
                viewTextures.loadLayer(i, imageFile, maskFile, true);
            }

            System.out.println((i+1) + "/" + viewSet.getCameraPoseCount() + " images loaded.");
        }

        System.out.println("Image loading completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

        System.out.println("Creating depth maps...");
        Date timestamp2 = new Date();

        // Build depth textures for each view
        int width = viewTextures.getWidth() / 2;
        int height = viewTextures.getHeight() / 2;
        depthTextures = context.getTextureFactory().build2DDepthTextureArray(width, height, viewSet.getCameraPoseCount()).createTexture();

        // Don't automatically generate any texture attachments for this framebuffer object
        try(FramebufferObject<ContextType> depthRenderingFBO = context.buildFramebufferObject(width, height).createFramebufferObject())
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
                        minDepth = Math.min(minDepth, getLinearDepth((double) nonlinearDepth / 0xFFFF, viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
                    }
                }
            }

            System.out.println("Depth maps created in " + (new Date().getTime() - timestamp2.getTime()) + " milliseconds.");

            return minDepth;
        }
    }

    public void estimateLightIntensity(double avgDistance)
    {
        if (param.isLightIntensityEstimationEnabled())
        {
            Vector3 lightIntensity = new Vector3((float)(avgDistance * avgDistance));
            System.out.println("Using light intensity: " + lightIntensity.x + ' ' + lightIntensity.y + ' ' + lightIntensity.z);

            for (int i = 0; i < viewSet.getLightCount(); i++)
            {
                viewSet.setLightIntensity(i, lightIntensity);
            }

            graphicsResources.updateLightData();
        }
        else
        {
            System.out.println("Skipping light fit.");
        }

        lightIntensityBuffer =  graphicsResources.lightIntensityBuffer;
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

        if (textureRectProgram != null)
        {
            textureRectProgram.close();
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
}
