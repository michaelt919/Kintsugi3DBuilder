/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.resources;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import javax.imageio.ImageIO;

import kintsugi3d.builder.app.ApplicationFolders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.builders.ColorTextureBuilder;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.geometry.GeometryMode;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.material.TextureLoadOptions;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.builder.core.*;
import kintsugi3d.builder.io.ViewSetReaderFromAgisoftXML;
import kintsugi3d.builder.io.ViewSetReaderFromVSET;
import kintsugi3d.util.ImageFinder;
import kintsugi3d.util.ImageLodResizer;
import kintsugi3d.util.ImageUndistorter;

/**
 * A class that encapsulates all of the GPU resources like vertex buffers, uniform buffers, and textures for a given
 * IBR instance and provides helper methods for applying these resources in typical use cases.
 * @param <ContextType>
 */
public final class IBRResourcesImageSpace<ContextType extends Context<ContextType>> extends IBRResourcesBase<ContextType>
{
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

        private void updateViewSetFromLoadOptions()
        {
            this.viewSet.setPreviewImageResolution(loadOptions.getPreviewImageWidth(), loadOptions.getPreviewImageHeight());
            String directoryName = String.format("%s/_%dx%d", viewSet.getUuid().toString(), loadOptions.getPreviewImageWidth(), loadOptions.getPreviewImageHeight());
            this.viewSet.setRelativePreviewImagePathName(new File(ApplicationFolders.getPreviewImagesRootDirectory().toFile(), directoryName).toString());
        }

        public Builder<ContextType> setPrimaryView(String primaryViewName)
        {
            this.primaryViewName = primaryViewName;
            return this;
        }

        public Builder<ContextType> setLoadOptions(ReadonlyLoadOptionsModel loadOptions)
        {
            this.loadOptions = loadOptions;

            if (this.viewSet != null)
            {
                updateViewSetFromLoadOptions();
            }

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

        public Builder<ContextType> loadVSETFile(File vsetFile) throws Exception
        {
            this.viewSet = ViewSetReaderFromVSET.getInstance().readFromFile(vsetFile);
            this.geometry = VertexGeometry.createFromOBJFile(this.viewSet.getGeometryFile());

            if (this.loadOptions != null)
            {
                updateViewSetFromLoadOptions();
            }

            return this;
        }

        // undistorted images are defined in the load options
        public Builder<ContextType> loadAgisoftFiles(File cameraFile, File geometryFile, File undistortedImageDirectory) throws Exception
        {
            this.viewSet = ViewSetReaderFromAgisoftXML.getInstance().readFromFile(cameraFile);
            if (geometryFile != null)
            {
                this.geometry = VertexGeometry.createFromOBJFile(geometryFile);
            }
            if (undistortedImageDirectory != null)
            {
                this.imageDirectoryOverride = undistortedImageDirectory;
                this.viewSet.setRelativeFullResImagePathName(cameraFile.getParentFile().toPath().relativize(undistortedImageDirectory.toPath()).toString());
            }

            if (this.loadOptions != null)
            {
                updateViewSetFromLoadOptions();
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

        public Builder<ContextType> generateUndistortedPreviewImages() throws IOException
        {
            if (this.viewSet != null)
            {
                IBRResourcesImageSpace.generateUndistortedPreviewImages(this.viewSet, this.context);
            }

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
                viewSet.setRelativeFullResImagePathName(viewSet.getRootDirectory().toPath().relativize(imageDirectoryOverride.toPath()).toString());
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

    private IBRResourcesImageSpace(ContextType context, ViewSet viewSet, VertexGeometry geometry,
        ReadonlyLoadOptionsModel loadOptions, LoadingMonitor loadingMonitor) throws IOException
    {
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

            if(loadingMonitor != null)
            {
                loadingMonitor.setMaximum(viewSet.getCameraPoseCount());
            }

            int m = viewSet.getCameraPoseCount();
            for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
            {
                log.info("Loading camera pose {}/{}", i, m);
                File imageFile = findOrGeneratePreviewImageFile(i);

                this.colorTextures.loadLayer(i, imageFile, true);

                if(loadingMonitor != null)
                {
                    loadingMonitor.setProgress(i+1);
                }
            }

            log.info("View Set textures loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
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
        catch (FileNotFoundException e)
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
     * @param renderingMode The rendering mode to use, which may change some of the preprocessor defines.
     * @return A program builder with all of the above preprocessor defines specified, ready to have the
     * vertex and fragment shaders added as well as any additional application-specific preprocessor definitions.
     */
    @Override
    public ProgramBuilder<ContextType> getShaderProgramBuilder(StandardRenderingMode renderingMode)
    {
        return getSharedResources().getShaderProgramBuilder(renderingMode)
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

    public ImageCache<ContextType> cache(ImageCacheSettings settings) throws IOException
    {
        settings.setCacheFolderName(getViewSet().getUuid().toString());

        ImageCache<ContextType> cache = new ImageCache<>(this, settings);

        if (!cache.isInitialized())
        {
            cache.initialize(/* TODO: implement high-res image directory */ getViewSet().getFullResImageFilePath());
        }

        return cache;
    }

    /**
     * Used to generate all preview images in bulk
     * @param viewSet
     * @param context
     * @throws IOException
     */
    private static void generateUndistortedPreviewImages(ViewSet viewSet, Context<?> context) throws IOException
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

            try(ImageUndistorter<?> undistort = new ImageUndistorter<>(context))
            {
                // Undistort and resave preview images
                for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
                {
                    try
                    {
                        // Check if the image is there first
                        ImageFinder.getInstance().findImageFile(viewSet.getPreviewImageFile(i));
                        log.info("Skipping image {}/{} : Already exists", i, viewSet.getCameraPoseCount());
                    }
                    catch (FileNotFoundException e)
                    {
                        // Only generate the image if it wasn't found
                        int projectionIndex = viewSet.getCameraProjectionIndex(i);
                        if (viewSet.getCameraProjection(projectionIndex) instanceof DistortionProjection)
                        {
                            log.info("Undistorting image {}/{}", i, viewSet.getCameraPoseCount());

                            DistortionProjection distortion = (DistortionProjection) viewSet.getCameraProjection(projectionIndex);
                            distortion = distortion.scaledTo(viewSet.getPreviewWidth(), viewSet.getPreviewHeight());
                            undistort.undistortFile(viewSet.findFullResImageFile(i), distortion, viewSet.getPreviewImageFile(i));
                        }
                        else
                        {
                            log.info("Resizing image {}/{} : No distortion parameters", i, viewSet.getCameraPoseCount());

                            // Fallback to simply resizing without undistorting
                            ImageLodResizer resizer = new ImageLodResizer(viewSet.findFullResImageFile(i));
                            resizer.saveAtResolution(viewSet.getPreviewImageFile(i), viewSet.getPreviewWidth(), viewSet.getPreviewHeight());
                        }
                    }
                }
            }

            log.info("Undistorted preview images generated in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
        }
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
                    undistort.undistortFile(getViewSet().findFullResImageFile(poseIndex), distortion, getViewSet().getPreviewImageFile(poseIndex));
                }

                return true;
            }
            else if (getViewSet().getPreviewWidth() > 0 && getViewSet().getPreviewHeight() > 0)
            {
                log.info("Resizing image {}/{} : No distortion parameters", poseIndex, getViewSet().getCameraPoseCount());

                // Fallback to simply resizing without undistorting
                ImageLodResizer resizer = new ImageLodResizer(getViewSet().findFullResImageFile(poseIndex));
                resizer.saveAtResolution(getViewSet().getPreviewImageFile(poseIndex), getViewSet().getPreviewWidth(), getViewSet().getPreviewHeight());

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
