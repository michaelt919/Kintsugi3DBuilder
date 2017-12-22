package tetzlaff.ibrelight.rendering;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.gl.material.Material;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.types.AbstractDataTypeFactory;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.IntVector2;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.ReadonlyLoadOptionsModel;
import tetzlaff.ibrelight.core.RenderingMode;
import tetzlaff.ibrelight.core.ViewSet;

public final class IBRResources<ContextType extends Context<ContextType>> implements AutoCloseable
{
    public final ContextType context;
    public final ViewSet viewSet;
    public final VertexGeometry geometry;

    /**
     * A GPU buffer containing the camera poses defining the transformation from object space to camera space for each view.
     * These are necessary to perform projective texture mapping.
     */
    public final UniformBuffer<ContextType> cameraPoseBuffer;

    /**
     * A GPU buffer containing projection transformations defining the intrinsic properties of each camera.
     */
    public final UniformBuffer<ContextType> cameraProjectionBuffer;

    /**
     * A GPU buffer containing for every view an index designating the projection transformation that should be used for each view.
     */
    public final UniformBuffer<ContextType> cameraProjectionIndexBuffer;

    /**
     * A GPU buffer containing light source positions, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * Assumed by convention to be in camera space.
     */
    public final UniformBuffer<ContextType> lightPositionBuffer;

    /**
     * A GPU buffer containing light source intensities, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     */
    public final UniformBuffer<ContextType> lightIntensityBuffer;

    /**
     * A GPU buffer containing for every view an index designating the light source position and intensity that should be used for each view.
     */
    public final UniformBuffer<ContextType> lightIndexBuffer;

    /**
     * A texture array instantiated on the GPU containing the image corresponding to each view in this dataset.
     */
    public final Texture3D<ContextType> colorTextures;

    public final Texture3D<ContextType> eigentextures;

    /**
     * A 1D texture defining how encoded RGB values should be converted to linear luminance.
     */
    public final Texture1D<ContextType> luminanceMap;

    /**
     * A 1D texture defining how encoded RGB values should be converted to linear luminance.
     */
    public final Texture1D<ContextType> inverseLuminanceMap;

    public final VertexBuffer<ContextType> positionBuffer;
    public final VertexBuffer<ContextType> texCoordBuffer;
    public final VertexBuffer<ContextType> normalBuffer;
    public final VertexBuffer<ContextType> tangentBuffer;
    public final Texture3D<ContextType> depthTextures;
    public final Texture2D<ContextType> diffuseTexture;
    public final Texture2D<ContextType> normalTexture;
    public final Texture2D<ContextType> specularTexture;
    public final Texture2D<ContextType> roughnessTexture;
    public final Texture3D<ContextType> shadowTextures;
    public final UniformBuffer<ContextType> shadowMatrixBuffer;
    public final UniformBuffer<ContextType> cameraWeightBuffer;

    private final double primaryViewDistance;

    private final float[] cameraWeights;

    public static final class Builder<ContextType extends Context<ContextType>>
    {
        private final ContextType context;
        private ViewSet viewSet;
        private VertexGeometry geometry;
        private File imageDirectoryOverride;
        private ReadonlyLoadOptionsModel loadOptions;
        private LoadingMonitor loadingMonitor;

        private float gamma;
        private double[] linearLuminanceValues;
        private byte[] encodedLuminanceValues;

        private Builder(ContextType context)
        {
            this.context = context;
        }

        public Builder<ContextType> setLoadOptions(ReadonlyLoadOptionsModel loadOptions)
        {
            this.loadOptions = loadOptions;
            return this;
        }

        public Builder<ContextType> setLoadingMonitor(LoadingMonitor loadingMonitor)
        {
            this.loadingMonitor = loadingMonitor;
            return this;
        }

        public Builder<ContextType> setTonemapping(float gamma, double[] linearLuminanceValues, byte[] encodedLuminanceValues)
        {
            this.gamma = gamma;
            this.linearLuminanceValues = Arrays.copyOf(linearLuminanceValues, linearLuminanceValues.length);
            this.encodedLuminanceValues = Arrays.copyOf(encodedLuminanceValues, encodedLuminanceValues.length);
            return this;
        }

        public Builder<ContextType> loadVSETFile(File vsetFile) throws FileNotFoundException
        {
            this.viewSet = ViewSet.loadFromVSETFile(vsetFile);
            try
            {
                this.geometry = VertexGeometry.createFromOBJFile(this.viewSet.getGeometryFile());
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            return this;
        }

        // undistorted images are defined in the load options
        public Builder<ContextType> loadAgisoftFiles(File cameraFile, File geometryFile, File undistortedImageDirectory) throws FileNotFoundException
        {
            this.viewSet = ViewSet.loadFromAgisoftXMLFile(cameraFile);
            Path parentDirectory = cameraFile.getParentFile().toPath();
            if (geometryFile != null)
            {
                this.geometry = VertexGeometry.createFromOBJFile(geometryFile);
                this.viewSet.setGeometryFileName(parentDirectory.relativize(geometryFile.toPath()).toString());
            }
            if (undistortedImageDirectory != null)
            {
                this.imageDirectoryOverride = undistortedImageDirectory;
                this.viewSet.setRelativeImagePathName(parentDirectory.relativize(undistortedImageDirectory.toPath()).toString());
            }
            return this;
        }

        public Builder<ContextType> useExistingViewSet(ViewSet existingViewSet)
        {
            this.viewSet = existingViewSet;
            return this;
        }

        public Builder<ContextType> useExistingGeometry(VertexGeometry existingGeometry)
        {
            this.geometry = existingGeometry;
            return this;
        }

        public Builder<ContextType> overrideImageDirectory(File imageDirectory)
        {
            this.imageDirectoryOverride = imageDirectory;
            return this;
        }

        public IBRResources<ContextType> create() throws IOException
        {
            if (linearLuminanceValues != null && encodedLuminanceValues != null)
            {
                viewSet.setTonemapping(gamma, linearLuminanceValues, encodedLuminanceValues);
            }

            if (imageDirectoryOverride != null)
            {
                viewSet.setRelativeImagePathName(viewSet.getRootDirectory().toPath().relativize(imageDirectoryOverride.toPath()).toString());
            }

            return new IBRResources<>(context, viewSet, geometry, loadOptions, loadingMonitor);
        }
    }

    public static <ContextType extends Context<ContextType>> Builder<ContextType> getBuilderForContext(ContextType context)
    {
        return new Builder<>(context);
    }

    private IBRResources(ContextType context, ViewSet viewSet, VertexGeometry geometry, ReadonlyLoadOptionsModel loadOptions, LoadingMonitor loadingMonitor) throws IOException
    {
        this.context = context;
        this.viewSet = viewSet;
        this.geometry = geometry;

        if (geometry != null)
        {
            this.cameraWeights = computeCameraWeights(viewSet, geometry);
            cameraWeightBuffer = context.createUniformBuffer().setData(NativeVectorBufferFactory.getInstance().createFromFloatArray(1, viewSet.getCameraPoseCount(), cameraWeights));
        }
        else
        {
            this.cameraWeights = null;
            this.cameraWeightBuffer = null;
        }

        // Store the poses in a uniform buffer
        if (viewSet.getCameraPoseData() != null)
        {
            // Create the uniform buffer
            cameraPoseBuffer = context.createUniformBuffer().setData(viewSet.getCameraPoseData());
        }
        else
        {
            cameraPoseBuffer = null;
        }

        // Store the camera projections in a uniform buffer
        if (viewSet.getCameraProjectionData() != null)
        {
            // Create the uniform buffer
            cameraProjectionBuffer = context.createUniformBuffer().setData(viewSet.getCameraProjectionData());
        }
        else
        {
            cameraProjectionBuffer = null;
        }

        // Store the camera projection indices in a uniform buffer
        if (viewSet.getCameraProjectionIndexData() != null)
        {
            cameraProjectionIndexBuffer = context.createUniformBuffer().setData(viewSet.getCameraProjectionIndexData());
        }
        else
        {
            cameraProjectionIndexBuffer = null;
        }

        // Store the light positions in a uniform buffer
        if (viewSet.getLightPositionData() != null)
        {
            // Create the uniform buffer
            lightPositionBuffer = context.createUniformBuffer().setData(viewSet.getLightPositionData());
        }
        else
        {
            lightPositionBuffer = null;
        }

        // Store the light positions in a uniform buffer
        if (viewSet.getLightIntensityData() != null)
        {
            // Create the uniform buffer
            lightIntensityBuffer = context.createUniformBuffer().setData(viewSet.getLightIntensityData());
        }
        else
        {
            lightIntensityBuffer = null;
        }

        // Store the light indices indices in a uniform buffer
        if (viewSet.getLightIndexData() != null)
        {
            lightIndexBuffer = context.createUniformBuffer().setData(viewSet.getLightIndexData());
        }
        else
        {
            lightIndexBuffer = null;
        }

        // Luminance map texture
        if (viewSet.getLuminanceEncoding() != null)
        {
            luminanceMap = viewSet.getLuminanceEncoding().createLuminanceMap(context);
            inverseLuminanceMap = viewSet.getLuminanceEncoding().createInverseLuminanceMap(context);
        }
        else
        {
            luminanceMap = null;
            inverseLuminanceMap = null;
        }

        // Read the images from a file
        if (loadOptions != null && loadOptions.areColorImagesRequested() && viewSet.getImageFilePath() != null && viewSet.getCameraPoseCount() > 0)
        {
            Date timestamp = new Date();

            // Try to read the eigentextures
            BufferedImage img;

            // Read a single image to get the dimensions for the texture array
            try (InputStream input = new FileInputStream(new File(viewSet.getImageFilePath(), "sv_0000_00_00.png"))) // myZip.retrieveFile(imageFile);
            {
                img = ImageIO.read(input);
            }

            if (img == null)
            {
                System.out.println("Eigentextures not found.  Loading view images as normal textures.");
                this.eigentextures = null;
            }
            else
            {
                Texture3D<ContextType> eigentexturesTemp = null;

                try
                {
                    eigentexturesTemp = context.getTextureFactory()
                        .build2DColorTextureArray(img.getWidth(), img.getHeight(), viewSet.getCameraPoseCount())
                        .setInternalFormat(CompressionFormat.RED_4BPP)
                        .setMipmapsEnabled(true)
                        .setLinearFilteringEnabled(true)
                        .setMaxAnisotropy(16.0f)
                        .createTexture();

                    // TODO don't hardcode
                    eigentexturesTemp.loadLayer(0, new File(viewSet.getImageFilePath(), "sv_0000_00_00.png"), true);
                    eigentexturesTemp.loadLayer(1, new File(viewSet.getImageFilePath(), "sv_0001_00_01.png"), true);
                    eigentexturesTemp.loadLayer(2, new File(viewSet.getImageFilePath(), "sv_0002_00_02.png"), true);
                    eigentexturesTemp.loadLayer(3, new File(viewSet.getImageFilePath(), "sv_0003_00_03.png"), true);
                    eigentexturesTemp.loadLayer(4, new File(viewSet.getImageFilePath(), "sv_0004_01_00.png"), true);
                    eigentexturesTemp.loadLayer(5, new File(viewSet.getImageFilePath(), "sv_0005_01_01.png"), true);
                    eigentexturesTemp.loadLayer(6, new File(viewSet.getImageFilePath(), "sv_0006_01_02.png"), true);
                    eigentexturesTemp.loadLayer(7, new File(viewSet.getImageFilePath(), "sv_0007_01_03.png"), true);
                    eigentexturesTemp.loadLayer(8, new File(viewSet.getImageFilePath(), "sv_0008_02_00.png"), true);
                    eigentexturesTemp.loadLayer(9, new File(viewSet.getImageFilePath(), "sv_0009_02_01.png"), true);
                    eigentexturesTemp.loadLayer(10, new File(viewSet.getImageFilePath(), "sv_0010_02_02.png"), true);
                    eigentexturesTemp.loadLayer(11, new File(viewSet.getImageFilePath(), "sv_0011_02_03.png"), true);
                    eigentexturesTemp.loadLayer(12, new File(viewSet.getImageFilePath(), "sv_0012_03_00.png"), true);
                    eigentexturesTemp.loadLayer(13, new File(viewSet.getImageFilePath(), "sv_0013_03_01.png"), true);
                    eigentexturesTemp.loadLayer(14, new File(viewSet.getImageFilePath(), "sv_0014_03_02.png"), true);
                    eigentexturesTemp.loadLayer(15, new File(viewSet.getImageFilePath(), "sv_0015_03_03.png"), true);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                this.eigentextures = eigentexturesTemp;
            }

            File imageFile = viewSet.getImageFile(0);

            if (!imageFile.exists())
            {
                // Try some alternate file formats/extensions
                String[] altFormats = { "png", "PNG", "jpg", "JPG", "jpeg", "JPEG" };
                for(String extension : altFormats)
                {
                    String[] filenameParts = viewSet.getImageFileName(0).split("\\.");
                    filenameParts[filenameParts.length - 1] = extension;
                    String altFileName = String.join(".", filenameParts);
                    File imageFileGuess = new File(imageFile.getParentFile(), altFileName);

                    System.out.printf("Trying '%s'\n", imageFileGuess.getAbsolutePath());
                    if (imageFileGuess.exists())
                    {
                        System.out.printf("Found!!\n");
                        imageFile = imageFileGuess;
                        break;
                    }
                }

                // Is it still not there?
                if (!imageFile.exists())
                {
                    throw new FileNotFoundException(
                            String.format("'%s' not found.", viewSet.getImageFileName(0)));
                }
            }

            // Read a single image to get the dimensions for the texture array
            try(InputStream input = new FileInputStream(imageFile)) // myZip.retrieveFile(imageFile);
            {
                img = ImageIO.read(input);
            }

            if (img == null)
            {
                throw new IOException(String.format("Error: Unsupported image format '%s'.",
                        viewSet.getImageFileName(0)));
            }

            ColorTextureBuilder<ContextType, ? extends Texture3D<ContextType>> textureArrayBuilder =
                    context.getTextureFactory().build2DColorTextureArray(img.getWidth(), img.getHeight(), viewSet.getCameraPoseCount());

            if (this.eigentextures == null)
            {
                if (loadOptions.isCompressionRequested())
                {
                    textureArrayBuilder.setInternalFormat(CompressionFormat.RGB_PUNCHTHROUGH_ALPHA1_4BPP);
                }
                else
                {
                    textureArrayBuilder.setInternalFormat(ColorFormat.RGBA8);
                }

                if (loadOptions.areMipmapsRequested())
                {
                    textureArrayBuilder.setMipmapsEnabled(true);
                }
                else
                {
                    textureArrayBuilder.setMipmapsEnabled(false);
                }

                textureArrayBuilder.setLinearFilteringEnabled(true);
                textureArrayBuilder.setMaxAnisotropy(16.0f);
            }
            else
            {
                textureArrayBuilder.setInternalFormat(ColorFormat.R16UI);
                textureArrayBuilder.setMipmapsEnabled(false);
                textureArrayBuilder.setLinearFilteringEnabled(false);
            }

            colorTextures = textureArrayBuilder.createTexture();

            if(loadingMonitor != null)
            {
                loadingMonitor.setMaximum(viewSet.getCameraPoseCount());
            }

            int m = viewSet.getCameraPoseCount();
            for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
            {
                System.out.printf("%d/%d", i, m);
                System.out.println();
                imageFile = viewSet.getImageFile(i);
                if (!imageFile.exists())
                {
                    // Try some alternate file formats/extensions
                    String[] altFormats = { "png", "PNG", "jpg", "JPG", "jpeg", "JPEG" };
                    for(String extension : altFormats)
                    {
                        String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
                        filenameParts[filenameParts.length - 1] = extension;
                        String altFileName = String.join(".", filenameParts);
                        File imageFileGuess = new File(imageFile.getParentFile(), altFileName);

                        if (imageFileGuess.exists())
                        {
                            imageFile = imageFileGuess;
                            break;
                        }
                    }

                    // Is it still not there?
                    if (!imageFile.exists())
                    {
                        throw new FileNotFoundException(
                                String.format("'%s' not found.", viewSet.getImageFileName(i)));
                    }
                }

                if (this.eigentextures == null)
                {
                    this.colorTextures.loadLayer(i, imageFile, true);
                }
                else
                {
                    this.colorTextures.loadLayer(i, imageFile, true,
                        AbstractDataTypeFactory.getInstance().getSingleComponentDataType(NativeDataType.SHORT),
                        color -> ((0x1F & (Math.round(Math.max(-1.0, (color.getRed() - 128) * 1.0 / 127.0) * 15) + 16)) << 11)
                            | ((0x3F & (Math.round(Math.max(-1.0, (color.getGreen() - 128) * 1.0 / 127.0) * 31) + 32)) << 5)
                            | (0x1F & (Math.round(Math.max(-1.0, (color.getBlue() - 128) * 1.0 / 127.0) * 15) + 16)));
                }

                if(loadingMonitor != null)
                {
                    loadingMonitor.setProgress(i+1);
                }
            }

            System.out.println("View Set textures loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
        }
        else
        {
            this.colorTextures = null;
            this.eigentextures = null;
        }

        if (loadingMonitor != null)
        {
            loadingMonitor.setMaximum(0.0);
        }

        if (geometry != null)
        {
            this.positionBuffer = context.createVertexBuffer().setData(geometry.getVertices());

            try
            (
                // Don't automatically generate any texture attachments for this framebuffer object
                FramebufferObject<ContextType> depthRenderingFBO =
                    context.buildFramebufferObject(loadOptions.getDepthImageWidth(), loadOptions.getDepthImageHeight())
                        .createFramebufferObject();

                // Load the program
                Program<ContextType> depthRenderingProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
                    .addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"))
                    .createProgram()
            )
            {
                Drawable<ContextType> depthDrawable = context.createDrawable(depthRenderingProgram);
                depthDrawable.addVertexBuffer("position", positionBuffer);

                double minDepth = viewSet.getRecommendedFarPlane();

                if (loadOptions.areDepthImagesRequested())
                {
                    // Build depth textures for each view
                    this.depthTextures =
                        context.getTextureFactory().build2DDepthTextureArray(
                                loadOptions.getDepthImageWidth(), loadOptions.getDepthImageHeight(), viewSet.getCameraPoseCount())
                            .createTexture();

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

                        depthDrawable.draw(PrimitiveMode.TRIANGLES, depthRenderingFBO);

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
                }
                else
                {
                    this.depthTextures = null;

                    Texture2D<ContextType> depthAttachment =
                        context.getTextureFactory().build2DDepthTexture(loadOptions.getDepthImageWidth(), loadOptions.getDepthImageHeight())
                            .createTexture();
                    depthRenderingFBO.setDepthAttachment(depthAttachment);

                    depthRenderingFBO.clearDepthBuffer();

                    depthRenderingProgram.setUniform("model_view", viewSet.getCameraPose(viewSet.getPrimaryViewIndex()));
                    depthRenderingProgram.setUniform("projection",
                        viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewSet.getPrimaryViewIndex()))
                            .getProjectionMatrix(
                                viewSet.getRecommendedNearPlane(),
                                viewSet.getRecommendedFarPlane()
                            )
                    );

                    depthDrawable.draw(PrimitiveMode.TRIANGLES, depthRenderingFBO);

                    short[] depthBufferData = depthRenderingFBO.readDepthBuffer();
                    for (short encodedDepth : depthBufferData)
                    {
                        int nonlinearDepth = 0xFFFF & (int) encodedDepth;
                        minDepth = Math.min(minDepth, getLinearDepth((double) nonlinearDepth / 0xFFFF,
                            viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
                    }
                }

                primaryViewDistance = minDepth;
            }
        }
        else
        {
            this.positionBuffer = null;
            this.depthTextures = null;
            primaryViewDistance = 0.0;
        }

        if (geometry != null && geometry.hasTexCoords())
        {
            this.texCoordBuffer = context.createVertexBuffer().setData(geometry.getTexCoords());
        }
        else
        {
            this.texCoordBuffer = null;
        }
        
        if (geometry != null && geometry.hasNormals())
        {
            this.normalBuffer = context.createVertexBuffer().setData(geometry.getNormals());
        }
        else
        {
            this.normalBuffer = null;
        }
        
        if (geometry != null && geometry.hasTexCoords() && geometry.hasNormals())
        {
            this.tangentBuffer = context.createVertexBuffer().setData(geometry.getTangents());
        }
        else
        {
            this.tangentBuffer = null;
        }

        // TODO Use more information from the material.  Currently just pulling texture names.
        if (this.geometry != null)
        {
            Material material = this.geometry.getMaterial();
            String diffuseTextureName = null;
            String normalTextureName = null;
            String specularTextureName = null;
            String roughnessTextureName = null;

            if (material != null)
            {
                if (material.getDiffuseMap() != null)
                {
                    diffuseTextureName = material.getDiffuseMap().getMapName();
                }

                if (material.getNormalMap() != null)
                {
                    normalTextureName = material.getNormalMap().getMapName();
                }

                if (material.getSpecularMap() != null)
                {
                    specularTextureName = material.getSpecularMap().getMapName();
                }

                if (material.getRoughnessMap() != null)
                {
                    roughnessTextureName = material.getRoughnessMap().getMapName();
                }
            }

            if (this.viewSet.getGeometryFileName() != null)
            {
                String prefix = this.viewSet.getGeometryFileName().split("\\.")[0];
                diffuseTextureName = diffuseTextureName != null ? diffuseTextureName : prefix + "_Kd.png";
                normalTextureName = normalTextureName != null ? normalTextureName : prefix + "_norm.png";
                specularTextureName = specularTextureName != null ? specularTextureName : prefix + "_Ks.png";
                roughnessTextureName = roughnessTextureName != null ? roughnessTextureName : prefix + "_Pr.png";
            }
            else
            {
                diffuseTextureName = diffuseTextureName != null ? diffuseTextureName : "diffuse.png";
                normalTextureName = normalTextureName != null ? normalTextureName : "normal.png";
                specularTextureName = specularTextureName != null ? specularTextureName : "specular.png";
                roughnessTextureName = roughnessTextureName != null ? roughnessTextureName : "roughness.png";
            }

            File diffuseFile = new File(this.geometry.getFilename().getParentFile(), diffuseTextureName);
            File normalFile = new File(this.geometry.getFilename().getParentFile(), normalTextureName);
            File specularFile = new File(this.geometry.getFilename().getParentFile(), specularTextureName);
            File roughnessFile = new File(this.geometry.getFilename().getParentFile(), roughnessTextureName);

            if (diffuseFile != null && diffuseFile.exists())
            {
                System.out.println("Diffuse texture found.");
                ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> diffuseTextureBuilder =
                    context.getTextureFactory().build2DColorTextureFromFile(diffuseFile, true);

//                if (loadOptions.isCompressionRequested())
//                {
//                    diffuseTextureBuilder.setInternalFormat(CompressionFormat.RGB_4BPP);
//                }
//                else
                diffuseTextureBuilder.setInternalFormat(ColorFormat.RGB8);

                diffuseTexture = diffuseTextureBuilder
                        .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                        .setLinearFilteringEnabled(true)
                        .createTexture();
            }
            else
            {
                diffuseTexture = null;
            }

            if (normalFile != null && normalFile.exists())
            {
                System.out.println("Normal texture found.");
                ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> normalTextureBuilder =
                    context.getTextureFactory().build2DColorTextureFromFile(normalFile, true);

//                if (loadOptions.isCompressionRequested())
//                {
//                    normalTextureBuilder.setInternalFormat(CompressionFormat.RED_4BPP_GREEN_4BPP);
//                }
//                else
                normalTextureBuilder.setInternalFormat(ColorFormat.RG8);

                normalTexture = normalTextureBuilder
                        .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                        .setLinearFilteringEnabled(true)
                        .createTexture();
            }
            else
            {
                normalTexture = null;
            }

            if (specularFile != null && specularFile.exists())
            {
                System.out.println("Specular texture found.");
                ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> specularTextureBuilder =
                    context.getTextureFactory().build2DColorTextureFromFile(specularFile, true);
//                if (loadOptions.isCompressionRequested())
//                {
//                    specularTextureBuilder.setInternalFormat(CompressionFormat.RGB_4BPP);
//                }
//                else
                specularTextureBuilder.setInternalFormat(ColorFormat.RGB8);

                specularTexture = specularTextureBuilder
                        .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                        .setLinearFilteringEnabled(true)
                        .createTexture();
            }
            else
            {
                specularTexture = null;
            }

            if (roughnessFile != null && roughnessFile.exists())
            {
                System.out.println("Roughness texture found.");
                ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> roughnessTextureBuilder =
                    context.getTextureFactory().build2DColorTextureFromFile(roughnessFile, true);

//                if (loadOptions.isCompressionRequested())
//                {
//                    roughnessTextureBuilder.setInternalFormat(CompressionFormat.RGB_4BPP);
//                }
//                else
                roughnessTextureBuilder.setInternalFormat(ColorFormat.RGB8);

                roughnessTexture = roughnessTextureBuilder
                        .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                        .setLinearFilteringEnabled(true)
                        .createTexture();
            }
            else
            {
                roughnessTexture = null;
            }
        }
        else
        {
            diffuseTexture = null;
            normalTexture = null;
            specularTexture = null;
            roughnessTexture = null;
        }

        if (this.depthTextures != null)
        {
            shadowTextures =
                context.getTextureFactory()
                    .build2DDepthTextureArray(this.depthTextures.getWidth(), this.depthTextures.getHeight(), this.viewSet.getCameraPoseCount())
                    .createTexture();

            try
            (
                // Don't automatically generate any texture attachments for this framebuffer object
                FramebufferObject<ContextType> depthRenderingFBO =
                    context.buildFramebufferObject(this.depthTextures.getWidth(), this.depthTextures.getHeight())
                        .createFramebufferObject();

                // Load the program
                Program<ContextType> depthRenderingProgram = context.getShaderProgramBuilder()
                        .addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
                        .addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"))
                        .createProgram()
            )
            {
                Drawable<ContextType> depthDrawable = context.createDrawable(depthRenderingProgram);
                depthDrawable.addVertexBuffer("position", this.positionBuffer);

                // Flatten the camera pose matrices into 16-component vectors and store them in the vertex list data structure.
                NativeVectorBuffer flattenedShadowMatrices = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 16, this.viewSet.getCameraPoseCount());

                // Render each depth texture
                for (int i = 0; i < this.viewSet.getCameraPoseCount(); i++)
                {
                    depthRenderingFBO.setDepthAttachment(shadowTextures.getLayerAsFramebufferAttachment(i));
                    depthRenderingFBO.clearDepthBuffer();

                    depthRenderingProgram.setUniform("model_view", this.viewSet.getCameraPose(i));
                    depthRenderingProgram.setUniform("projection",
                        this.viewSet.getCameraProjection(this.viewSet.getCameraProjectionIndex(i))
                            .getProjectionMatrix(
                                this.viewSet.getRecommendedNearPlane(),
                                this.viewSet.getRecommendedFarPlane()
                            )
                    );

                    Matrix4 modelView = Matrix4.lookAt(
                            this.viewSet.getCameraPoseInverse(i).times(this.viewSet.getLightPosition(0).asPosition()).getXYZ(),
                            this.geometry.getCentroid(),
                            new Vector3(0, 1, 0));
                    depthRenderingProgram.setUniform("model_view", modelView);

                    Matrix4 projection = this.viewSet.getCameraProjection(this.viewSet.getCameraProjectionIndex(i))
                            .getProjectionMatrix(
                                this.viewSet.getRecommendedNearPlane(),
                                this.viewSet.getRecommendedFarPlane() * 2 // double it for good measure
                            );
                    depthRenderingProgram.setUniform("projection", projection);

                    depthDrawable.draw(PrimitiveMode.TRIANGLES, depthRenderingFBO);

                    Matrix4 fullTransform = projection.times(modelView);

                    int d = 0;
                    for (int col = 0; col < 4; col++) // column
                    {
                        for (int row = 0; row < 4; row++) // row
                        {
                            flattenedShadowMatrices.set(i, d, fullTransform.get(row, col));
                            d++;
                        }
                    }
                }

                // Create the uniform buffer
                shadowMatrixBuffer = context.createUniformBuffer().setData(flattenedShadowMatrices);
            }
        }
        else
        {
            shadowTextures = null;
            shadowMatrixBuffer = null;
        }
    }

    private static double getLinearDepth(double nonLinearDepth, double nearPlane, double farPlane)
    {
        return 2 * nearPlane * farPlane / (farPlane + nearPlane - nonLinearDepth * (farPlane - nearPlane));
    }

    private void setupCommon(Program<ContextType> program)
    {
        program.setTexture("eigentextures", this.eigentextures);
        program.setTexture("viewWeightTextures", this.colorTextures);
        program.setUniform("blockSize", new IntVector2(64, 64));
        program.setUniform("viewWeightPacking", new IntVector2(4, 4));


        program.setTexture("viewImages", this.colorTextures);
        program.setUniformBuffer("CameraWeights", this.cameraWeightBuffer);
        program.setUniformBuffer("CameraPoses", this.cameraPoseBuffer);
        program.setUniformBuffer("CameraProjections", this.cameraProjectionBuffer);
        program.setUniformBuffer("CameraProjectionIndices", this.cameraProjectionIndexBuffer);
        if (this.lightPositionBuffer != null && this.lightIntensityBuffer != null && this.lightIndexBuffer != null)
        {
            program.setUniformBuffer("LightPositions", this.lightPositionBuffer);
            program.setUniformBuffer("LightIntensities", this.lightIntensityBuffer);
            program.setUniformBuffer("LightIndices", this.lightIndexBuffer);
        }
        program.setUniform("viewCount", this.viewSet.getCameraPoseCount());

        program.setUniform("gamma", this.viewSet.getGamma());

        if (this.luminanceMap == null)
        {
            program.setUniform("useLuminanceMap", false);
            program.setTexture("luminanceMap", null);
        }
        else
        {
            program.setUniform("useLuminanceMap", true);
            program.setTexture("luminanceMap", this.luminanceMap);
        }

        if (this.inverseLuminanceMap == null)
        {
            program.setUniform("useInverseLuminanceMap", false);
            program.setTexture("inverseLuminanceMap", null);
        }
        else
        {
            program.setUniform("useInverseLuminanceMap", true);
            program.setTexture("inverseLuminanceMap", this.inverseLuminanceMap);
        }

        program.setUniform("infiniteLightSources", this.viewSet.areLightSourcesInfinite());

        if (this.depthTextures == null)
        {
            program.setUniform("occlusionEnabled", false);
        }
        else
        {
            program.setTexture("depthImages", this.depthTextures);
            program.setUniform("occlusionEnabled", true);
            program.setUniform("occlusionBias", 0.002f);
        }

        if (this.shadowMatrixBuffer == null || this.shadowTextures == null)
        {
            program.setUniform("shadowTestEnabled", false);
        }
        else
        {
            program.setUniform("shadowTestEnabled", true);
            program.setUniformBuffer("ShadowMatrices", this.shadowMatrixBuffer);
            program.setTexture("shadowImages", this.shadowTextures);
            program.setUniform("occlusionBias", 0.002f);
        }
    }

    public void setupShaderProgram(Program<ContextType> program, boolean enableTextures)
    {
        setupCommon(program);

        if (this.normalTexture == null)
        {
            program.setUniform("useNormalTexture", false);
            program.setTexture("normalMap", null);
        }
        else
        {
            program.setUniform("useNormalTexture", enableTextures);
            program.setTexture("normalMap", this.normalTexture);
        }

        if (this.diffuseTexture == null)
        {
            program.setUniform("useDiffuseTexture", false);
            program.setTexture("diffuseMap", null);
        }
        else
        {
            program.setUniform("useDiffuseTexture", enableTextures);
            program.setTexture("diffuseMap", this.diffuseTexture);
        }

        if (this.specularTexture == null)
        {
            program.setUniform("useSpecularTexture", false);
            program.setTexture("specularMap", null);
        }
        else
        {
            program.setUniform("useSpecularTexture", enableTextures);
            program.setTexture("specularMap", this.specularTexture);
        }

        if (this.roughnessTexture == null)
        {
            program.setUniform("useRoughnessTexture", false);
            program.setTexture("roughnessMap", null);
        }
        else
        {
            program.setUniform("useRoughnessTexture", enableTextures);
            program.setTexture("roughnessMap", this.roughnessTexture);
        }
    }

    public void setupShaderProgram(Program<ContextType> program, RenderingMode renderingMode)
    {
        setupCommon(program);

        if (this.normalTexture == null)
        {
            program.setUniform("useNormalTexture", false);
            program.setTexture("normalMap", null);
        }
        else
        {
            program.setUniform("useNormalTexture", renderingMode.useNormalTexture());
            program.setTexture("normalMap", this.normalTexture);
        }

        if (this.diffuseTexture == null)
        {
            program.setUniform("useDiffuseTexture", false);
            program.setTexture("diffuseMap", null);
        }
        else
        {
            program.setUniform("useDiffuseTexture", renderingMode.useDiffuseTexture());
            program.setTexture("diffuseMap", this.diffuseTexture);
        }

        if (this.specularTexture == null)
        {
            program.setUniform("useSpecularTexture", false);
            program.setTexture("specularMap", null);
        }
        else
        {
            program.setUniform("useSpecularTexture", renderingMode.useSpecularTextures());
            program.setTexture("specularMap", this.specularTexture);
        }

        if (this.roughnessTexture == null)
        {
            program.setUniform("useRoughnessTexture", false);
            program.setTexture("roughnessMap", null);
        }
        else
        {
            program.setUniform("useRoughnessTexture", renderingMode.useSpecularTextures());
            program.setTexture("roughnessMap", this.roughnessTexture);
        }
    }

    public double getPrimaryViewDistance()
    {
        return primaryViewDistance;
    }

    public float getCameraWeight(int index)
    {
        if (this.cameraWeights != null)
        {
            return this.cameraWeights[index];
        }
        else
        {
            throw new IllegalStateException("Camera weights are unavailable.");
        }
    }

    private static float[] computeCameraWeights(ViewSet viewSet, VertexGeometry geometry)
    {
        float[] cameraWeights = new float[viewSet.getCameraPoseCount()];
        
        Vector3[] viewDirections = IntStream.range(0, viewSet.getCameraPoseCount())
            .mapToObj(i -> viewSet.getCameraPoseInverse(i).getColumn(3).getXYZ()
                            .minus(geometry.getCentroid()).normalized())
            .toArray(Vector3[]::new);

        int[] totals = new int[viewSet.getCameraPoseCount()];
        int targetSampleCount = viewSet.getCameraPoseCount() * 256;
        double densityFactor = Math.sqrt(Math.PI * targetSampleCount);
        int sampleRows = (int)Math.ceil(densityFactor / 2) + 1;

        // Find the view with the greatest distance from any other view.
        // Directions that are further from any view than distance will be ignored in the view weight calculation.
        double maxMinDistance = 0.0;
        for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
        {
            double minDistance = Double.MAX_VALUE;
            for (int j = 0; j < viewSet.getCameraPoseCount(); j++)
            {
                if (i != j)
                {
                    minDistance = Math.min(minDistance, Math.acos(Math.max(-1.0, Math.min(1.0f, viewDirections[i].dot(viewDirections[j])))));
                }
            }
            maxMinDistance = Math.max(maxMinDistance, minDistance);
        }

        int actualSampleCount = 0;

        for (int i = 0; i < sampleRows; i++)
        {
            double r = Math.sin(Math.PI * (double)i / (double)(sampleRows-1));
            int sampleColumns = Math.max(1, (int)Math.ceil(densityFactor * r));

            for (int j = 0; j < sampleColumns; j++)
            {
                Vector3 sampleDirection = new Vector3(
                        (float)(r * Math.cos(2 * Math.PI * (double)j / (double)sampleColumns)),
                        (float) Math.cos(Math.PI * (double)i / (double)(sampleRows-1)),
                        (float)(r * Math.sin(2 * Math.PI * (double)j / (double)sampleColumns)));

                double minDistance = maxMinDistance;
                int minIndex = -1;
                for (int k = 0; k < viewSet.getCameraPoseCount(); k++)
                {
                    double distance = Math.acos(Math.max(-1.0, Math.min(1.0f, sampleDirection.dot(viewDirections[k]))));
                    if (distance < minDistance)
                    {
                        minDistance = distance;
                        minIndex = k;
                    }
                }

                if (minIndex >= 0)
                {
                    totals[minIndex]++;
                }

                actualSampleCount++;
            }
        }

        System.out.println("---");
        System.out.println("View weights:");

        for (int k = 0; k < viewSet.getCameraPoseCount(); k++)
        {
            cameraWeights[k] = (float)totals[k] / (float)actualSampleCount;
            System.out.println(viewSet.getImageFileName(k) + '\t' + cameraWeights[k]);
        }

        System.out.println("---");

        return cameraWeights;
    }

    @Override
    public void close()
    {
        if (this.cameraWeightBuffer != null)
        {
            this.cameraWeightBuffer.close();
        }

        if (this.cameraPoseBuffer != null)
        {
            this.cameraPoseBuffer.close();
        }

        if (this.cameraProjectionBuffer != null)
        {
            this.cameraProjectionBuffer.close();
        }

        if (this.cameraProjectionIndexBuffer != null)
        {
            this.cameraProjectionIndexBuffer.close();
        }

        if (this.lightPositionBuffer != null)
        {
            this.lightPositionBuffer.close();
        }

        if (this.lightIntensityBuffer != null)
        {
            this.lightIntensityBuffer.close();
        }

        if (this.lightIndexBuffer != null)
        {
            this.lightIndexBuffer.close();
        }

        if (this.positionBuffer != null)
        {
            this.positionBuffer.close();
        }

        if (this.texCoordBuffer != null)
        {
            this.texCoordBuffer.close();
        }

        if (this.normalBuffer != null)
        {
            this.normalBuffer.close();
        }

        if (this.colorTextures != null)
        {
            this.colorTextures.close();
        }

        if (this.eigentextures != null)
        {
            this.eigentextures.close();
        }

        if (depthTextures != null)
        {
            depthTextures.close();
        }

        if (diffuseTexture != null)
        {
            diffuseTexture.close();
        }

        if (normalTexture != null)
        {
            normalTexture.close();
        }

        if (specularTexture != null)
        {
            specularTexture.close();
        }

        if (roughnessTexture != null)
        {
            roughnessTexture.close();
        }

        if (shadowTextures != null)
        {
            shadowTextures.close();
        }

        if (shadowMatrixBuffer != null)
        {
            shadowMatrixBuffer.close();
        }
    }
}
