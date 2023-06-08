/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.rendering.resources;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.vecmath.IntVector2;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibrelight.core.SimpleLoadOptionsModel;
import tetzlaff.util.BufferedImageBuilder;

public class ImageCache<ContextType extends Context<ContextType>>
{
    private final ContextType context;
    private final IBRResources<ContextType> resources;
    private final ImageCacheSettings settings;

    private final File sampledDir;
    private final IntVector2[][] sampledPixelCoords;

    ImageCache(IBRResources<ContextType> resources, ImageCacheSettings settings)
    {
        this.context = resources.context;
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
            }
            catch (IOException | RuntimeException e)
            {
                e.printStackTrace();

                // Reset in case it partially succeeded.
                Arrays.stream(this.sampledPixelCoords).forEach(column -> Arrays.fill(column, null));
            }
        }
    }

    private File getChunkDir(int i, int j)
    {
        return new File(settings.getCacheDirectory(), String.format("%d_%d", i, j));
    }

    /**
     * Initializes the cache by loading each image one at a time and writing out texture space chunks as well as a "sampled" low-res texture for each image
     * As a side effect of this method, the context's state will have back face culling enabled,
     * @throws IOException
     */
    public void initialize(File originalImageDirectory) throws IOException
    {
        // Create directories to organize the cache
        sampledDir.mkdirs();

        // Make sure backface culling is disabled
        context.getState().disableBackFaceCulling();

        for (int i = 0; i < settings.getTextureSubdiv(); i++)
        {
            for (int j = 0; j < settings.getTextureSubdiv(); j++)
            {
                getChunkDir(i, j).mkdirs();
            }
        }

        try(FramebufferObject<ContextType> fbo = context.buildFramebufferObject(settings.getTextureWidth(), settings.getTextureHeight())
                .addColorAttachment(ColorFormat.RGBA8)
                .createFramebufferObject())
        {
            selectSampleLocations(fbo);
            buildCache(fbo);
        }
    }

    private void selectSampleLocations(Framebuffer<ContextType> fbo) throws IOException
    {

        try (Program<ContextType> maskProgram = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/common/solid.frag"))
            .createProgram())
        {
            Drawable<ContextType> maskDrawable = resources.createDrawable(maskProgram);
            fbo.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
            maskProgram.setUniform("color", new Vector4(1.0f));
            maskDrawable.draw(fbo);

            // Debugging
            fbo.saveColorBufferToFile(0, "PNG", new File(settings.getCacheDirectory(), "debug.png"));

            int[] mask = fbo.readColorBufferARGB(0);
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
        return new File(settings.getCacheDirectory(), "sampleLocations.csv");
    }

    private void writeSampleLocationsToFile() throws FileNotFoundException
    {
        try(PrintStream out = new PrintStream(getSampleLocationsFile()))
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
        try(Scanner scanner = new Scanner(getSampleLocationsFile()))
        {
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
                        String[] elements = coords[i].split(",");
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
        // Change file extension to .png
        String[] filenameParts = resources.viewSet.getImageFileName(viewIndex).split("\\.");
        filenameParts[filenameParts.length - 1] = "png";
        return String.join(".", filenameParts);
    }

    private void buildCache(Framebuffer<ContextType> fbo)
        throws IOException
    {
        SimpleLoadOptionsModel loadOptions = new SimpleLoadOptionsModel();
        loadOptions.setColorImagesRequested(true);
        loadOptions.setAlphaRequested(true);
        loadOptions.setCompressionRequested(false);
        loadOptions.setMipmapsRequested(false);
        loadOptions.setDepthImagesRequested(true);

        try(Program<ContextType> texSpaceProgram = SingleCalibratedImageResource.getShaderProgramBuilder(context, resources.viewSet, loadOptions)
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/colorappearance/projtex_single.frag"))
            .createProgram())
        {
            Drawable<ContextType> texSpaceDrawable = resources.createDrawable(texSpaceProgram);

            // Loop over the images, processing each one at a time
            for (int k = 0; k < resources.viewSet.getCameraPoseCount(); k++)
            {
                try (SingleCalibratedImageResource<ContextType> image =
                    resources.createSingleImageResource(k,
                        // TODO: use specified originalImageDirectory; need to handle search for different file extensions; jpg/png/tiff/etc.
                        resources.findImageFile(k), // new File(originalImageDirectory, resources.viewSet.getImageFileName(k)),
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

                    // Generate chunk images
                    // Loop over "columns"
                    for (int i = 0; i < settings.getTextureSubdiv(); i++)
                    {
                        int xNext = (int) Math.round((double) (i + 1) * (double) settings.getTextureWidth() / (double) settings.getTextureSubdiv());

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
                            int yNext = (int) Math.round((double) (j + 1) * (double) settings.getTextureHeight() / (double) settings.getTextureSubdiv());

                            int width = xNext - x;
                            int height = yNext - y;

                            // Read pixels from the framebuffer
                            int[] chunk = fbo.readColorBufferARGB(0, x, y, width, height);
                            BufferedImage chunkImage = BufferedImageBuilder.build()
                                .setDataFromArray(chunk, width, height)
                                .flipVertical()
                                .create();
                            chunkImage.setRGB(0, 0, width, height, chunk, 0, width);

                            // Write the chunk image out to disk
                            ImageIO.write(chunkImage, "PNG",
                                new File(new File(settings.getCacheDirectory(), String.format("%d_%d", i, j)), pngFilename));

                            // See derivations for x above
                            int ySampleStart = (int) Math.ceil((double) (y + 0.5) * (double) settings.getSampledSize() / (double) settings.getTextureHeight()) - 1;
                            int ySampleEnd = (int) Math.ceil((double) (yNext - 0.5) * (double) settings.getSampledSize() / (double) settings.getTextureHeight());

                            // Fill in any pixels in the "sampled" image that come from this chunk, using the pixel coordinates selected randomly earlier.
                            for (int xSample = xSampleStart; xSample < xSampleEnd; xSample++)
                            {
                                for (int ySample = ySampleStart; ySample < ySampleEnd; ySample++)
                                {
                                    // Copy the randomly selected pixel into the sampled image.
                                    IntVector2 coords = sampledPixelCoords[xSample][ySample];

                                    // due to randomness, no guarantee that all coords are in this chunk
                                    if (coords.x >= x && coords.x < xNext && coords.y >= y && coords.y < yNext)
                                    {
                                        // Make sure to account for the start of the chunk to avoid out-of-bounds indices after the first chunk
                                        sampled.setRGB(xSample, ySample, chunkImage.getRGB(coords.x - x, coords.y - y));
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
        }
    }

    public Texture3D<ContextType> createSampledTextureArray() throws IOException
    {
        Texture3D<ContextType> textureArray = context.getTextureFactory()
            .build2DColorTextureArray(settings.getSampledSize(), settings.getSampledSize(), resources.viewSet.getCameraPoseCount())
            .setLinearFilteringEnabled(false)
            .setMipmapsEnabled(false)
            .setInternalFormat(ColorFormat.RGBA32F)
            .createTexture();

        // Framebuffer to calculate incident radiance at full resolution
        try
        (
            Program<ContextType> radianceProgram = resources.getIBRShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, "shaders/common/texspace_noscale.vert")
                .addShader(ShaderType.FRAGMENT, "shaders/specularfit/incidentRadiance.frag")
                .createProgram();
            FramebufferObject<ContextType> highResFBO = context.buildFramebufferObject(settings.getTextureWidth(), settings.getTextureHeight())
                .addColorAttachment(ColorFormat.RGB32F)
                .createFramebufferObject();
            FramebufferObject<ContextType> sampledResFBO = context.buildFramebufferObject(settings.getTextureWidth(), settings.getTextureHeight())
                // No attachments as we'll just attach the layers of the texture array.
                .createFramebufferObject()
        )
        {
            Drawable<ContextType> radianceDrawable = resources.createDrawable(radianceProgram);
            for (int k = 0; k < resources.viewSet.getCameraPoseCount(); k++)
            {
                radianceProgram.setUniform("viewIndex", k);
                highResFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                radianceDrawable.draw(highResFBO);
                float[] radiance = highResFBO.readFloatingPointColorBufferRGBA(0);

                BufferedImage sampledImg = BufferedImageBuilder.build()
                    .loadDataFromFile(new File(sampledDir, getPNGFilename(k)))
                    .flipVertical()
                    .create();
                int[] sampledImgData = sampledImg.getRGB(0, 0, sampledImg.getWidth(), sampledImg.getHeight(), null, 0, sampledImg.getWidth());
                NativeVectorBuffer finalData = NativeVectorBufferFactory.getInstance()
                    .createEmpty(NativeDataType.FLOAT, 4, settings.getSampledSize() * settings.getSampledSize());
                for (int i = 0; i < finalData.getCount(); i++)
                {
                    Color sampledImgPixel = new Color(sampledImgData[i], true);
                    IntVector2 highResCoords = sampledPixelCoords[i % settings.getSampledSize()][i / settings.getSampledSize()];
                    int highResIndex = highResCoords.x + highResCoords.y * settings.getTextureWidth();
                    finalData.set(i, 0, sampledImgPixel.getRed() / (255.0f * radiance[4 * highResIndex]));
                    finalData.set(i, 1, sampledImgPixel.getGreen() / (255.0f * radiance[4 * highResIndex + 1]));
                    finalData.set(i, 2, sampledImgPixel.getBlue() / (255.0f * radiance[4 * highResIndex + 2]));
                    finalData.set(i, 3, sampledImgPixel.getAlpha() / 255.0f);
                }

                textureArray.loadLayer(k, finalData);
            }

            return textureArray;
        }
    }
}
