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

package tetzlaff.ibrelight.export.specularfit;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.rendering.resources.IBRResources;
import tetzlaff.util.ColorArrayList;
import tetzlaff.optimization.KMeansClustering;

public class SpecularFitInitializer<ContextType extends Context<ContextType>>
{
    private final IBRResources<ContextType> resources;
    private final SpecularFitSettings settings;

    public SpecularFitInitializer(IBRResources<ContextType> resources, SpecularFitSettings settings)
    {
        this.resources = resources;
        this.settings = settings;
    }

    private Program<ContextType> createAverageProgram() throws FileNotFoundException
    {
        return new SpecularFitProgramFactory<>(resources, settings).createProgram(
            new File("shaders/common/texspace_noscale.vert"),
            new File("shaders/specularfit/average.frag"));
    }

    public void initialize(SpecularFitSolution solution)
    {
        try (Program<ContextType> averageProgram = createAverageProgram();
            FramebufferObject<ContextType> framebuffer =
                resources.context.buildFramebufferObject(settings.width, settings.height)
                    .addColorAttachment(ColorFormat.RGBA32F)
                    .createFramebufferObject())
        {
            Drawable<ContextType> drawable = resources.createDrawable(averageProgram);

            System.out.println("Clustering to initialize weights...");

            // Clear framebuffer
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

            // Run shader program to fill framebuffer with per-pixel information.
            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

            if (SpecularOptimization.DEBUG)
            {
                try
                {
                    framebuffer.saveColorBufferToFile(0, "PNG", new File(settings.outputDirectory, "average.png"));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            float[] averages = framebuffer.readFloatingPointColorBufferRGBA(0);

            List<Vector3> centers = new KMeansClustering(new ColorArrayList(averages)).makeClusters(solution.getWeightsList());

            // Initialize weight validity.
            IntStream.range(0, averages.length / 4)
                .filter(p -> averages[4 * p + 3] > 0.0)
                .forEach(p -> solution.setWeightsValidity(p, true));

            // Output for debugging
            System.out.println("Refined centers:");
            for (int b = 0; b < settings.basisCount; b++)
            {
                System.out.println(centers.get(b));
            }

            if (SpecularOptimization.DEBUG)
            {
                BufferedImage weightImg = new BufferedImage(settings.width, settings.height, BufferedImage.TYPE_INT_ARGB);
                int[] weightDataPacked = new int[settings.width * settings.height];
                for (int p = 0; p < settings.width * settings.height; p++)
                {
                    if (averages[4 * p + 3] > 0.0)
                    {
                        int bSelect = -1;

                        for (int b = 0; b < settings.basisCount; b++)
                        {
                            if (solution.getWeights(p).get(b) > 0)
                            {
                                bSelect = b;
                            }
                        }

                        // Flip vertically
                        int weightDataIndex = p % settings.width + settings.width * (settings.height - p / settings.width - 1);

                        switch (bSelect)
                        {
                            case 0:
                                weightDataPacked[weightDataIndex] = Color.RED.getRGB();
                                break;
                            case 1:
                                weightDataPacked[weightDataIndex] = Color.GREEN.getRGB();
                                break;
                            case 2:
                                weightDataPacked[weightDataIndex] = Color.BLUE.getRGB();
                                break;
                            case 3:
                                weightDataPacked[weightDataIndex] = Color.YELLOW.getRGB();
                                break;
                            case 4:
                                weightDataPacked[weightDataIndex] = Color.CYAN.getRGB();
                                break;
                            case 5:
                                weightDataPacked[weightDataIndex] = Color.MAGENTA.getRGB();
                                break;
                            case 6:
                                weightDataPacked[weightDataIndex] = Color.WHITE.getRGB();
                                break;
                            case 7:
                                weightDataPacked[weightDataIndex] = Color.GRAY.getRGB();
                                break;
                            default:
                                weightDataPacked[weightDataIndex] = Color.BLACK.getRGB();
                                break;
                        }
                    }
                }

                weightImg.setRGB(0, 0, weightImg.getWidth(), weightImg.getHeight(), weightDataPacked, 0, weightImg.getWidth());

                try
                {
                    ImageIO.write(weightImg, "PNG", new File(settings.outputDirectory, "k-means.png"));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }
}
