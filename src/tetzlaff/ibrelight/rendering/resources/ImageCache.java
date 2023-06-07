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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.IntVector2;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibrelight.core.SimpleLoadOptionsModel;
import tetzlaff.util.BufferedImageBuilder;

public class ImageCache<ContextType extends Context<ContextType>>
{
    private final ContextType context;
    private final IBRResources<ContextType> resources;
    private final File originalImageDirectory;
    private final ImageCacheSettings settings;

    ImageCache(IBRResources<ContextType> resources, File originalImageDirectory, ImageCacheSettings settings)
    {
        this.context = resources.context;
        this.resources = resources;
        this.originalImageDirectory = originalImageDirectory;
        this.settings = settings;
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
    public void initialize() throws IOException
    {
        // Create directories to organize the cache
        File sampledDir = new File(settings.getCacheDirectory(), "sampled");
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

        SimpleLoadOptionsModel loadOptions = new SimpleLoadOptionsModel();
        loadOptions.setColorImagesRequested(true);
        loadOptions.setAlphaRequested(true);
        loadOptions.setCompressionRequested(false);
        loadOptions.setMipmapsRequested(false);
        loadOptions.setDepthImagesRequested(true);

        try(FramebufferObject<ContextType> fbo = context.buildFramebufferObject(settings.getTextureWidth(), settings.getTextureHeight())
                .addColorAttachment(ColorFormat.RGBA8)
                .createFramebufferObject())
        {
            // Square 2D array to store sampled pixel coords.
            IntVector2[][] sampledPixelCoords = IntStream.range(0, settings.getSampledSize())
                .mapToObj(i -> new IntVector2[settings.getSampledSize()])
                .toArray(IntVector2[][]::new);

            try(Program<ContextType> maskProgram = context.getShaderProgramBuilder()
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
                    int xRandMin = (int)Math.round((double)settings.getTextureWidth() * (double)xSample / (double)settings.getSampledSize());
                    int xRandMax = (int)Math.round((double)settings.getTextureWidth() * (double)(xSample + 1) / (double)settings.getSampledSize());

                    for (int ySample = 0; ySample < settings.getSampledSize(); ySample++)
                    {
                        int xRand;
                        int yRand;
                        int attempts = 0;

                        do
                        {
                            xRand = random.nextInt(xRandMax - xRandMin) + xRandMin;

                            int yRandMin = (int)Math.round((double)settings.getTextureHeight() * (double)ySample / (double)settings.getSampledSize());
                            int yRandMax = (int)Math.round((double)settings.getTextureHeight() * (double)(ySample + 1) / (double)settings.getSampledSize());
                            yRand = random.nextInt(yRandMax - yRandMin) + yRandMin;
                            attempts++;
                        }
                        while  (attempts < maxAttempts && maskImg.getRGB(xRand, yRand) == 0);
                        // keep repeating until the mask indicates a valid pixel, or until too many attempts

                        // Store the pixel coordinates in the 2D array to reference later.
                        sampledPixelCoords[xSample][ySample] = new IntVector2(xRand, yRand);
                    }
                }
            }

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
                        // Change file extension to .png
                        String[] filenameParts = resources.viewSet.getImageFileName(k).split("\\.");
                        filenameParts[filenameParts.length - 1] = "png";
                        String pngFilename = String.join(".", filenameParts);

                        fbo.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                        image.setupShaderProgram(texSpaceProgram);
                        texSpaceDrawable.draw(fbo);

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
    }
}
