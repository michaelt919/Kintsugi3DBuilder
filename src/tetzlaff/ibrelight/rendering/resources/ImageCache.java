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

import javax.imageio.ImageIO;

import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.SimpleLoadOptionsModel;

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

        try(
            Program<ContextType> texSpaceProgram = SingleCalibratedImageResource.getShaderProgramBuilder(context, resources.viewSet, loadOptions)
                .addShader(ShaderType.VERTEX, new File("shaders/common/texspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/colorappearance/projtex_single.frag"))
                .createProgram();
            FramebufferObject<ContextType> fbo = context.buildFramebufferObject(settings.getTextureWidth(), settings.getTextureHeight())
                .addColorAttachment(ColorFormat.RGBA8)
                .createFramebufferObject()
        )
        {
            Drawable<ContextType> texSpaceDrawable = resources.createDrawable(texSpaceProgram);

            Random random = new Random();

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

                    // "Sampled" image to store randomly selected pixels for preliminary optimization at a lower resolution.
                    BufferedImage sampled = new BufferedImage(settings.getSampledSize(), settings.getSampledSize(), BufferedImage.TYPE_INT_ARGB);

                    int x = 0;
                    int y = 0;

                    // Generate chunk images
                    // Loop over "columns"
                    for (int i = 0; i < settings.getTextureSubdiv(); i++)
                    {
                        int xNext = (int)Math.round((double)(i + 1) * (double)settings.getTextureWidth() / (double)settings.getTextureSubdiv());
                        int xChunkRange = xNext - x;

                        // Calculate the x-bounds for picking random pixels for the "sampled" image
                        int xSampleStart = (int)Math.round((double)x * (double)settings.getSampledSize() / (double)settings.getTextureWidth());
                        int xSampleEnd = (int)Math.round((double)xNext * (double)settings.getSampledSize() / (double)settings.getTextureWidth());
                        int xSampleRange = xSampleEnd - xSampleStart;

                        // Loop over "rows"
                        for (int j = 0; j < settings.getTextureSubdiv(); j++)
                        {
                            int yNext = (int)Math.round((double)(j + 1) * (double)settings.getTextureHeight() / (double)settings.getTextureSubdiv());
                            int yChunkRange = yNext - y;

                            int width = xNext - x;
                            int height = yNext - y;

                            // Read pixels from the framebuffer
                            int[] chunk = fbo.readColorBufferARGB(0, x, y, width, height);
                            BufferedImage chunkImage = new BufferedImage(settings.getSampledSize(), settings.getSampledSize(), BufferedImage.TYPE_INT_ARGB);
                            sampled.setRGB(0, 0, width, height, chunk, 0, width);

                            // Write the chunk image out to disk
                            ImageIO.write(chunkImage, "PNG",
                                new File(new File(settings.getCacheDirectory(), String.format("%d_%d", i, j)), resources.viewSet.getImageFileName(k)));

                            // Calculate the y-bounds for picking random pixels for the "sampled" image
                            int ySampleStart = (int)Math.round((double)y * (double)settings.getSampledSize() / (double)settings.getTextureHeight());
                            int ySampleEnd = (int)Math.round((double)yNext * (double)settings.getSampledSize() / (double)settings.getTextureHeight());
                            int ySampleRange = ySampleEnd - ySampleStart;

                            // Fill in any pixels in the "sampled" image that come from this chunk, by randomly selecting pixels.
                            for (int xSample = xSampleStart; xSample < xSampleEnd; xSample++)
                            {
                                int xRandMin = x + (int)Math.round((double)xChunkRange * (double)(xSample - xSampleStart) / (double)xSampleRange);
                                int xRandMax = x + (int)Math.round((double)xChunkRange * (double)(xSample + 1 - xSampleStart) / (double)xSampleRange);

                                for (int ySample = ySampleStart; ySample < ySampleEnd; ySample++)
                                {
                                    int xRand = random.nextInt(xRandMax - xRandMin) + xRandMin;

                                    int yRandMin = y + (int)Math.round((double)yChunkRange * (double)(ySample - ySampleStart) / (double)ySampleRange);
                                    int yRandMax = y + (int)Math.round((double)yChunkRange * (double)(ySample + 1 - ySampleStart) / (double)ySampleRange);
                                    int yRand = random.nextInt(yRandMax - yRandMin) + yRandMin;

                                    // Copy the randomly selected pixel into the sampled image.
                                    sampled.setRGB(xSample, ySample, chunkImage.getRGB(xRand, yRand));
                                }
                            }

                            // Advance to the next row
                            y = yNext;
                        }

                        // Advance to the next column
                        x = xNext;
                        y = 0;
                    }

                    ImageIO.write(sampled, "PNG", new File(sampledDir, resources.viewSet.getImageFileName(k)));
                }
            }
        }
    }
}
