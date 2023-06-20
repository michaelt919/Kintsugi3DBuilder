/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.rendering.resources;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;

import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.FramebufferObjectBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.geometry.GeometryResources;
import tetzlaff.gl.geometry.VertexGeometry;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.ReadonlyLoadOptionsModel;
import tetzlaff.ibrelight.core.StandardRenderingMode;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.util.ColorList;

/**
 * A class that encapsulates all of the GPU resources like vertex buffers, uniform buffers, and textures for a given
 * IBR instance and provides helper methods for applying these resources in typical use cases.
 * @param <ContextType>
 */
public final class IBRResourcesImageSpace<ContextType extends Context<ContextType>> extends IBRResourcesBase<ContextType>
{
    /**
     * The geometry for this instance that the vertex buffers were loaded from.
     */
    public final VertexGeometry geometry;

    /**
     * A GPU buffer containing projection transformations defining the intrinsic properties of each camera.
     */
    public final UniformBuffer<ContextType> cameraProjectionBuffer;

    /**
     * A GPU buffer containing for every view an index designating the projection transformation that should be used for each view.
     */
    public final UniformBuffer<ContextType> cameraProjectionIndexBuffer;

    /**
     * A texture array instantiated on the GPU containing the image corresponding to each view in this dataset.
     */
    public final Texture3D<ContextType> colorTextures;

    /**
     * Contains the VBOs for positions, tex-coords, normals, and tangents
     */
    public final GeometryResources<ContextType> geometryResources;

    /**
     * A depth texture array containing a depth image for every view.
     */
    public final Texture3D<ContextType> depthTextures;

    /**
     * A depth texture array containing a shadow map for every view.
     */
    public final Texture3D<ContextType> shadowTextures;

    /**
     * A GPU buffer containing the matrices that were used for each shadow map in the shadowTextures array.
     */
    public final UniformBuffer<ContextType> shadowMatrixBuffer;

    private final double primaryViewDistance;

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
        private String primaryViewName;

        private Builder(ContextType context)
        {
            this.context = context;
        }

        public Builder<ContextType> setPrimaryView(String primaryViewName)
        {
            this.primaryViewName = primaryViewName;
            return this;
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
            this.geometry = VertexGeometry.createFromOBJFile(this.viewSet.getGeometryFile());
            return this;
        }

        // undistorted images are defined in the load options
        public Builder<ContextType> loadAgisoftFiles(File cameraFile, File geometryFile, File undistortedImageDirectory) throws FileNotFoundException, XMLStreamException
        {
            this.viewSet = ViewSet.loadFromAgisoftXMLFile(cameraFile);
            if (geometryFile != null)
            {
                this.geometry = VertexGeometry.createFromOBJFile(geometryFile);
            }
            if (undistortedImageDirectory != null)
            {
                this.imageDirectoryOverride = undistortedImageDirectory;
                this.viewSet.setRelativeImagePathName(cameraFile.getParentFile().toPath().relativize(undistortedImageDirectory.toPath()).toString());
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

        public IBRResourcesImageSpace<ContextType> create() throws IOException
        {
            if (linearLuminanceValues != null && encodedLuminanceValues != null)
            {
                viewSet.setTonemapping(gamma, linearLuminanceValues, encodedLuminanceValues);
            }

            if (imageDirectoryOverride != null)
            {
                viewSet.setRelativeImagePathName(viewSet.getRootDirectory().toPath().relativize(imageDirectoryOverride.toPath()).toString());
            }

            if (primaryViewName != null)
            {
                viewSet.setPrimaryView(primaryViewName);
            }

            if (geometry != null)
            {
                viewSet.setGeometryFileName(viewSet.getRootDirectory().toPath().relativize(geometry.getFilename().toPath()).toString());
            }
            else if (viewSet.getGeometryFile() != null)
            {
                // Load geometry if it wasn't specified but a view set was.
                geometry = VertexGeometry.createFromOBJFile(viewSet.getGeometryFile());
            }

            return new IBRResourcesImageSpace<>(context, viewSet, geometry, loadOptions, loadingMonitor);
        }
    }

    public static <ContextType extends Context<ContextType>> Builder<ContextType> getBuilderForContext(ContextType context)
    {
        return new Builder<>(context);
    }

    private IBRResourcesImageSpace(ContextType context, ViewSet viewSet, VertexGeometry geometry, ReadonlyLoadOptionsModel loadOptions, LoadingMonitor loadingMonitor) throws IOException
    {
        super(context, viewSet, geometry != null ? computeCameraWeights(viewSet, geometry) : null, geometry != null ? geometry.getMaterial() : null,
            loadOptions, loadingMonitor);

        this.geometry = geometry;

        // Read the images from a file
        if (loadOptions != null && loadOptions.areColorImagesRequested() && viewSet.getImageFilePath() != null && viewSet.getCameraPoseCount() > 0)
        {
            Date timestamp = new Date();

            File imageFile = viewSet.findPrimaryImageFile();

            // Read a single image to get the dimensions for the texture array
            BufferedImage img = null;
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
            loadOptions.configureColorTextureBuilder(textureArrayBuilder);
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
                imageFile = viewSet.findImageFile(i);

                this.colorTextures.loadLayer(i, imageFile, true);

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
        }

        if (loadingMonitor != null)
        {
            loadingMonitor.setMaximum(0.0);
        }

        // Store the camera projections in a uniform buffer
        if (viewSet != null && viewSet.getCameraProjectionData() != null)
        {
            // Create the uniform buffer
            cameraProjectionBuffer = context.createUniformBuffer().setData(viewSet.getCameraProjectionData());
        }
        else
        {
            cameraProjectionBuffer = null;
        }

        // Store the camera projection indices in a uniform buffer
        if (viewSet != null && viewSet.getCameraProjectionIndexData() != null)
        {
            cameraProjectionIndexBuffer = context.createUniformBuffer().setData(viewSet.getCameraProjectionIndexData());
        }
        else
        {
            cameraProjectionIndexBuffer = null;
        }

        if (geometry != null)
        {
            geometryResources = geometry.createGraphicsResources(context);

            if (viewSet != null && loadOptions.getDepthImageWidth() != 0 && loadOptions.getDepthImageHeight() != 0)
            {
                try
                (
                    // Don't automatically generate any texture attachments for this framebuffer object
                    FramebufferObject<ContextType> depthRenderingFBO =
                        context.buildFramebufferObject(loadOptions.getDepthImageWidth(), loadOptions.getDepthImageHeight())
                            .createFramebufferObject();

                    // Create a depth map generator -- includes the depth map program and drawable
                    DepthMapGenerator<ContextType> depthMapGenerator = DepthMapGenerator.createFromGeometryResources(geometryResources);
                )
                {
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
                            depthMapGenerator.generateDepthMap(viewSet, i, depthRenderingFBO);

                            if (i == viewSet.getPrimaryViewIndex())
                            {
                                minDepth = getMinDepthFromFBO(depthRenderingFBO, viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane());
                            }
                        }
                    }
                    else
                    {
                        this.depthTextures = null;

                        try (Texture2D<ContextType> depthAttachment = context.getTextureFactory()
                            .build2DDepthTexture(loadOptions.getDepthImageWidth(), loadOptions.getDepthImageHeight())
                            .createTexture())
                        {
                            depthRenderingFBO.setDepthAttachment(depthAttachment);
                            depthMapGenerator.generateDepthMap(viewSet, viewSet.getPrimaryViewIndex(), depthRenderingFBO);
                            minDepth = getMinDepthFromFBO(depthRenderingFBO, viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane());
                        }
                    }

                    primaryViewDistance = minDepth;
                }
            }
            else
            {
                this.depthTextures = null;
                primaryViewDistance = 0.0;
            }
        }
        else
        {
            this.geometryResources = GeometryResources.createNullResources();
            this.depthTextures = null;
            primaryViewDistance = 0.0;
        }

        if (this.depthTextures != null)
        {
            shadowTextures =
                context.getTextureFactory()
                    .build2DDepthTextureArray(this.depthTextures.getWidth(), this.depthTextures.getHeight(), this.getViewSet().getCameraPoseCount())
                    .createTexture();
            shadowMatrixBuffer = context.createUniformBuffer();

            updateShadowTextures();
        }
        else
        {
            shadowTextures = null;
            shadowMatrixBuffer = null;
        }
    }

    private static <ContextType extends Context<ContextType>> double getMinDepthFromFBO(Framebuffer<ContextType> depthFramebuffer, double nearPlane, double farPlane)
    {
        double minDepth = farPlane;

        short[] depthBufferData = depthFramebuffer.readDepthBuffer();
        for (short encodedDepth : depthBufferData)
        {
            int nonlinearDepth = 0xFFFF & (int) encodedDepth;
            minDepth = Math.min(minDepth, getLinearDepth((2.0 * nonlinearDepth) / 0xFFFF - 1.0, nearPlane, farPlane));
        }
        return minDepth;
    }

    static <ContextType extends Context<ContextType>> ProgramBuilder<ContextType> getDepthMapProgramBuilder(ContextType context)
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"));
    }

    /**
     * Gets a shader program builder with the following preprocessor defines automatically injected based on the
     * characteristics of this instance:
     * <ul>
     *     <li>CAMERA_POSE_COUNT</li>
     *     <li>LIGHT_COUNT</li>
     *     <li>CAMERA_PROJECTION_COUNT</li>
     *     <li>LUMINANCE_MAP_ENABLED</li>
     *     <li>INVERSE_LUMINANCE_MAP_ENABLED</li>
     *     <li>INFINITE_LIGHT_SOURCES</li>
     *     <li>VISIBILITY_TEST_ENABLED</li>
     *     <li>SHADOW_TEST_ENABLED</li>
     *     <li>IMAGE_BASED_RENDERING_ENABLED</li>
     *     <li>DIFFUSE_TEXTURE_ENABLED</li>
     *     <li>SPECULAR_TEXTURE_ENABLED</li>
     *     <li>ROUGHNESS_TEXTURE_ENABLED</li>
     *     <li>NORMAL_TEXTURE_ENABLED</li>
     * </ul>
     *
     * @param renderingMode The rendering mode to use, which may change some of the preprocessor defines.
     * @return A program builder with all of the above preprocessor defines specified, ready to have the
     * vertex and fragment shaders added as well as any additional application-specific preprocessor definitions.
     */
    @Override
    public ProgramBuilder<ContextType> getIBRShaderProgramBuilder(StandardRenderingMode renderingMode)
    {
        ProgramBuilder<ContextType> builder = getContext().getShaderProgramBuilder()
            .define("CAMERA_POSE_COUNT", this.getViewSet().getCameraPoseCount())
            .define("LIGHT_COUNT", this.getViewSet().getLightCount())
            .define("CAMERA_PROJECTION_COUNT", this.getViewSet().getCameraProjectionCount())
            .define("LUMINANCE_MAP_ENABLED", this.getLuminanceMapResources().getLuminanceMap() != null)
            .define("INVERSE_LUMINANCE_MAP_ENABLED", this.getLuminanceMapResources().getInverseLuminanceMap() != null)
            .define("INFINITE_LIGHT_SOURCES", this.getViewSet().areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", this.depthTextures != null)
            .define("SHADOW_TEST_ENABLED", this.shadowTextures != null)
            .define("IMAGE_BASED_RENDERING_ENABLED", renderingMode.isImageBased())
            .define("DIFFUSE_TEXTURE_ENABLED", this.getMaterialResources().getDiffuseTexture() != null && renderingMode.useDiffuseTexture())
            .define("SPECULAR_TEXTURE_ENABLED", this.getMaterialResources().getSpecularTexture() != null && renderingMode.useSpecularTextures())
            .define("ROUGHNESS_TEXTURE_ENABLED", this.getMaterialResources().getRoughnessTexture() != null && renderingMode.useSpecularTextures())
            .define("NORMAL_TEXTURE_ENABLED", this.getMaterialResources().getNormalTexture() != null && renderingMode.useNormalTexture());

        return builder;
    }

    /**
     * Gets a shader program builder with the following preprocessor defines automatically injected based on the
     * characteristics of this instance:
     * <ul>
     *     <li>CAMERA_POSE_COUNT</li>
     *     <li>LIGHT_COUNT</li>
     *     <li>CAMERA_PROJECTION_COUNT</li>
     *     <li>LUMINANCE_MAP_ENABLED</li>
     *     <li>INVERSE_LUMINANCE_MAP_ENABLED</li>
     *     <li>INFINITE_LIGHT_SOURCES</li>
     *     <li>VISIBILITY_TEST_ENABLED</li>
     *     <li>SHADOW_TEST_ENABLED</li>
     *     <li>IMAGE_BASED_RENDERING_ENABLED</li>
     *     <li>DIFFUSE_TEXTURE_ENABLED</li>
     *     <li>SPECULAR_TEXTURE_ENABLED</li>
     *     <li>ROUGHNESS_TEXTURE_ENABLED</li>
     *     <li>NORMAL_TEXTURE_ENABLED</li>
     * </ul>
     * This overload uses the default mode of RenderingMode.IMAGE_BASED.
     * @return A program builder with all of the above preprocessor defines specified, ready to have the
     * vertex and fragment shaders added as well as any additional application-specific preprocessor definitions.
     */
    @Override
    public ProgramBuilder<ContextType> getIBRShaderProgramBuilder()
    {
        return getIBRShaderProgramBuilder(StandardRenderingMode.IMAGE_BASED);
    }

    private void updateShadowTextures() throws FileNotFoundException
    {
        if (this.shadowTextures != null)
        {
            try
            (
                // Don't automatically generate any texture attachments for this framebuffer object
                FramebufferObject<ContextType> depthRenderingFBO =
                    getContext().buildFramebufferObject(this.shadowTextures.getWidth(), this.shadowTextures.getHeight())
                        .createFramebufferObject();

                // Load the program
                DepthMapGenerator<ContextType> depthMapGenerator = DepthMapGenerator.createFromGeometryResources(geometryResources)
            )
            {
                // Flatten the camera pose matrices into 16-component vectors and store them in the vertex list data structure.
                NativeVectorBuffer flattenedShadowMatrices = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 16, this.getViewSet().getCameraPoseCount());

                // Render each depth texture
                for (int i = 0; i < this.getViewSet().getCameraPoseCount(); i++)
                {
                    depthRenderingFBO.setDepthAttachment(shadowTextures.getLayerAsFramebufferAttachment(i));
                    Matrix4 shadowMatrix = depthMapGenerator.generateShadowMap(getViewSet(), i, depthRenderingFBO);

                    int d = 0;
                    for (int col = 0; col < 4; col++) // column
                    {
                        for (int row = 0; row < 4; row++) // row
                        {
                            flattenedShadowMatrices.set(i, d, shadowMatrix.get(row, col));
                            d++;
                        }
                    }
                }

                // Create the uniform buffer
                shadowMatrixBuffer.setData(flattenedShadowMatrices);
            }
        }
    }

    private static double getLinearDepth(double nonLinearDepth, double nearPlane, double farPlane)
    {
        return 2 * nearPlane * farPlane / (farPlane + nearPlane - nonLinearDepth * (farPlane - nearPlane));
    }

    /**
     * Refresh the light data in the uniform buffers using the current values in the view set.
     */
    public void updateLightData()
    {
        // Store the light positions in a uniform buffer
        if (lightPositionBuffer != null && getViewSet().getLightPositionData() != null)
        {
            // Create the uniform buffer
            lightPositionBuffer.setData(getViewSet().getLightPositionData());
        }

        // Store the light positions in a uniform buffer
        if (lightIntensityBuffer != null && getViewSet().getLightIntensityData() != null)
        {
            // Create the uniform buffer
            lightIntensityBuffer.setData(getViewSet().getLightIntensityData());
        }

        try
        {
            updateShadowTextures();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    private void setupCommon(Program<ContextType> program)
    {
        if (this.colorTextures != null)
        {
            program.setTexture("viewImages", this.colorTextures);
        }

        if (this.cameraWeightBuffer != null)
        {
            program.setUniformBuffer("CameraWeights", this.cameraWeightBuffer);
        }

        if (this.cameraPoseBuffer != null)
        {
            program.setUniformBuffer("CameraPoses", this.cameraPoseBuffer);
        }

        if (this.cameraProjectionBuffer != null && this.cameraProjectionIndexBuffer != null)
        {
            program.setUniformBuffer("CameraProjections", this.cameraProjectionBuffer);
            program.setUniformBuffer("CameraProjectionIndices", this.cameraProjectionIndexBuffer);
        }

        if (this.lightPositionBuffer != null && this.lightIntensityBuffer != null && this.lightIndexBuffer != null)
        {
            program.setUniformBuffer("LightPositions", this.lightPositionBuffer);
            program.setUniformBuffer("LightIntensities", this.lightIntensityBuffer);
            program.setUniformBuffer("LightIndices", this.lightIndexBuffer);
        }

        program.setUniform("gamma", this.getViewSet().getGamma());

        if (this.depthTextures == null)
        {
            program.setTexture("depthImages", getContext().getTextureFactory().getNullTexture(SamplerType.FLOAT_2D_ARRAY));
        }
        else
        {
            program.setTexture("depthImages", this.depthTextures);
            program.setUniform("occlusionBias", 0.002f);
        }

        if (this.shadowMatrixBuffer == null || this.shadowTextures == null)
        {
            program.setTexture("shadowImages", getContext().getTextureFactory().getNullTexture(SamplerType.FLOAT_2D_ARRAY));
        }
        else
        {
            program.setUniformBuffer("ShadowMatrices", this.shadowMatrixBuffer);
            program.setTexture("shadowImages", this.shadowTextures);
            program.setUniform("occlusionBias", 0.002f);
        }
    }

    /**
     * Sets up a shader program to use this instance's IBR resources.
     * While the geometry is generally associated with a Drawable using the createDrawable function,
     * this method binds all of the textures and associated data like camera poses, light positions, etc.
     * to the shader program's uniform variables.
     * @param program The shader program to set up using this instance's resources.
     */
    @Override
    public void setupShaderProgram(Program<ContextType> program)
    {
        setupCommon(program);
        getLuminanceMapResources().setupShaderProgram(program);
        getMaterialResources().setupShaderProgram(program);
    }

    /**
     * Creates a Drawable using this instance's geometry resources, and the specified shader program.
     * @param program The program to use to construct the Drawable.
     * @return A Drawable for rendering this instance using the specified shader program.
     */
    @Override
    public Drawable<ContextType> createDrawable(Program<ContextType> program)
    {
        return geometryResources.createDrawable(program);
    }

    /**
     * Returns a sequential stream with the views in this IBR instance as its source.
     * A shader program and a framebuffer (which may contain multiple attachments) must be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param drawable A drawable (typically obtained using IBRResources.createDrawable)
     *                 that contains the shader program to be invoked on each view in this instance.
     * @param framebuffer The GPU framebuffer which will store the result of invoking the specified drawable.
     * @param attachmentCount The number of attachments that the framebuffer contains.
     * @return a sequential Stream over the views in this instance.
     */
    @Override
    public GraphicsStream<ColorList[]> stream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount)
    {
        return new SequentialViewRenderStream<>(getViewSet().getCameraPoseCount(), drawable, framebuffer, attachmentCount);
    }

    /**
     * Returns a sequential stream with the views in this IBR instance as its source.
     * A shader program and a framebuffer (with a single attachment for this overload) must be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param drawable A drawable (typically obtained using IBRResources.createDrawable)
     *                 that contains the shader program to be invoked on each view in this instance.
     * @param framebuffer The GPU framebuffer which will store the result of invoking the specified drawable.
     * @return a sequential Stream over the views in this instance.
     */
    @Override
    public GraphicsStream<ColorList> stream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        return new SequentialViewRenderStream<>(getViewSet().getCameraPoseCount(), drawable, framebuffer, 1)
            .map(singletonList -> singletonList[0]);
    }

    /**
     * Returns a sequential stream with the views in this IBR instance as its source.
     * Unlike stream(), this function manages the allocation of the shader program and the framebuffer object
     * and returns an AutoCloseable so that they may be automatically deallocated using a try-with-resources block.
     * Builders for the shader program and framebuffer object must still be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param programBuilder A builder for the shader program to be invoked on each view in this instance.
     * @param framebufferBuilder A builder for the GPU framebuffer which will store the result of invoking the
     *                           specified drawable.
     * @return a sequential Stream over the views in this instance,
     * with a dual function as an AutoCloseable that manages the associated GPU resources.
     * @throws FileNotFoundException if the shader program files cannot be found.
     */
    @Override
    public GraphicsStreamResource<ContextType> streamAsResource(
        ProgramBuilder<ContextType> programBuilder,
        FramebufferObjectBuilder<ContextType> framebufferBuilder) throws FileNotFoundException
    {
        return new GraphicsStreamResource<>(programBuilder, framebufferBuilder,
            (program, framebuffer) -> new SequentialViewRenderStream<>(
                getViewSet().getCameraPoseCount(), createDrawable(program), framebuffer, framebuffer.getColorAttachmentCount()));
    }

    /**
     * Returns a parallel stream with the views in this IBR instance as its source.
     * A shader program and a framebuffer (which may contain multiple attachments) must be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param drawable A drawable (typically obtained using IBRResources.createDrawable)
     *                 that contains the shader program to be invoked on each view in this instance.
     * @param framebuffer The GPU framebuffer which will store the result of invoking the specified drawable.
     * @param attachmentCount The number of attachments that the framebuffer contains.
     * @param maxRunningThreads The maximum number of threads allowed to be running at once.  The fact that one thread
     *                          will be dedicated to GPU rendering should be considered when specifying this parameter.
     * @return a parallel Stream over the views in this instance.
     */
    @Override
    public GraphicsStream<ColorList[]> parallelStream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount, int maxRunningThreads)
    {
        return new ParallelViewRenderStream<>(getViewSet().getCameraPoseCount(), drawable, framebuffer, attachmentCount, maxRunningThreads);
    }

    /**
     * Returns a parallel stream with the views in this IBR instance as its source,
     * with a default limit for the number of threads running at once.
     * A shader program and a framebuffer (which may contain multiple attachments) must be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param drawable A drawable (typically obtained using IBRResources.createDrawable)
     *                 that contains the shader program to be invoked on each view in this instance.
     * @param framebuffer The GPU framebuffer which will store the result of invoking the specified drawable.
     * @param attachmentCount The number of attachments that the framebuffer contains.
     * @return a parallel Stream over the views in this instance.
     */
    @Override
    public GraphicsStream<ColorList[]> parallelStream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount)
    {
        return new ParallelViewRenderStream<>(getViewSet().getCameraPoseCount(), drawable, framebuffer, attachmentCount);
    }

    /**
     * Returns a parallel stream with the views in this IBR instance as its source.
     * A shader program and a framebuffer (with a single attachment for this overload) must be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param drawable A drawable (typically obtained using IBRResources.createDrawable)
     *                 that contains the shader program to be invoked on each view in this instance.
     * @param framebuffer The GPU framebuffer which will store the result of invoking the specified drawable.
     * @return a parallel Stream over the views in this instance.
     */
    @Override
    public GraphicsStream<ColorList> parallelStream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        return new ParallelViewRenderStream<>(getViewSet().getCameraPoseCount(), drawable, framebuffer, 1)
            .map(singletonList -> singletonList[0]);
    }

    /**
     * Returns a parallel stream with the views in this IBR instance as its source.
     * Unlike parallelStream(), this function manages the allocation of the shader program and the framebuffer object
     * and returns an AutoCloseable so that they may be automatically deallocated using a try-with-resources block.
     * Builders for the shader program and framebuffer object must still be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param programBuilder A builder for the shader program to be invoked on each view in this instance.
     * @param framebufferBuilder A builder for the GPU framebuffer which will store the result of invoking the
     *                           specified drawable.
     * @param maxRunningThreads The maximum number of threads allowed to be running at once.  The fact that one thread
     *                          will be dedicated to GPU rendering should be considered when specifying this parameter.
     * @return a parallel Stream over the views in this instance,
     * with a dual function as an AutoCloseable that manages the associated GPU resources.
     * @throws FileNotFoundException if the shader program files cannot be found.
     */
    @Override
    public GraphicsStreamResource<ContextType> parallelStreamAsResource(
        ProgramBuilder<ContextType> programBuilder,
        FramebufferObjectBuilder<ContextType> framebufferBuilder,
        int maxRunningThreads) throws FileNotFoundException
    {
        return new GraphicsStreamResource<>(programBuilder, framebufferBuilder,
            (program, framebuffer) -> new ParallelViewRenderStream<>(
                getViewSet().getCameraPoseCount(), createDrawable(program), framebuffer, framebuffer.getColorAttachmentCount(), maxRunningThreads));
    }

    /**
     * Returns a parallel stream with the views in this IBR instance as its source,
     * with a default limit for the number of threads running at once.
     * Unlike parallelStream(), this function manages the allocation of the shader program and the framebuffer object
     * and returns an AutoCloseable so that they may be automatically deallocated using a try-with-resources block.
     * Builders for the shader program and framebuffer object must still be specified in order to map
     * the views (which live only on the GPU) to a data buffer that can be examined on the CPU.
     * @param programBuilder A builder for the shader program to be invoked on each view in this instance.
     * @param framebufferBuilder A builder for the GPU framebuffer which will store the result of invoking the
     *                           specified drawable.
     * @return a parallel Stream over the views in this instance,
     * with a dual function as an AutoCloseable that manages the associated GPU resources.
     * @throws FileNotFoundException if the shader program files cannot be found.
     */
    @Override
    public GraphicsStreamResource<ContextType> parallelStreamAsResource(
        ProgramBuilder<ContextType> programBuilder,
        FramebufferObjectBuilder<ContextType> framebufferBuilder) throws FileNotFoundException
    {
        return new GraphicsStreamResource<>(programBuilder, framebufferBuilder,
            (program, framebuffer) -> new ParallelViewRenderStream<>(
                getViewSet().getCameraPoseCount(), createDrawable(program), framebuffer, framebuffer.getColorAttachmentCount()));
    }

    /**
     * Gets the distance from the camera to the centroid in the primary view.
     * This is frequently used to calibrate scale in IBRelight.
     * @return The camera distance in the primary view.
     */
    public double getPrimaryViewDistance()
    {
        return primaryViewDistance;
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

    /**
     * Creates a resource for just a single view, using the default image for that view but with custom load options
     * @param viewIndex
     * @param loadOptions
     * @return
     * @throws IOException
     */
    public SingleCalibratedImageResource<ContextType> createSingleImageResource(int viewIndex, ReadonlyLoadOptionsModel loadOptions)
        throws IOException
    {
        return createSingleImageResource(viewIndex, getViewSet().getImageFile(viewIndex), loadOptions);
    }

    /**
     * Creates a resource for just a single view, using a specified image file with custom load options
     * @param viewIndex
     * @param imageFile
     * @param loadOptions
     * @return
     * @throws IOException
     */
    public SingleCalibratedImageResource<ContextType> createSingleImageResource(int viewIndex, File imageFile, ReadonlyLoadOptionsModel loadOptions)
        throws IOException
    {
        return new SingleCalibratedImageResource<>(getContext(), getViewSet(), viewIndex, imageFile, geometry, loadOptions);
    }

    public ImageCache<ContextType> cache(ImageCacheSettings settings) throws IOException
    {
        ImageCache<ContextType> cache = new ImageCache<>(this, settings);
        cache.initialize(/* TODO: implement high-res image directory */ getViewSet().getImageFilePath());
        return cache;
    }

    @Override
    public void close()
    {
        super.close();

        if (this.cameraProjectionBuffer != null)
        {
            this.cameraProjectionBuffer.close();
        }

        if (this.cameraProjectionIndexBuffer != null)
        {
            this.cameraProjectionIndexBuffer.close();
        }

        if (this.geometryResources != null)
        {
            this.geometryResources.close();
        }

        if (this.colorTextures != null)
        {
            this.colorTextures.close();
        }

        if (this.depthTextures != null)
        {
            this.depthTextures.close();
        }

        if (this.shadowTextures != null)
        {
            this.shadowTextures.close();
        }

        if (this.shadowMatrixBuffer != null)
        {
            this.shadowMatrixBuffer.close();
        }
    }
}
