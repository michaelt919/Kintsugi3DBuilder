/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.resources.ibr;

import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.*;
import kintsugi3d.builder.io.*;
import kintsugi3d.builder.io.metashape.MetashapeModel;
import kintsugi3d.gl.builders.ColorTextureBuilder;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.geometry.GeometryMode;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.interactive.GraphicsRequest;
import kintsugi3d.gl.material.TextureLoadOptions;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.ImageFinder;
import kintsugi3d.util.ImageHelper;
import kintsugi3d.util.ImageUndistorter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

/**
 * A class that encapsulates all of the GPU resources like vertex buffers, uniform buffers, and textures for a given
 * IBR instance and provides helper methods for applying these resources in typical use cases.
 * @param <ContextType>
 */
public final class IBRResourcesImageSpace<ContextType extends Context<ContextType>> extends IBRResourcesBase<ContextType>
    implements IBRResourcesCacheable<ContextType>
{
    private static final boolean MULTITHREAD_PREVIEW_IMAGE_GENERATION = false;

    private static final Logger log = LoggerFactory.getLogger(IBRResourcesImageSpace.class);
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
        private ReadonlyLoadOptionsModel imageLoadOptions;
        private ProgressMonitor progressMonitor;
        private double[] linearLuminanceValues;
        private byte[] encodedLuminanceValues;
        private String primaryViewName;
        private String orientationViewName;

        private Builder(ContextType context)
        {
            this.context = context;
        }

        private void updateViewSetFromImageLoadOptions()
        {
            if (this.imageLoadOptions != null)
            {
                this.viewSet.setPreviewImageResolution(imageLoadOptions.getPreviewImageWidth(), imageLoadOptions.getPreviewImageHeight());
                String directoryName = String.format("%s/_%dx%d", viewSet.getUUID().toString(), imageLoadOptions.getPreviewImageWidth(), imageLoadOptions.getPreviewImageHeight());

                this.viewSet.setRelativePreviewImagePathName(new File(ApplicationFolders.getPreviewImagesRootDirectory().toFile(), directoryName).toString());
            }
        }

        public Builder<ContextType> setPrimaryView(String primaryViewName)
        {
            this.primaryViewName = primaryViewName;
            return this;
        }

        public Builder<ContextType> setOrientationView(String orientationViewName, double rotation)
        {
            this.orientationViewName = orientationViewName;

            if (orientationViewName == null)
            {
                this.viewSet.setOrientationViewIndex(-1);
            }

            this.viewSet.setOrientationViewRotationDegrees(rotation);
            return this;
        }

        public Builder<ContextType> setImageLoadOptions(ReadonlyLoadOptionsModel imageLoadOptions)
        {
            this.imageLoadOptions = imageLoadOptions;

            if (this.viewSet != null)
            {
                updateViewSetFromImageLoadOptions();
            }

            return this;
        }

        public Builder<ContextType> setProgressMonitor(ProgressMonitor progressMonitor)
        {
            this.progressMonitor = progressMonitor;
            return this;
        }

        public Builder<ContextType> setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
        {
            this.linearLuminanceValues = Arrays.copyOf(linearLuminanceValues, linearLuminanceValues.length);
            this.encodedLuminanceValues = Arrays.copyOf(encodedLuminanceValues, encodedLuminanceValues.length);
            return this;
        }

        public Builder<ContextType> loadVSETFile(File vsetFile, File supportingFilesDirectory) throws Exception
        {
            this.viewSet = ViewSetReaderFromVSET.getInstance().readFromFile(vsetFile, supportingFilesDirectory).finish();
            updateViewSetFromImageLoadOptions();
            loadAndValidateGeometry();
            return this;
        }

        private static ViewSetReader getReaderForFile(File cameraFile)
        {
            if (cameraFile.getName().endsWith(".xml")) // Agisoft Metashape
            {
                return ViewSetReaderFromAgisoftXML.getInstance();
            }
            else if (cameraFile.getName().endsWith(".csv")) // RealityCapture
            {
                return ViewSetReaderFromRealityCaptureCSV.getInstance();
            }
            else
            {
                throw new IllegalArgumentException(MessageFormat.format("Unrecognized file extension for camera calibration: {0}", cameraFile));
            }
        }

        // images are defined in the load options
        public Builder<ContextType> loadLooseFiles(File cameraFile, ViewSetLoadOptions viewSetLoadOptions) throws Exception
        {
            return loadLooseFiles(getReaderForFile(cameraFile), cameraFile, viewSetLoadOptions);
        }

        // images are defined in the load options
        public Builder<ContextType> loadLooseFiles(ViewSetReader reader, File cameraFile, ViewSetLoadOptions viewSetLoadOptions) throws Exception
        {
            // Load view set
            this.viewSet = reader.readFromFile(cameraFile, viewSetLoadOptions)
                .finish();

            updateViewSetFromImageLoadOptions();
            loadAndValidateGeometry();
            return this;
        }

        /**
         *
         * @param model
         * @param supportingFilesDirectory
         * @return
         * @throws IOException
         */
        public Builder<ContextType> loadFromMetashapeModel(MetashapeModel model)
            throws IOException, XMLStreamException
        {
            this.viewSet = ViewSetReaderFromAgisoftXML.loadViewsetFromChunk(model.getChunk()).finish();
            updateViewSetFromImageLoadOptions();
            loadAndValidateGeometry();
            return this;
        }

        private void loadAndValidateGeometry() throws IOException
        {
            if (viewSet.getGeometryFile() != null)
            {
                this.geometry = VertexGeometry.createFromGeometryFile(viewSet.getGeometryFile());

                if (this.geometry == null)
                {
                    throw new IllegalArgumentException(MessageFormat.format("Unsupported geometry file: {0}", viewSet.getGeometryFile()));
                }

                if (!this.geometry.hasNormals())
                {
                    throw new MeshImportException("Imported Object has no Normals");
                }

                if (!this.geometry.hasTexCoords())
                {
                    throw new MeshImportException("Imported Object has no Texture Coordinates");
                }
            }
            else
            {
                throw new IllegalArgumentException("Geometry file may not be null.");
            }
        }

        public ViewSet getViewSet()
        {
            return viewSet;
        }

        public Builder<ContextType> useExistingViewSet(ViewSet existingViewSet)
        {
            this.viewSet = existingViewSet;

            if (geometry != null)
            {
                viewSet.setGeometryFile(geometry.getFilename());
            }

            return this;
        }

        public Builder<ContextType> useExistingGeometry(VertexGeometry existingGeometry)
        {
            this.geometry = existingGeometry;

            if (viewSet != null)
            {
                viewSet.setGeometryFile(geometry.getFilename());
            }

            return this;
        }

        public Builder<ContextType> overrideImageDirectory(File imageDirectory)
        {
            this.imageDirectoryOverride = imageDirectory;
            return this;
        }

        public Builder<ContextType> generateUndistortedPreviewImages() throws IOException, UserCancellationException
        {
            if (this.viewSet != null)
            {
                IBRResourcesImageSpace.generateUndistortedPreviewImages(
                    this.viewSet, this.imageLoadOptions.getMaxLoadingThreads(), this.progressMonitor);
            }

            return this;
        }

        public Builder<ContextType> generateThumbnailImages() throws IOException, UserCancellationException
        {
            if (this.viewSet != null)
            {
                IBRResourcesImageSpace.generateThumbnailImages(
                        this.viewSet, this.loadOptions.getMaxLoadingThreads(), this.progressMonitor
                );
            }

            return this;
        }

        public IBRResourcesImageSpace<ContextType> create() throws IOException, UserCancellationException
        {
            if (linearLuminanceValues != null && encodedLuminanceValues != null)
            {
                viewSet.setTonemapping(linearLuminanceValues, encodedLuminanceValues);
            }

            if (imageDirectoryOverride != null)
            {
                viewSet.setFullResImageDirectory(imageDirectoryOverride);
            }

            if (primaryViewName != null)
            {
                viewSet.setPrimaryView(primaryViewName);
            }

            if (orientationViewName != null)
            {
                viewSet.setOrientationView(orientationViewName);
            }

            if (geometry == null && viewSet.getGeometryFile() != null)
            {
                // Load geometry if it wasn't specified but a view set was.
                geometry = VertexGeometry.createFromGeometryFile(viewSet.getGeometryFile());
            }

            return new IBRResourcesImageSpace<>(context, viewSet, geometry, imageLoadOptions, progressMonitor);
        }
    }

    public static <ContextType extends Context<ContextType>> Builder<ContextType> getBuilderForContext(ContextType context)
    {
        return new Builder<>(context);
    }

    private IBRResourcesImageSpace(ContextType context, ViewSet viewSet, VertexGeometry geometry,
        ReadonlyLoadOptionsModel loadOptions, ProgressMonitor progressMonitor) throws IOException, UserCancellationException
    {
        // IAN: This super call should be creating the geometry
        super(new IBRSharedResources<>(context, viewSet, geometry,
                    loadOptions != null ? loadOptions.getTextureLoadOptions() : new TextureLoadOptions()),
                true);

        // Read the images from a file
        if (loadOptions != null && loadOptions.areColorImagesRequested() && viewSet.getFullResImageFilePath() != null && viewSet.getCameraPoseCount() > 0)
        {
            Date timestamp = new Date();

            int width;
            int height;

            // Use preview-resolution images for the texture array due to VRAM limitations
            try
            {
                File imageFile = viewSet.findPreviewPrimaryImageFile();

                // Read a single image to get the dimensions for the texture array
                BufferedImage img;
                try (InputStream input = new FileInputStream(imageFile)) // myZip.retrieveFile(imageFile);
                {
                    img = ImageIO.read(input);
                }

                if (img == null)
                {
                    throw new IOException(String.format("Error: Unsupported image format '%s'.",
                        viewSet.getImageFileName(0)));
                }

                width = img.getWidth();
                height = img.getHeight();
            }
            catch (FileNotFoundException e)
            {
                // Need to regenerate preview-resolution images
                width = loadOptions.getPreviewImageWidth();
                height = loadOptions.getPreviewImageHeight();
            }

            ColorTextureBuilder<ContextType, ? extends Texture3D<ContextType>> textureArrayBuilder =
                    context.getTextureFactory().build2DColorTextureArray(width, height, viewSet.getCameraPoseCount());
            loadOptions.configureColorTextureBuilder(textureArrayBuilder);
            colorTextures = textureArrayBuilder.createTexture();

            if(progressMonitor != null)
            {
                progressMonitor.setStage(1, "Loading preview-resolution images...");
                progressMonitor.setMaxProgress(viewSet.getCameraPoseCount());
            }

            int m = viewSet.getCameraPoseCount();
            for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
            {
                if (progressMonitor != null)
                {
                    progressMonitor.setProgress(i, MessageFormat.format("{0} ({1}/{2})", viewSet.getImageFileName(i), i + 1, viewSet.getCameraPoseCount()));
                    progressMonitor.allowUserCancellation();
                }

                try
                {
                    File imageFile = findOrGeneratePreviewImageFile(i);

                    this.colorTextures.loadLayer(i, imageFile, true);
                }
                catch (FileNotFoundException e)
                {
                    // If the file is not found, continue and try to load other images.
                    log.error("Failed to load image.", e);
                }
            }

            if (progressMonitor != null)
            {
                progressMonitor.setProgress(viewSet.getCameraPoseCount(), "All images loaded.");
            }

            log.info("View Set textures loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
        }
        else
        {
            this.colorTextures = null;
        }

        if (progressMonitor != null)
        {
            progressMonitor.setStage(2, "Finished loading images.");
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

        if (getGeometryResources() != null)
        {
            if (viewSet != null && loadOptions != null && loadOptions.getDepthImageWidth() != 0 && loadOptions.getDepthImageHeight() != 0)
            {
                try
                (
                    // Don't automatically generate any texture attachments for this framebuffer object
                    FramebufferObject<ContextType> depthRenderingFBO =
                        context.buildFramebufferObject(loadOptions.getDepthImageWidth(), loadOptions.getDepthImageHeight())
                            .createFramebufferObject();

                    // Create a depth map generator -- includes the depth map program and drawable
                    DepthMapGenerator<ContextType> depthMapGenerator =
                        DepthMapGenerator.createFromGeometryResources(getGeometryResources())
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

        short[] depthBufferData = depthFramebuffer.getTextureReaderForDepthAttachment().read();
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


    private void updateShadowTextures() throws IOException
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
                DepthMapGenerator<ContextType> depthMapGenerator = DepthMapGenerator.createFromGeometryResources(getGeometryResources())
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
     * Refresh the light data in the uniform buffers using the current values in the view set,
     * and also update the shadow textures.
     */
    @Override
    public void updateLightCalibration(Vector3 lightCalibration)
    {
        super.updateLightCalibration(lightCalibration);

        try
        {
            updateShadowTextures();
        }
        catch (IOException e)
        {
            log.error("Error updating light calibration:", e);
        }
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
     * @return A program builder with all of the above preprocessor defines specified, ready to have the
     * vertex and fragment shaders added as well as any additional application-specific preprocessor definitions.
     */
    @Override
    public ProgramBuilder<ContextType> getShaderProgramBuilder()
    {
        return getSharedResources().getShaderProgramBuilder()
            .define("GEOMETRY_MODE", GeometryMode.PROJECT_3D_TO_2D) // should default to this, but just in case
            .define("GEOMETRY_TEXTURES_ENABLED", false) // should default to this, but just in case
            .define("COLOR_APPEARANCE_MODE", ColorAppearanceMode.IMAGE_SPACE) // should default to this, but just in case
            .define("CAMERA_PROJECTION_COUNT", getViewSet().getCameraProjectionCount())
            .define("VISIBILITY_TEST_ENABLED", this.depthTextures != null)
            .define("SHADOW_TEST_ENABLED", this.shadowTextures != null);
    }

    @Override
    public void setupShaderProgram(Program<ContextType> program)
    {
        getSharedResources().setupShaderProgram(program);

        if (this.colorTextures != null)
        {
            program.setTexture("viewImages", this.colorTextures);
        }

        if (this.cameraProjectionBuffer != null && this.cameraProjectionIndexBuffer != null)
        {
            program.setUniformBuffer("CameraProjections", this.cameraProjectionBuffer);
            program.setUniformBuffer("CameraProjectionIndices", this.cameraProjectionIndexBuffer);
        }

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

    @Override
    public Drawable<ContextType> createDrawable(Program<ContextType> program)
    {
        return getGeometryResources().createDrawable(program);
    }

    /**
     * Gets the distance from the camera to the centroid in the primary view.
     * This is frequently used to calibrate scale in Kintsugi 3D Builder.
     * @return The camera distance in the primary view.
     */
    public double getPrimaryViewDistance()
    {
        return primaryViewDistance;
    }

    public void calibrateLightIntensities(boolean infiniteLightSources)
    {
        if (primaryViewDistance > 0)
        {
            Vector3 lightIntensity = new Vector3((float) (primaryViewDistance * primaryViewDistance));
            initializeLightIntensities(lightIntensity, infiniteLightSources);
        }
        else
        {
            log.warn("Light intensities not calibrated; primaryViewDistance was zero (were depth images generated first?)");
        }
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
        return createSingleImageResource(viewIndex, getViewSet().findFullResImageFile(viewIndex), loadOptions);
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
        return new SingleCalibratedImageResource<>(getContext(), getViewSet(), viewIndex, imageFile, getGeometry(), loadOptions);
    }

    @Override
    public ImageCache<ContextType> cache(ImageCacheSettings settings, ProgressMonitor monitor) throws IOException, UserCancellationException
    {
        settings.setCacheFolderName(getViewSet().getUUID().toString());

        ImageCache<ContextType> cache = new ImageCache<>(this, settings);

        if (!cache.isInitialized())
        {
            cache.initialize(monitor);
        }

        return cache;
    }

    private static BufferedImage getDecodedImage(ViewSet viewSet, int i) throws IOException
    {
        // Read the image (and do ICC processing, if applicable) on a worker thread
        File fullResImageFile = viewSet.findFullResImageFile(i);
        log.info("Decoding {}", fullResImageFile);
        return ImageIO.read(fullResImageFile);
        // TODO ICC transformation?
    }

    private static BufferedImage getDecodedMaskImage(ViewSet viewSet, int i) throws IOException {
        // Read the image (and do ICC processing, if applicable) on a worker thread
        File maskFile = viewSet.getMask(i);
        if (maskFile == null){
            return null;
        }
        log.info("Decoding {}", maskFile);
        return ImageIO.read(maskFile);
    }

    private static <ContextType extends Context<ContextType>> BufferedImage undistortImage(
        BufferedImage distortedImage,BufferedImage distortedMaskImage, boolean mipmapsEnabled, ViewSet viewSet, int projectionIndex, ContextType context)
        throws IOException
    {
        DistortionProjection distortion = (DistortionProjection) viewSet.getCameraProjection(projectionIndex);
        distortion = distortion.scaledTo(viewSet.getPreviewWidth(), viewSet.getPreviewHeight());

        try (ImageUndistorter<?> undistort = new ImageUndistorter<>(context))
        {
            return undistort.undistort(distortedImage,distortedMaskImage, mipmapsEnabled, distortion);
        }
    }

    // Overload primarily for manually setting the resolution of thumbnail images.
    private static <ContextType extends Context<ContextType>> BufferedImage undistortThumbnailImage(
            BufferedImage distortedImage, ViewSet viewSet, int projectionIndex, ContextType context)
            throws IOException
    {
        DistortionProjection distortion = (DistortionProjection) viewSet.getCameraProjection(projectionIndex);
        distortion = distortion.scaledTo(viewSet.getThumbnailWidth(),viewSet.getThumbnailHeight());

        try (ImageUndistorter<?> undistort = new ImageUndistorter<>(context))
        {
            return undistort.undistort(distortedImage, distortion);
        }
    }


    private static void throwUndistortFailed(ProgressMonitor progressMonitor, AtomicInteger failedCount) throws IOException
    {
        IOException e = new IOException("Failed to undistort " + failedCount.get() + " images");
        progressMonitor.warn(e);

        // Generating preview images partially failed, but we'll try to load the rest of the project.
        // Go back to indeterminate progress until it starts to actually load for rendering
        progressMonitor.setMaxProgress(0.0);

        throw e;
    }

    private static void resizeImage(File fullResImageFile, ViewSet viewSet, int i) throws IOException
    {
        log.info("Resizing image {} : No distortion parameters", fullResImageFile);
        ImageHelper resizer = new ImageHelper(fullResImageFile);
        resizer.saveAtResolution(viewSet.getPreviewImageFile(i),
            viewSet.getPreviewWidth(), viewSet.getPreviewHeight());
    }

    private static void resizeImageWithMask(File fullResImageFile, File mask, ViewSet viewSet, int i) throws IOException {
        log.info("Resizing image {} with mask {}: No distortion parameters", fullResImageFile, mask);
        //TODO: add checks to verify mask and img are the same size?
        ImageHelper imgResizer = new ImageHelper(fullResImageFile);
        ImageHelper maskResizer = new ImageHelper(mask);

        int w = viewSet.getPreviewWidth();
        int h = viewSet.getPreviewHeight();
        BufferedImage resizedImg = imgResizer.scaleToResolution(w, h);
        BufferedImage resizedMask = maskResizer.scaleToResolution(w, h);

        ImageHelper maskedImg = new ImageHelper(applyGrayscaleMaskToAlpha(resizedImg, resizedMask));
        maskedImg.saveAtResolution(viewSet.getPreviewImageFile(i), w, h);
    }

    //    https://stackoverflow.com/questions/221830/set-bufferedimage-alpha-mask-in-java
    public static BufferedImage applyGrayscaleMaskToAlpha(BufferedImage image, BufferedImage mask)
    {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage result = new BufferedImage(width, height, image.getType());

        int[] imagePixels = image.getRGB(0, 0, width, height, null, 0, width);
        int[] maskPixels = mask.getRGB(0, 0, width, height, null, 0, width);

        int[] resultPixels = result.getRGB(0, 0, width, height, null, 0, width);

        for (int i = 0; i < imagePixels.length; i++)
        {
            int color = imagePixels[i] & 0x00ffffff; // Mask preexisting alpha
            int alpha = maskPixels[i] << 24; // Shift blue to alpha
            resultPixels[i] = color | alpha;
        }

        result.setRGB(0, 0, width, height, resultPixels, 0, width);
        return result;
    }

    private static void resizeThumbnailImage(File fullResImageFile, ViewSet viewSet, int i) throws IOException
    {
        log.info("Resizing image {} : No distortion parameters", fullResImageFile);
        ImageHelper resizer = new ImageHelper(fullResImageFile);
        resizer.saveAtResolution(viewSet.getThumbnailImageFile(i),
                viewSet.getThumbnailWidth(), viewSet.getThumbnailHeight());
    }

    private static void logFinished(File fileFinished)
    {
        log.info("Finished {}", fileFinished);
    }

    private static void markFinished(ViewSet viewSet, AtomicInteger finishedCount)
    {
        finishedCount.getAndAdd(1);
    }

    private static void logExists(File previewImageFile)
    {
        log.info("Skipping {} : Already exists", previewImageFile);
    }

    /**
     * Used to generate all preview images in bulk
     * @param viewSet
     * @throws IOException
     */
    private static void generateUndistortedPreviewImages(ViewSet viewSet, int maxLoadingThreads, ProgressMonitor progressMonitor)
        throws IOException, UserCancellationException
    {
        if (Objects.equals(viewSet.getRelativePreviewImagePathName(), viewSet.getRelativeFullResImagePathName()))
        {
            throw new IllegalStateException("Preview directory is the same as the full res directory; generating preview images would overwrite full resolution images.");
        }
        else if (viewSet.getPreviewWidth() == 0 || viewSet.getPreviewHeight() == 0)
        {
            log.warn("Preview width or preview height are 0; skipping preview images");
        }
        else
        {
            Date timestamp = new Date();

            log.info("Generating undistorted preview images...");

            viewSet.getPreviewImageFilePath().mkdirs();

            progressMonitor.setMaxProgress(viewSet.getCameraPoseCount());

            AtomicInteger finishedCount = new AtomicInteger(0);
            AtomicInteger failedCount = new AtomicInteger(0);
            AtomicReference<UserCancellationException> cancelled = new AtomicReference<>(null);

            if (MULTITHREAD_PREVIEW_IMAGE_GENERATION)
            {
                multithreadPreviewImgGeneration(viewSet, maxLoadingThreads, progressMonitor, finishedCount, failedCount);
            }
            else // sequential mode
            {
                sequentialPreviewImgGeneration(viewSet, progressMonitor, cancelled, finishedCount, failedCount);
            }

            // Wait for all threads to finish
            while (cancelled.get() == null && failedCount.get() + finishedCount.get() < viewSet.getCameraPoseCount())
            {
                Thread.onSpinWait();
            }

            if (cancelled.get() != null)
            {
                throw cancelled.get();
            }
            else if (failedCount.get() > 0)
            {
                throwUndistortFailed(progressMonitor, failedCount);
            }
            else
            {
                // Generating preview images is now complete.
                // Go back to indeterminate progress until it starts to actually load for rendering
                progressMonitor.setMaxProgress(0.0);
                log.info("Undistorted preview images generated in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
            }
        }
    }

    private static void generateThumbnailImages(ViewSet viewSet, int maxLoadingThreads, ProgressMonitor progressMonitor) throws IOException, UserCancellationException{
        Date timestamp = new Date();

        File supportingFilePath = viewSet.getSupportingFilesFilePath();
        File thumbnails = new File(supportingFilePath,"thumbnails");
        thumbnails.mkdirs();

        progressMonitor.setMaxProgress(viewSet.getCameraPoseCount());

        AtomicInteger finishedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicReference<UserCancellationException> cancelled = new AtomicReference<>(null);

        if (MULTITHREAD_PREVIEW_IMAGE_GENERATION)
        {
            multithreadThumbnailImgGeneration(viewSet, maxLoadingThreads, progressMonitor, finishedCount, failedCount);
        }
        else // sequential mode
        {
            sequentialThumbnailImgGeneration(viewSet, progressMonitor, cancelled, failedCount, finishedCount);
        }

        // Wait for all threads to finish
        while (cancelled.get() == null && failedCount.get() + finishedCount.get() < viewSet.getCameraPoseCount())
        {
            Thread.onSpinWait();
        }

        if (cancelled.get() != null)
        {
            throw cancelled.get();
        }
        else if (failedCount.get() > 0)
        {
            throwUndistortFailed(progressMonitor, failedCount);
        }
        else
        {
            // Generating preview images is now complete.
            // Go back to indeterminate progress until it starts to actually load for rendering
            progressMonitor.setMaxProgress(0.0);
            log.info("Undistorted thumbnail images generated in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
        }
    }

    private static void sequentialPreviewImgGeneration(ViewSet viewSet, ProgressMonitor progressMonitor,
        AtomicReference<UserCancellationException> cancelled, AtomicInteger finishedCount, AtomicInteger failedCount)
    {
        // Do the undistortion on the rendering thread
        Rendering.runLater(new GraphicsRequest()
        {
            @Override
            public <ContextType extends Context<ContextType>> void executeRequest(ContextType context) throws UserCancellationException
            {
                for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
                {
                    progressMonitor.setProgress(i, MessageFormat.format("{0} ({1}/{2})", viewSet.getImageFileName(i), i+1, viewSet.getCameraPoseCount()));

                    try
                    {
                        progressMonitor.allowUserCancellation();
                    }
                    catch (UserCancellationException e)
                    {
                        cancelled.set(e); // forward exception to another thread
                        throw e;
                    }

                    try
                    {
                        // Check if the image is there first
                        File previewImageFile = viewSet.findPreviewImageFile(i);
                        logExists(previewImageFile);
                        markFinished(viewSet, finishedCount);
                    }
                    catch (FileNotFoundException e)
                    {
                        try
                        {
                            // Only generate the image if it wasn't found
                            int projectionIndex = viewSet.getCameraProjectionIndex(i);
                            if (viewSet.getCameraProjection(projectionIndex) instanceof DistortionProjection)
                            {
                                BufferedImage decodedImage = getDecodedImage(viewSet, i);
                                BufferedImage decodedMaskImage = getDecodedMaskImage(viewSet, i);
                                log.info("Undistorting image {}", i);
                                BufferedImage imageOut = undistortImage(decodedImage,decodedMaskImage, true, viewSet, projectionIndex, context);
                                log.info("Saving image {}", i);
                                ImageIO.write(imageOut, "PNG", viewSet.getPreviewImageFile(i));
                                logFinished(viewSet.getPreviewImageFile(i));
                            }
                            else
                            {
                                // Fallback to simply resizing without undistorting
                                File fullResImageFile = viewSet.findFullResImageFile(i);
                                File mask = viewSet.getMask(i);
//                                resizeImage(fullResImageFile, viewSet, i);
                                resizeImageWithMask(fullResImageFile, mask, viewSet, i);
                            }

                            markFinished(viewSet, finishedCount);
                        }
                        catch (RuntimeException | IOException ex)
                        {
                            log.error(ex.getMessage(), ex);
                            failedCount.getAndAdd(1);
                        }
                    }
                }
            }
        });

        log.info("Waiting for undistortion to finish on rendering thread");
    }

    private static void multithreadPreviewImgGeneration(ViewSet viewSet, int maxLoadingThreads, ProgressMonitor progressMonitor,
        AtomicInteger finishedCount, AtomicInteger failedCount)
    {
        progressMonitor.setProgress(0, "Importing and downsizing images (multithread)...");

        // Need to use custom ForkJoinPool so that number of threads doesn't go out of control and use up the Java heap space
        ForkJoinPool customThreadPool = new ForkJoinPool(maxLoadingThreads);

        customThreadPool.submit(() -> IntStream.range(0, viewSet.getCameraPoseCount())
            .parallel() // allow images to be processed in parallel; especially important for ICC transformation if present
            .forEach(i ->
            {
                try
                {
                    // Check if the image is there first
                    File previewImageFile = viewSet.findPreviewImageFile(i);
                    logExists(previewImageFile);
                    markFinished(viewSet, finishedCount);
                    progressMonitor.setProgress(finishedCount.get() + failedCount.get(),
                        MessageFormat.format("Completed: {0} ({1}/{2})", viewSet.getImageFileName(i),
                            finishedCount.get() + failedCount.get(), viewSet.getCameraPoseCount()));
                }
                catch (FileNotFoundException e)
                {
                    // Only generate the image if it wasn't found
                    int projectionIndex = viewSet.getCameraProjectionIndex(i);
                    if (viewSet.getCameraProjection(projectionIndex) instanceof DistortionProjection)
                    {
                        try
                        {
                            BufferedImage decodedImage = getDecodedImage(viewSet, i);

                            // Do the undistortion on the rendering thread
                            Rendering.runLater(new GraphicsRequest()
                            {
                                @Override
                                public <ContextType extends Context<ContextType>> void executeRequest(ContextType context)
                                {
                                    try
                                    {
                                        BufferedImage decodedMaskImage = getDecodedMaskImage(viewSet, i);
                                        BufferedImage imageOut = undistortImage(decodedImage, decodedMaskImage, true, viewSet, projectionIndex, context);

                                        // Write to a file on another thread so as not to block the rendering thread
                                        new Thread(() ->
                                        {
                                            try
                                            {
                                                ImageIO.write(imageOut, "PNG", viewSet.getPreviewImageFile(i));
                                                logFinished(viewSet.getPreviewImageFile(i));
                                                markFinished(viewSet, finishedCount);
                                                progressMonitor.setProgress(finishedCount.get() + failedCount.get(),
                                                    MessageFormat.format("Completed: {0} ({1}/{2})", viewSet.getImageFileName(i),
                                                        finishedCount.get() + failedCount.get(), viewSet.getCameraPoseCount()));
                                            }
                                            catch (IOException|RuntimeException ex)
                                            {
                                                // Failure to save the final file
                                                log.error(ex.getMessage(), ex);
                                                failedCount.getAndAdd(1);
                                            }

                                        }).start();
                                    }
                                    catch (IOException|RuntimeException ex)
                                    {
                                        // Failure to undistort
                                        log.error(ex.getMessage(), ex);
                                        failedCount.getAndAdd(1);
                                    }
                                }
                            });
                        }
                        catch (IOException|RuntimeException ex)
                        {
                            // Failure to read the original image
                            log.error(ex.getMessage(), ex);
                            failedCount.getAndAdd(1);
                        }
                    }
                    else
                    {
                        try
                        {
                            // Fallback to simply resizing without undistorting
                            // Does not require graphics context, so threading is simple.
                            File fullResImageFile = viewSet.findFullResImageFile(i);
//                            resizeImage(fullResImageFile, viewSet, i);
                            File mask = viewSet.getMask(i);
                            resizeImageWithMask(fullResImageFile, mask, viewSet, i);
                            markFinished(viewSet, finishedCount);
                            progressMonitor.setProgress(finishedCount.get() + failedCount.get(),
                                MessageFormat.format("Completed: {0} ({1}/{2})", viewSet.getImageFileName(i),
                                    finishedCount.get() + failedCount.get(), viewSet.getCameraPoseCount()));
                        }
                        catch (IOException|RuntimeException ex)
                        {
                            log.error(ex.getMessage(), ex);
                            failedCount.getAndAdd(1);
                        }
                    }
                }
            }));

        log.info("Finished reading all images; waiting for undistortion to finish on other threads");
    }

    private static void multithreadThumbnailImgGeneration(ViewSet viewSet, int maxLoadingThreads, ProgressMonitor progressMonitor,
          AtomicInteger finishedCount, AtomicInteger failedCount)
    {
        progressMonitor.setProgress(2, "Importing and downsizing images (multithread)...");

        // Need to use custom ForkJoinPool so that number of threads doesn't go out of control and use up the Java heap space
        ForkJoinPool customThreadPool = new ForkJoinPool(maxLoadingThreads);

        customThreadPool.submit(() -> IntStream.range(0, viewSet.getCameraPoseCount())
                .parallel() // allow images to be processed in parallel; especially important for ICC transformation if present
                .forEach(i ->
                {
                    try
                    {
                        // Check if the image is there first
                        File thumbnailImageFile = viewSet.findThumbnailImageFile(i);
                        logExists(thumbnailImageFile);
                        markFinished(viewSet, finishedCount);
                        progressMonitor.setProgress(finishedCount.get() + failedCount.get(),
                                MessageFormat.format("Thumbnail Completed: {0} ({1}/{2})", viewSet.getImageFileName(i),
                                        finishedCount.get() + failedCount.get(), viewSet.getCameraPoseCount()));
                    }
                    catch (FileNotFoundException e)
                    {
                        // Only generate the image if it wasn't found
                        int projectionIndex = viewSet.getCameraProjectionIndex(i);
                        if (viewSet.getCameraProjection(projectionIndex) instanceof DistortionProjection)
                        {
                            try
                            {
                                BufferedImage decodedImage = getDecodedImage(viewSet, i);

                                // Do the undistortion on the rendering thread
                                Rendering.runLater(new GraphicsRequest()
                                {
                                    @Override
                                    public <ContextType extends Context<ContextType>> void executeRequest(ContextType context)
                                    {
                                        try
                                        {
                                            BufferedImage imageOut = undistortThumbnailImage(decodedImage, viewSet, projectionIndex, context);

                                            // Write to a file on another thread so as not to block the rendering thread
                                            new Thread(() ->
                                            {
                                                try
                                                {
                                                    ImageIO.write(imageOut, "PNG", viewSet.getThumbnailImageFile(i));
                                                    logFinished(viewSet.getThumbnailImageFile(i));
                                                    markFinished(viewSet, finishedCount);
                                                    progressMonitor.setProgress(finishedCount.get() + failedCount.get(),
                                                            MessageFormat.format("Thumbnail Completed: {0} ({1}/{2})", viewSet.getImageFileName(i),
                                                                    finishedCount.get() + failedCount.get(), viewSet.getCameraPoseCount()));
                                                }
                                                catch (IOException|RuntimeException ex)
                                                {
                                                    // Failure to save the final file
                                                    log.error(ex.getMessage(), ex);
                                                    failedCount.getAndAdd(1);
                                                }

                                            }).start();
                                        }
                                        catch (IOException|RuntimeException ex)
                                        {
                                            // Failure to undistort
                                            log.error(ex.getMessage(), ex);
                                            failedCount.getAndAdd(1);
                                        }
                                    }
                                });
                            }
                            catch (IOException|RuntimeException ex)
                            {
                                // Failure to read the original image
                                log.error(ex.getMessage(), ex);
                                failedCount.getAndAdd(1);
                            }
                        }
                        else
                        {
                            try
                            {
                                // Fallback to simply resizing without undistorting
                                // Does not require graphics context, so threading is simple.
                                File fullResImageFile = viewSet.findFullResImageFile(i);
                                resizeThumbnailImage(fullResImageFile, viewSet, i);
                                markFinished(viewSet, finishedCount);
                                progressMonitor.setProgress(finishedCount.get() + failedCount.get(),
                                        MessageFormat.format("Thumbnail Completed: {0} ({1}/{2})", viewSet.getImageFileName(i),
                                                finishedCount.get() + failedCount.get(), viewSet.getCameraPoseCount()));
                            }
                            catch (IOException|RuntimeException ex)
                            {
                                log.error(ex.getMessage(), ex);
                                failedCount.getAndAdd(1);
                            }
                        }
                    }
                }));
    }

    private static void sequentialThumbnailImgGeneration(ViewSet viewSet, ProgressMonitor progressMonitor, AtomicReference<UserCancellationException> cancelled, AtomicInteger failedCount, AtomicInteger finishedCount)
    {
        // Do the undistortion on the rendering thread
        Rendering.runLater(new GraphicsRequest()
        {
            @Override
            public <ContextType extends Context<ContextType>> void executeRequest(ContextType context) throws UserCancellationException
            {
                for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
                {
                    progressMonitor.setProgress(i, MessageFormat.format("{0} ({1}/{2})", viewSet.getImageFileName(i), i+1, viewSet.getCameraPoseCount()));

                    try
                    {
                        progressMonitor.allowUserCancellation();
                    }
                    catch (UserCancellationException e)
                    {
                        cancelled.set(e); // forward exception to another thread
                        throw e;
                    }

                    try
                    {
                        // Check if the image is there first
                        File thumbnailImageFile = viewSet.findThumbnailImageFile(i);
                        logExists(thumbnailImageFile);
                        markFinished(viewSet, finishedCount);
                    }
                    catch (FileNotFoundException e)
                    {
                        try
                        {
                            // Only generate the image if it wasn't found
                            int projectionIndex = viewSet.getCameraProjectionIndex(i);
                            if (viewSet.getCameraProjection(projectionIndex) instanceof DistortionProjection)
                            {
                                BufferedImage decodedImage = getDecodedImage(viewSet, i);
                                log.info("Undistorting thumbnail image {}", i);
                                BufferedImage imageOut = undistortThumbnailImage(decodedImage, viewSet, projectionIndex, context);
                                log.info("Saving thumbnail image {}", i);
                                ImageIO.write(imageOut, "PNG", viewSet.getThumbnailImageFile(i));
                                logFinished(viewSet.getThumbnailImageFile(i));
                            }
                            else
                            {
                                // Fallback to simply resizing without undistorting
                                File fullResImageFile = viewSet.findFullResImageFile(i);
                                resizeThumbnailImage(fullResImageFile, viewSet, i);
                            }

                            markFinished(viewSet, finishedCount);
                        }
                        catch (RuntimeException | IOException ex)
                        {
                            log.error(ex.getMessage(), ex);
                            failedCount.getAndAdd(1);
                        }
                    }
                }
            }
        });

        log.info("Waiting for undistortion to finish on rendering thread");
    }

    /**
     * Used to generate a single preview image if one is missing
     * @param poseIndex
     * @throws IOException
     */
    private boolean generateUndistortedPreviewImage(int poseIndex) throws IOException
    {
        if (Objects.equals(getViewSet().getRelativePreviewImagePathName(), getViewSet().getRelativeFullResImagePathName()))
        {
            throw new IllegalStateException("Preview directory is the same as the full res directory; generating preview images would overwrite full resolution images.");
        }
        else
        {
            // Make sure the preview image directory exists; create it if not
            getViewSet().getPreviewImageFilePath().mkdirs();

            int projectionIndex = getViewSet().getCameraProjectionIndex(poseIndex);
            if (getViewSet().getCameraProjection(projectionIndex) instanceof DistortionProjection)
            {
                // Distortion exists; undistort
                log.info("Undistorting image {}/{}", poseIndex, getViewSet().getCameraPoseCount());

                DistortionProjection distortion = (DistortionProjection) getViewSet().getCameraProjection(projectionIndex);

                // If no preview width / height is specified, just use whatever was originally in the distortion model
                if (getViewSet().getPreviewWidth() > 0 && getViewSet().getPreviewHeight() > 0)
                {
                    distortion = distortion.scaledTo(getViewSet().getPreviewWidth(), getViewSet().getPreviewHeight());
                }

                try (ImageUndistorter<?> undistort = new ImageUndistorter<>(getContext()))
                {
                    undistort.undistortFile(
                            getViewSet().findFullResImageFile(poseIndex),
                            getViewSet().getMask(poseIndex),
                            true,
                            distortion,
                            getViewSet().getPreviewImageFile(poseIndex));
                }

                return true;
            }
            else if (getViewSet().getPreviewWidth() > 0 && getViewSet().getPreviewHeight() > 0)
            {
                log.info("Resizing image {}/{} : No distortion parameters", poseIndex, getViewSet().getCameraPoseCount());

                // Fallback to simply resizing without undistorting
                resizeImage(getViewSet().findFullResImageFile(poseIndex), getViewSet(), poseIndex);

                return true;
            }
            else
            {
                // No distortion or preview dimensions, just use the original image
                log.warn("Using full resolution image {}/{} : No distortion and preview width and/or preview height are 0",
                    poseIndex, getViewSet().getCameraPoseCount());
                return false;
            }
        }
    }

    private File findOrGeneratePreviewImageFile(int index) throws IOException
    {
        try
        {
            // See if the preview image is already there
            return ImageFinder.getInstance().findImageFile(getViewSet().getPreviewImageFile(index));
        }
        catch (FileNotFoundException e)
        {
            if (generateUndistortedPreviewImage(index)) // Generate file if necessary
            {
                return ImageFinder.getInstance().findImageFile(getViewSet().getPreviewImageFile(index));
            }
            else // File was not generated: no distortion and preview dimensions are zero.
            {
                return ImageFinder.getInstance().findImageFile(getViewSet().getFullResImageFile(index));
            }
        }
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
