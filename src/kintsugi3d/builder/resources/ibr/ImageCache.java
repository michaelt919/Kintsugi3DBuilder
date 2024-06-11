/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.resources.ibr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import kintsugi3d.builder.core.DefaultProgressMonitor;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.SimpleLoadOptionsModel;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.geometry.GeometryFramebuffer;
import kintsugi3d.gl.geometry.GeometryTextures;
import kintsugi3d.gl.material.TextureLoadOptions;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import kintsugi3d.gl.vecmath.IntVector2;
import kintsugi3d.gl.vecmath.Vector4;
import kintsugi3d.util.BufferedImageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageCache<ContextType extends Context<ContextType>>
{
    private static final Logger log = LoggerFactory.getLogger(ImageCache.class);
    private final ContextType context;
    private final IBRResourcesImageSpace<ContextType> resources;
    private final ImageCacheSettings settings;

    private final File sampledDir;
    private final IntVector2[][] sampledPixelCoords;

    private boolean initialized = false;

    ImageCache(IBRResourcesImageSpace<ContextType> resources, ImageCacheSettings settings)
    {
        this.context = resources.getContext();
        this.resources = resources;
        this.settings = settings;

        this.sampledDir = new File(settings.getCacheDirectory(), "sampled");

        // Square 2D array to store sampled pixel coords.
        this.sampledPixelCoords = IntStream.range(0, settings.getSampledSize())
            .mapToObj(i -> new IntVector2[settings.getSampledSize()])
            .toArray(IntVector2[][]::new);

        // Check if the sample locations were previously generated, and if so, read them from the file.
        if (getSampleLocationsFile().exists())
        {
            try
            {
                readSampleLocationsFromFile();
                initialized = true; // reading sample locations succeeded; assume that we already have a valid cache

                log.info("Using existing cache.");
            }
            catch (IOException | RuntimeException e)
            {
                log.error("Error initializing image cache:", e);

                // Reset in case it partially succeeded.
                Arrays.stream(this.sampledPixelCoords).forEach(column -> Arrays.fill(column, null));
            }
        }
    }

    public ContextType getContext()
    {
        return context;
    }

    public ImageCacheSettings getSettings()
    {
        return settings;
    }

    public boolean isInitialized()
    {
        return initialized;
    }

    /**
     * Initializes the cache by loading each image one at a time and writing out texture space blocks as well as a "sampled" low-res texture for each image
     * As a side effect of this method, the context's state will have back face culling enabled,
     * @throws IOException
     */
    public void initialize(ProgressMonitor monitor) throws IOException
    {
        // Create directories to organize the cache
        sampledDir.mkdirs();

        // Make sure backface culling is disabled
        context.getState().disableBackFaceCulling();

        for (int i = 0; i < settings.getTextureSubdiv(); i++)
        {
            for (int j = 0; j < settings.getTextureSubdiv(); j++)
            {
                settings.getBlockDir(i, j).mkdirs();
            }
        }

        try(FramebufferObject<ContextType> fbo = context.buildFramebufferObject(settings.getTextureWidth(), settings.getTextureHeight())
                .addColorAttachment(ColorFormat.RGBA8)
                .createFramebufferObject())
        {
            selectSampleLocations(fbo);
            buildCache(fbo, monitor);
        }
    }

    private void selectSampleLocations(Framebuffer<ContextType> fbo) throws IOException
    {

        try (ProgramObject<ContextType> maskProgram = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_dynamic.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/common/solid.frag"))
            .createProgram();
            Drawable<ContextType> maskDrawable = resources.createDrawable(maskProgram))
        {
            fbo.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
            maskProgram.setUniform("color", new Vector4(1.0f));
            maskDrawable.draw(fbo);

            // Debugging
            File file = new File(settings.getCacheDirectory(), "debug.png");
            fbo.getTextureReaderForColorAttachment(0).saveToFile("PNG", file);

            int[] mask = fbo.getTextureReaderForColorAttachment(0).readARGB();
            BufferedImage maskImg = BufferedImageBuilder.build()
                .setDataFromArray(mask, settings.getTextureWidth(), settings.getTextureHeight())
                .flipVertical()
                .create();

            Random random = new Random();

            // Somewhat arbitrary heuristic
            int maxAttempts = 3 + settings.getTextureWidth() * settings.getTextureHeight() / (settings.getSampledSize() * settings.getSampledSize());

            // Randomly determine the pixel coordinates in the full resolution texture for each sample
            for (int xSample = 0; xSample < settings.getSampledSize(); xSample++)
            {
                int xRandMin = (int) Math.round((double) settings.getTextureWidth() * (double) xSample / (double) settings.getSampledSize());
                int xRandMax = (int) Math.round((double) settings.getTextureWidth() * (double) (xSample + 1) / (double) settings.getSampledSize());

                for (int ySample = 0; ySample < settings.getSampledSize(); ySample++)
                {
                    int xRand;
                    int yRand;
                    int attempts = 0;

                    do
                    {
                        xRand = random.nextInt(xRandMax - xRandMin) + xRandMin;

                        int yRandMin = (int) Math.round((double) settings.getTextureHeight() * (double) ySample / (double) settings.getSampledSize());
                        int yRandMax = (int) Math.round((double) settings.getTextureHeight() * (double) (ySample + 1) / (double) settings.getSampledSize());
                        yRand = random.nextInt(yRandMax - yRandMin) + yRandMin;
                        attempts++;
                    }
                    while (attempts < maxAttempts && maskImg.getRGB(xRand, yRand) == 0);
                    // keep repeating until the mask indicates a valid pixel, or until too many attempts

                    // Store the pixel coordinates in the 2D array to reference later.
                    sampledPixelCoords[xSample][ySample] = new IntVector2(xRand, yRand);
                }
            }

            writeSampleLocationsToFile();
        }
    }

    private File getSampleLocationsFile()
    {
        return new File(settings.getCacheDirectory(), "sampleLocations.txt");
    }

    private void writeSampleLocationsToFile() throws IOException
    {
        try(PrintStream out = new PrintStream(getSampleLocationsFile(), StandardCharsets.UTF_8))
        {
            Arrays.stream(sampledPixelCoords)
                .map(column -> Arrays.stream(column)
                    .map(coords -> String.format("%d,%d", coords.x, coords.y))
                    .collect(Collectors.joining(";")))
                .forEach(out::println);
        }
    }

    private void readSampleLocationsFromFile() throws IOException
    {
        // Sometimes the interrupted flag gets stuck on and needs to be reset or all File IO on the thread will fail.
        if (Thread.interrupted())
        {
            log.warn("Thread interrupted", new Throwable("Thread interrupted"));
        }

        try(Scanner scanner = new Scanner(getSampleLocationsFile(), StandardCharsets.UTF_8))
        {
            scanner.useLocale(Locale.US);

            // Loop over columns
            for (int i = 0; i < settings.getSampledSize(); i++)
            {
                String column = scanner.nextLine();
                String[] coords = column.split(";");
                if (coords.length == settings.getSampledSize())
                {
                    // Loop over rows
                    for (int j = 0; j < coords.length; j++)
                    {
                        // Parse coordinates
                        String[] elements = coords[j].split(",");
                        if (elements.length == 2)
                        {
                            sampledPixelCoords[i][j] = new IntVector2(Integer.parseInt(elements[0]), Integer.parseInt(elements[1]));
                        }
                        else
                        {
                            throw new IOException("Coordinates must consist of exactly two integers");
                        }
                    }
                }
                else
                {
                    throw new IOException("Unexpected number of samples in sample locations file");
                }
            }
        }
    }


    private String getPNGFilename(int viewIndex)
    {
        String originalFilename = resources.getViewSet().getImageFileName(viewIndex);
        String originalFilenameLowercase = originalFilename.toLowerCase();
        if (originalFilenameLowercase.endsWith("png"))
        {
            return originalFilename;
        }
        else if (originalFilenameLowercase.endsWith("jpeg") || originalFilenameLowercase.endsWith("jpg")
            || originalFilenameLowercase.endsWith("tif") || originalFilenameLowercase.endsWith("tiff"))
        {
            // Change file extension to .png
            String[] filenameParts = resources.getViewSet().getImageFileName(viewIndex).split("\\.");
            filenameParts[filenameParts.length - 1] = "png";
            return String.join(".", filenameParts);
        }
        else
        {
            // Append .png
            return originalFilename + ".png";
        }
    }

    private void buildCache(Framebuffer<ContextType> fbo, ProgressMonitor monitor)
        throws IOException
    {
        SimpleLoadOptionsModel loadOptions = new SimpleLoadOptionsModel();
        loadOptions.requestColorImages(true);
        loadOptions.requestAlpha(true);
        loadOptions.requestCompression(false);
        loadOptions.requstMipmaps(false);
        loadOptions.requestDepthImages(true);

        try(ProgramObject<ContextType> texSpaceProgram = SingleCalibratedImageResource.getShaderProgramBuilder(context, resources.getViewSet(), loadOptions)
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_dynamic.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/colorappearance/projtex_single.frag"))
            .createProgram();
            Drawable<ContextType> texSpaceDrawable = resources.createDrawable(texSpaceProgram))
        {

            if (monitor != null)
            {
                monitor.setMaxProgress(resources.getViewSet().getCameraPoseCount());
            }

            // Loop over the images, processing each one at a time
            for (int k = 0; k < resources.getViewSet().getCameraPoseCount(); k++)
            {
                if (monitor != null)
                {
                    monitor.setProgress(k, MessageFormat.format("{0} ({1}/{2})", resources.getViewSet().getImageFileName(k), k, resources.getViewSet().getCameraPoseCount()));
                }

                try (SingleCalibratedImageResource<ContextType> image =
                    resources.createSingleImageResource(k,
                        resources.getViewSet().findFullResImageFile(k), // new File(originalImageDirectory, resources.viewSet.getImageFileName(k)),
                        loadOptions))
                {
                    fbo.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                    image.setupShaderProgram(texSpaceProgram);
                    texSpaceDrawable.draw(fbo);

                    String pngFilename = getPNGFilename(k);

                    // "Sampled" image to store randomly selected pixels for preliminary optimization at a lower resolution.
                    BufferedImage sampled = new BufferedImage(settings.getSampledSize(), settings.getSampledSize(), BufferedImage.TYPE_INT_ARGB);

                    int x = 0;
                    int y = 0;

                    // Generate block images
                    // Loop over "columns"
                    for (int i = 0; i < settings.getTextureSubdiv(); i++)
                    {
                        int xNext = getSettings().getBlockStartX(i + 1);

                        // xRand < round(texture width * (xSample + 1) / sampled width)
                        // xRand <= texture width * (xSample + 1) / sampled width - 0.5;
                        // xSample >= (xRand + 0.5) * sampled width / texture width - 1
                        // When i = 0: x = 0, xSampleStart should be 0 [ceil(0.5 * sampled width / texture width) - 1 = 1 - 1]
                        // -------
                        // xRand >= round(texture width * xSample / sampled width)
                        // xRand > texture width * xSample / sampled width - 0.5;
                        // xSample < (xRand + 0.5) * sampled width / texture width
                        // xRand < xNext, therefore:
                        // xSample < (xNext - 0.5) * sampled width / texture width
                        // When i = sampled width - 1: xNext = texture width, xSampleEnd should be sampled width
                        // [ceil((texture width - 0.5) * sampled width / texture width) = ceil(sampled width - 0.5 * sampled width / textured width)]
                        int xSampleStart = (int) Math.ceil((x + 0.5) * (double) settings.getSampledSize() / (double) settings.getTextureWidth()) - 1;
                        int xSampleEnd = (int) Math.ceil((xNext - 0.5) * (double) settings.getSampledSize() / (double) settings.getTextureWidth());

                        // Loop over "rows"
                        for (int j = 0; j < settings.getTextureSubdiv(); j++)
                        {
                            int yNext = getSettings().getBlockStartY(j + 1);

                            int width = xNext - x;
                            int height = yNext - y;

                            // Read pixels from the framebuffer
                            int[] block = fbo.getTextureReaderForColorAttachment(0).readARGB(x, y, width, height);
                            BufferedImage blockImage = BufferedImageBuilder.build()
                                .setDataFromArray(block, width, height)
                                .flipVertical()
                                .create();
//                            blockImage.setRGB(0, 0, width, height, block, 0, width);

                            // Write the block image out to disk
                            ImageIO.write(blockImage, "PNG",
                                new File(new File(settings.getCacheDirectory(), String.format("%d_%d", i, j)), pngFilename));

                            // See derivations for x above
                            int ySampleStart = (int) Math.ceil((y + 0.5) * (double) settings.getSampledSize() / (double) settings.getTextureHeight()) - 1;
                            int ySampleEnd = (int) Math.ceil((yNext - 0.5) * (double) settings.getSampledSize() / (double) settings.getTextureHeight());

                            // Fill in any pixels in the "sampled" image that come from this block, using the pixel coordinates selected randomly earlier.
                            for (int xSample = xSampleStart; xSample < xSampleEnd; xSample++)
                            {
                                for (int ySample = ySampleStart; ySample < ySampleEnd; ySample++)
                                {
                                    // Copy the randomly selected pixel into the sampled image.
                                    IntVector2 coords = sampledPixelCoords[xSample][ySample];

                                    // due to randomness, no guarantee that all coords are in this block
                                    if (coords.x >= x && coords.x < xNext && coords.y >= y && coords.y < yNext)
                                    {
                                        // Make sure to account for the start of the block to avoid out-of-bounds indices after the first block
                                        // Need to flip both the source and destination y-components (since the block image is already flipped,
                                        // our coordinates are in non-flipped space, but we ultimately do want the sampled image to also be flipped).
                                        sampled.setRGB(xSample, settings.getSampledSize() - ySample - 1,
                                            blockImage.getRGB(coords.x - x, height - (coords.y - y) - 1));
                                    }
                                }
                            }

                            // Advance to the next row
                            y = yNext;
                        }

                        // Advance to the next column
                        x = xNext;
                        y = 0;
                    }

                    ImageIO.write(sampled, "PNG", new File(sampledDir, pngFilename));
                }
            }

            if (monitor != null)
            {
                monitor.setProgress(resources.getViewSet().getCameraPoseCount(), "All images completed.");
            }
        }
    }

    public IBRResourcesTextureSpace<ContextType> createSampledResources() throws IOException
    {
        TextureLoadOptions loadOptions = new TextureLoadOptions();
        loadOptions.setLinearFilteringRequested(false);
        loadOptions.setMipmapsRequested(false);
        loadOptions.setCompressionRequested(false);

        try
        {
            return new IBRResourcesTextureSpace<>(resources.getSharedResources(), this::createSampledGeometryTextures,
                sampledDir, loadOptions, settings.getSampledSize(), settings.getSampledSize(),
                new DefaultProgressMonitor() // simple progress monitor for logging; will not be shown in the UI
                {
                    private double maxProgress = 0.0;

                    @Override
                    public void setMaxProgress(double maxProgress)
                    {
                        this.maxProgress = maxProgress;
                    }

                    @Override
                    public void setProgress(double progress, String message)
                    {
                        log.info("[{}%] {}", new DecimalFormat("#.##").format(progress / maxProgress * 100), message);
                    }
                });
        }
        catch (IOException e)
        {
            log.warn("Incomplete cache; will try to rebuild.");

            // Try to reinitialize in case the cache was only partially complete.
            this.initialize(null); // no loading monitor for this edge case

            // If initialize() completed without exceptions, then createSampledResources() should work now.
            return createSampledResources();
        }
    }

    /**
     *
     * @return A resource (that itself needs to be closed with no longer needed) which in turn creates resources for specific blocks
     */
    public TextureBlockResourceFactory<ContextType> createBlockResourceFactory()
    {
        return new TextureBlockResourceFactory<>(resources.getSharedResources(), this);
    }

    private int getHighResIndexForSample(int sampleIndex)
    {
        // Figure out which pixels in the high-res image are being used in the sampled image
        IntVector2 highResCoords = sampledPixelCoords[sampleIndex % settings.getSampledSize()][sampleIndex / settings.getSampledSize()];
        return highResCoords.x + highResCoords.y * settings.getTextureWidth();
    }

    private ReadonlyNativeVectorBuffer sampleHighResBuffer(ReadonlyNativeVectorBuffer highResBuffer)
    {
        NativeVectorBuffer sampledBuffer = NativeVectorBufferFactory.getInstance()
            .createEmpty(NativeDataType.FLOAT, 3, settings.getSampledSize() * settings.getSampledSize());
        for (int i = 0; i < sampledBuffer.getCount(); i++)
        {
            int highResIndex = getHighResIndexForSample(i);
            sampledBuffer.set(i, 0, highResBuffer.get(highResIndex, 0));
            sampledBuffer.set(i, 1, highResBuffer.get(highResIndex, 1));
            sampledBuffer.set(i, 2, highResBuffer.get(highResIndex, 2));
        }
        return sampledBuffer;
    }

    private GeometryTextures<ContextType> createSampledGeometryTextures()
    {
        try(GeometryFramebuffer<ContextType> geomTexturesFullRes =
            this.resources.getGeometryResources().createGeometryFramebuffer(settings.getTextureWidth(), settings.getTextureHeight()))
        {
            // Use non-rendered since we need to sample on the CPU (where the sample coordinates live) and then pass the data back to the GPU.
            GeometryTextures<ContextType> sampledGeometryTextures =
                GeometryFramebuffer.createEmpty(context, settings.getSampledSize(), settings.getSampledSize());

            // Sample position buffer
            Framebuffer<ContextType> contextTypeFramebuffer2 = geomTexturesFullRes.getFramebuffer();
            sampledGeometryTextures.getPositionTexture().load(
                sampleHighResBuffer(NativeVectorBufferFactory.getInstance()
                    .createFromFloatArray(4, settings.getTextureWidth() * settings.getTextureHeight(),
                        contextTypeFramebuffer2.getTextureReaderForColorAttachment(0).readFloatingPointRGBA())));

            // Sample normal buffer
            Framebuffer<ContextType> contextTypeFramebuffer1 = geomTexturesFullRes.getFramebuffer();
            sampledGeometryTextures.getNormalTexture().load(
                sampleHighResBuffer(NativeVectorBufferFactory.getInstance()
                    .createFromFloatArray(4, settings.getTextureWidth() * settings.getTextureHeight(),
                        contextTypeFramebuffer1.getTextureReaderForColorAttachment(1).readFloatingPointRGBA())));

            // Sample tangent buffer
            Framebuffer<ContextType> contextTypeFramebuffer = geomTexturesFullRes.getFramebuffer();
            sampledGeometryTextures.getTangentTexture().load(
                sampleHighResBuffer(NativeVectorBufferFactory.getInstance()
                    .createFromFloatArray(4, settings.getTextureWidth() * settings.getTextureHeight(),
                        contextTypeFramebuffer.getTextureReaderForColorAttachment(2).readFloatingPointRGBA())));

            return sampledGeometryTextures;
        }
    }
}
