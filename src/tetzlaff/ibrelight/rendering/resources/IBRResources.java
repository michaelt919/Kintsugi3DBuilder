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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;

import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.FramebufferObjectBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.gl.material.Material;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.types.AbstractDataTypeFactory;
import tetzlaff.gl.geometry.GeometryResources;
import tetzlaff.gl.geometry.VertexGeometry;
import tetzlaff.gl.vecmath.IntVector3;
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
public final class IBRResources<ContextType extends Context<ContextType>> implements AutoCloseable
{
    /**
     * The graphics context associated with this instance.
     */
    public final ContextType context;

    /**
     * The view set that these resources were loaded from.
     */
    public final ViewSet viewSet;

    /**
     * The geometry for this instance that the vertex buffers were loaded from.
     */
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

    // TODO: Everything related to luminance maps should maybe be encapsulated in its own class

    /**
     * A 1D texture defining how encoded RGB values should be converted to linear luminance.
     */
    private Texture1D<ContextType> luminanceMap;

    /**
     * A 1D texture defining how encoded RGB values should be converted to linear luminance.
     */
    private Texture1D<ContextType> inverseLuminanceMap;

    /**
     * Contains the VBOs for positions, tex-coords, normals, and tangents
     */
    public GeometryResources<ContextType> geometryResources;

    // TODO: Everything related to standard texture maps should maybe be encapsulated in its own class

    /**
     * A depth texture array containing a depth image for every view.
     */
    public final Texture3D<ContextType> depthTextures;

    /**
     * A diffuse texture map, if one exists.
     */
    public final Texture2D<ContextType> diffuseTexture;

    /**
     * A normal map, if one exists.
     */
    public final Texture2D<ContextType> normalTexture;

    /**
     * A specular reflectivity map, if one exists.
     */
    public final Texture2D<ContextType> specularTexture;

    /**
     * A specular roughness map, if one exists.
     */
    public final Texture2D<ContextType> roughnessTexture;

    /**
     * A depth texture array containing a shadow map for every view.
     */
    public final Texture3D<ContextType> shadowTextures;

    /**
     * A GPU buffer containing the matrices that were used for each shadow map in the shadowTextures array.
     */
    public final UniformBuffer<ContextType> shadowMatrixBuffer;

    /**
     * A GPU buffer containing the weights associated with all the views (determined by the distance from other views).
     */
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

            if (primaryViewName != null)
            {
                viewSet.setPrimaryView(primaryViewName);
            }

            if (geometry == null && viewSet != null && viewSet.getGeometryFile() != null)
            {
                // Load geometry if it wasn't specified but a view set was.
                geometry = VertexGeometry.createFromOBJFile(viewSet.getGeometryFile());
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

        // Read the images from a file
        if (loadOptions != null && loadOptions.areColorImagesRequested() && viewSet.getImageFilePath() != null && viewSet.getCameraPoseCount() > 0)
        {
            Date timestamp = new Date();

            File imageFile = findImageFile(viewSet.getPrimaryViewIndex());

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
                imageFile = findImageFile(i);

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

        // Store the poses in a uniform buffer
        if (viewSet != null && viewSet.getCameraPoseData() != null)
        {
            // Create the uniform buffer
            cameraPoseBuffer = context.createUniformBuffer().setData(viewSet.getCameraPoseData());
        }
        else
        {
            cameraPoseBuffer = null;
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

        // Store the light positions in a uniform buffer
        if (viewSet != null && viewSet.getLightPositionData() != null)
        {
            // Create the uniform buffer
            lightPositionBuffer = context.createUniformBuffer().setData(viewSet.getLightPositionData());
        }
        else
        {
            lightPositionBuffer = null;
        }

        // Store the light positions in a uniform buffer
        if (viewSet != null && viewSet.getLightIntensityData() != null)
        {
            // Create the uniform buffer
            lightIntensityBuffer = context.createUniformBuffer().setData(viewSet.getLightIntensityData());
        }
        else
        {
            lightIntensityBuffer = null;
        }

        // Store the light indices in a uniform buffer
        if (viewSet != null && viewSet.getLightIndexData() != null)
        {
            lightIndexBuffer = context.createUniformBuffer().setData(viewSet.getLightIndexData());
        }
        else
        {
            lightIndexBuffer = null;
        }

        // Luminance map texture
        if (viewSet != null && viewSet.hasCustomLuminanceEncoding())
        {
            luminanceMap = viewSet.getLuminanceEncoding().createLuminanceMap(context);
            inverseLuminanceMap = viewSet.getLuminanceEncoding().createInverseLuminanceMap(context);
        }
        else
        {
            luminanceMap = null;
            inverseLuminanceMap = null;
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

                if (loadOptions.isCompressionRequested())
                {
                    diffuseTextureBuilder.setInternalFormat(CompressionFormat.RGB_4BPP);
                }
                else
                {
                    diffuseTextureBuilder.setInternalFormat(ColorFormat.RGB8);
                }

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

                if (loadOptions.isCompressionRequested())
                {
                    normalTextureBuilder.setInternalFormat(CompressionFormat.RED_4BPP_GREEN_4BPP);
                }
                else
                {
                    normalTextureBuilder.setInternalFormat(ColorFormat.RG8);
                }

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
                if (loadOptions.isCompressionRequested())
                {
                    specularTextureBuilder.setInternalFormat(CompressionFormat.RGB_4BPP);
                }
                else
                {
                    specularTextureBuilder.setInternalFormat(ColorFormat.RGB8);
                }

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
                ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> roughnessTextureBuilder;

//                if (loadOptions.isCompressionRequested())
//                {
//                    // Use 16 bits to give the built-in compression algorithm extra precision to work with.
//                    roughnessTextureBuilder =
//                        context.getTextureFactory().build2DColorTextureFromFile(roughnessFile, true,
//                            AbstractDataTypeFactory.getInstance().getMultiComponentDataType(NativeDataType.UNSIGNED_SHORT, 3),
//                            color -> new IntVector3(
//                                (int)Math.max(0, Math.min(0xFFFF, Math.round(
//                                    (Math.max(-15.0, Math.min(15.0, (color.getRed() - color.getGreen()) * 30.0 / 255.0)) + 16.0) * 0xFFFF / 31.0))),
//                                (int)Math.max(0, Math.min(0xFFFF, Math.round(color.getGreen() * 0xFFFF / 255.0))),
//                                (int)Math.max(0, Math.min(0xFFFF, Math.round(
//                                    (Math.max(-15.0, Math.min(15.0, (color.getBlue() - color.getGreen()) * 30.0 / 255.0)) + 16.0) * 0xFFFF / 31.0)))));
//                    roughnessTextureBuilder.setInternalFormat(CompressionFormat.RGB_4BPP);
//                }
//                else
                {
                    roughnessTextureBuilder =
                        context.getTextureFactory().build2DColorTextureFromFile(roughnessFile, true,
                            AbstractDataTypeFactory.getInstance().getMultiComponentDataType(NativeDataType.UNSIGNED_BYTE, 3),
                            color -> new IntVector3(
                                (int)Math.max(0, Math.min(255, Math.round(
                                    (Math.max(-15.0, Math.min(15.0, (color.getRed() - color.getGreen()) * 30.0 / 255.0)) + 16.0) * 255.0 / 31.0))),
                                color.getGreen(),
                                (int)Math.max(0, Math.min(255, Math.round(
                                    (Math.max(-15.0, Math.min(15.0, (color.getBlue() - color.getGreen()) * 30.0 / 255.0)) + 16.0) * 255.0 / 31.0)))));
                    roughnessTextureBuilder.setInternalFormat(ColorFormat.RGB8);
                }

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
    public ProgramBuilder<ContextType> getIBRShaderProgramBuilder(StandardRenderingMode renderingMode)
    {
        ProgramBuilder<ContextType> builder = context.getShaderProgramBuilder()
            .define("CAMERA_POSE_COUNT", this.viewSet.getCameraPoseCount())
            .define("LIGHT_COUNT", this.viewSet.getLightCount())
            .define("CAMERA_PROJECTION_COUNT", this.viewSet.getCameraProjectionCount())
            .define("LUMINANCE_MAP_ENABLED", this.luminanceMap != null)
            .define("INVERSE_LUMINANCE_MAP_ENABLED", this.inverseLuminanceMap != null)
            .define("INFINITE_LIGHT_SOURCES", this.viewSet.areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", this.depthTextures != null)
            .define("SHADOW_TEST_ENABLED", this.shadowTextures != null)
            .define("IMAGE_BASED_RENDERING_ENABLED", renderingMode.isImageBased())
            .define("DIFFUSE_TEXTURE_ENABLED", this.diffuseTexture != null && renderingMode.useDiffuseTexture())
            .define("SPECULAR_TEXTURE_ENABLED", this.specularTexture != null && renderingMode.useSpecularTextures())
            .define("ROUGHNESS_TEXTURE_ENABLED", this.roughnessTexture != null && renderingMode.useSpecularTextures())
            .define("NORMAL_TEXTURE_ENABLED", this.normalTexture != null && renderingMode.useNormalTexture());

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
    public ProgramBuilder<ContextType> getIBRShaderProgramBuilder()
    {
        return getIBRShaderProgramBuilder(StandardRenderingMode.IMAGE_BASED);
    }

    public static File findImageFile(File requestedFile) throws FileNotFoundException
    {
        if (requestedFile.exists())
        {
            return requestedFile;
        }
        else
        {
            // Try some alternate file formats/extensions
            String[] altFormats = { "png", "PNG", "jpg", "JPG", "jpeg", "JPEG" };
            for(String extension : altFormats)
            {
                String[] filenameParts = requestedFile.getName().split("\\.");

                String altFileName;
                if (filenameParts.length > 1)
                {
                    filenameParts[filenameParts.length - 1] = extension;
                    altFileName = String.join(".", filenameParts);
                }
                else
                {
                    altFileName = String.join(".", filenameParts[0], extension);
                }

                File imageFileGuess = new File(requestedFile.getParentFile(), altFileName);

                System.out.printf("Trying '%s'\n", imageFileGuess.getAbsolutePath());
                if (imageFileGuess.exists())
                {
                    System.out.printf("Found!!\n");
                    return imageFileGuess;
                }
            }

            // Is it still not there?
            throw new FileNotFoundException(
                String.format("'%s' not found.", requestedFile.getName()));
        }
    }

    /**
     * Finds the image file for a particular view index.
     * @param index The index of the view to find.
     * @return The image file at the specified view index.
     * @throws FileNotFoundException if the image file cannot be found.
     */
    public File findImageFile(int index) throws FileNotFoundException
    {
        return findImageFile(viewSet.getImageFile(index));
    }

    private void updateShadowTextures() throws FileNotFoundException
    {
        if (this.shadowTextures != null)
        {
            try
            (
                // Don't automatically generate any texture attachments for this framebuffer object
                FramebufferObject<ContextType> depthRenderingFBO =
                    context.buildFramebufferObject(this.shadowTextures.getWidth(), this.shadowTextures.getHeight())
                        .createFramebufferObject();

                // Load the program
                DepthMapGenerator<ContextType> depthMapGenerator = DepthMapGenerator.createFromGeometryResources(geometryResources)
            )
            {
                // Flatten the camera pose matrices into 16-component vectors and store them in the vertex list data structure.
                NativeVectorBuffer flattenedShadowMatrices = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 16, this.viewSet.getCameraPoseCount());

                // Render each depth texture
                for (int i = 0; i < this.viewSet.getCameraPoseCount(); i++)
                {
                    depthRenderingFBO.setDepthAttachment(shadowTextures.getLayerAsFramebufferAttachment(i));
                    Matrix4 shadowMatrix = depthMapGenerator.generateShadowMap(viewSet, i, depthRenderingFBO);

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
     * Refresh the luminance map textures using the current values in the view set.
     */
    public void updateLuminanceMap()
    {
        if (luminanceMap != null)
        {
            luminanceMap.close();
            luminanceMap = null;
        }

        if (inverseLuminanceMap != null)
        {
            inverseLuminanceMap.close();
            inverseLuminanceMap = null;
        }

        if (viewSet.hasCustomLuminanceEncoding())
        {
            luminanceMap = viewSet.getLuminanceEncoding().createLuminanceMap(context);
            inverseLuminanceMap = viewSet.getLuminanceEncoding().createInverseLuminanceMap(context);
        }
    }

    /**
     * Refresh the light data in the uniform buffers using the current values in the view set.
     */
    public void updateLightData()
    {
        // Store the light positions in a uniform buffer
        if (lightPositionBuffer != null && viewSet.getLightPositionData() != null)
        {
            // Create the uniform buffer
            lightPositionBuffer.setData(viewSet.getLightPositionData());
        }

        // Store the light positions in a uniform buffer
        if (lightIntensityBuffer != null && viewSet.getLightIntensityData() != null)
        {
            // Create the uniform buffer
            lightIntensityBuffer.setData(viewSet.getLightIntensityData());
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

    /**
     * Gets the luminance map (the gamma decoding function) as a 1D texture.
     * @return The 1D luminance map texture.
     */
    public Texture1D<ContextType> getLuminanceMap()
    {
        return luminanceMap;
    }

    /**
     * Gets the inverse luminance map (the gamma encoding function) as a 1D texture.
     * @return The 1D inverse luminance map texture.
     */
    public Texture1D<ContextType> getInverseLuminanceMap()
    {
        return inverseLuminanceMap;
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

        program.setUniform("gamma", this.viewSet.getGamma());

        if (this.luminanceMap == null)
        {
            program.setTexture("luminanceMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_1D));
        }
        else
        {
            program.setTexture("luminanceMap", this.luminanceMap);
        }

        if (this.inverseLuminanceMap == null)
        {
            program.setTexture("inverseLuminanceMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_1D));
        }
        else
        {
            program.setTexture("inverseLuminanceMap", this.inverseLuminanceMap);
        }

        if (this.depthTextures == null)
        {
            program.setTexture("depthImages", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_2D_ARRAY));
        }
        else
        {
            program.setTexture("depthImages", this.depthTextures);
            program.setUniform("occlusionBias", 0.002f);
        }

        if (this.shadowMatrixBuffer == null || this.shadowTextures == null)
        {
            program.setTexture("shadowImages", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_2D_ARRAY));
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
    public void setupShaderProgram(Program<ContextType> program)
    {
        setupCommon(program);

        if (this.normalTexture == null)
        {
            program.setTexture("normalMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_2D));
        }
        else
        {
            program.setTexture("normalMap", this.normalTexture);
        }

        if (this.diffuseTexture == null)
        {
            program.setTexture("diffuseMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_2D));
        }
        else
        {
            program.setTexture("diffuseMap", this.diffuseTexture);
        }

        if (this.specularTexture == null)
        {
            program.setTexture("specularMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_2D));
        }
        else
        {
            program.setTexture("specularMap", this.specularTexture);
        }

        if (this.roughnessTexture == null)
        {
            program.setTexture("roughnessMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_2D));
        }
        else
        {
            program.setTexture("roughnessMap", this.roughnessTexture);
        }
    }

    /**
     * Creates a Drawable using this instance's geometry resources, and the specified shader program.
     * @param program The program to use to construct the Drawable.
     * @return A Drawable for rendering this instance using the specified shader program.
     */
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
    public GraphicsStream<ColorList[]> stream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount)
    {
        return new SequentialViewRenderStream<>(viewSet.getCameraPoseCount(), drawable, framebuffer, attachmentCount);
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
    public GraphicsStream<ColorList> stream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        return new SequentialViewRenderStream<>(viewSet.getCameraPoseCount(), drawable, framebuffer, 1)
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
    public GraphicsStreamResource<ContextType> streamAsResource(
        ProgramBuilder<ContextType> programBuilder,
        FramebufferObjectBuilder<ContextType> framebufferBuilder) throws FileNotFoundException
    {
        return new GraphicsStreamResource<>(programBuilder, framebufferBuilder,
            (program, framebuffer) -> new SequentialViewRenderStream<>(
                viewSet.getCameraPoseCount(), createDrawable(program), framebuffer, framebuffer.getColorAttachmentCount()));
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
    public GraphicsStream<ColorList[]> parallelStream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount, int maxRunningThreads)
    {
        return new ParallelViewRenderStream<>(viewSet.getCameraPoseCount(), drawable, framebuffer, attachmentCount, maxRunningThreads);
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
    public GraphicsStream<ColorList[]> parallelStream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int attachmentCount)
    {
        return new ParallelViewRenderStream<>(viewSet.getCameraPoseCount(), drawable, framebuffer, attachmentCount);
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
    public GraphicsStream<ColorList> parallelStream(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        return new ParallelViewRenderStream<>(viewSet.getCameraPoseCount(), drawable, framebuffer, 1)
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
    public GraphicsStreamResource<ContextType> parallelStreamAsResource(
        ProgramBuilder<ContextType> programBuilder,
        FramebufferObjectBuilder<ContextType> framebufferBuilder,
        int maxRunningThreads) throws FileNotFoundException
    {
        return new GraphicsStreamResource<>(programBuilder, framebufferBuilder,
            (program, framebuffer) -> new ParallelViewRenderStream<>(
                viewSet.getCameraPoseCount(), createDrawable(program), framebuffer, framebuffer.getColorAttachmentCount(), maxRunningThreads));
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
    public GraphicsStreamResource<ContextType> parallelStreamAsResource(
        ProgramBuilder<ContextType> programBuilder,
        FramebufferObjectBuilder<ContextType> framebufferBuilder) throws FileNotFoundException
    {
        return new GraphicsStreamResource<>(programBuilder, framebufferBuilder,
            (program, framebuffer) -> new ParallelViewRenderStream<>(
                viewSet.getCameraPoseCount(), createDrawable(program), framebuffer, framebuffer.getColorAttachmentCount()));
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

    /**
     * Gets the weight associated with a given view/camera (determined by the distance from other views).
     * @param index The index of the view for which to retrieve its weight.
     * @return The weight for the specified view.
     */
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
        return createSingleImageResource(viewIndex, viewSet.getImageFile(viewIndex), loadOptions);
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
        return new SingleCalibratedImageResource<ContextType>(context, viewSet, viewIndex, imageFile, geometry, loadOptions);
    }

    public ImageCache<ContextType> cache(ImageCacheSettings settings) throws IOException
    {
        ImageCache<ContextType> cache = new ImageCache<>(this, settings);
        cache.initialize(/* TODO: implement high-res image directory */ viewSet.getImageFilePath());
        return cache;
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

        if (this.geometryResources != null)
        {
            this.geometryResources.close();
        }

        if (this.colorTextures != null)
        {
            this.colorTextures.close();
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

        if (luminanceMap != null)
        {
            luminanceMap.close();
        }

        if (inverseLuminanceMap != null)
        {
            inverseLuminanceMap.close();
        }
    }
}
