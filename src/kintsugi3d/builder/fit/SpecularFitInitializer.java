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

package kintsugi3d.builder.fit;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import kintsugi3d.builder.fit.decomposition.SpecularDecomposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;
import kintsugi3d.util.ColorArrayList;
import kintsugi3d.optimization.KMeansClustering;

public class SpecularFitInitializer<ContextType extends Context<ContextType>>
{
    private static final Logger log = LoggerFactory.getLogger(SpecularFitInitializer.class);
    private final ReadonlyIBRResources<ContextType> resources;
    private final SpecularBasisSettings specularBasisSettings;

    public SpecularFitInitializer(ReadonlyIBRResources<ContextType> resources, SpecularBasisSettings specularBasisSettings)
    {
        this.resources = resources;
        this.specularBasisSettings = specularBasisSettings;
    }

    private ProgramObject<ContextType> createAverageProgram(SpecularFitProgramFactory<ContextType> programFactory) throws FileNotFoundException
    {
        return programFactory.createProgram(resources,
            new File("shaders/common/texspace_dynamic.vert"),
            new File("shaders/specularfit/average.frag"));
    }

    public void initialize(SpecularFitProgramFactory<ContextType> programFactory, SpecularDecomposition solution)
    {
        try (ProgramObject<ContextType> averageProgram = createAverageProgram(programFactory);
             FramebufferObject<ContextType> framebuffer =
                resources.getContext().buildFramebufferObject(solution.getTextureResolution().width, solution.getTextureResolution().height)
                    .addColorAttachment(ColorFormat.RGBA32F)
                    .createFramebufferObject())
        {
            Drawable<ContextType> drawable = resources.createDrawable(averageProgram);

            log.info("Clustering to initialize weights...");

            // Clear framebuffer
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

            // Run shader program to fill framebuffer with per-pixel information.
            drawable.draw(framebuffer);

//            if (SpecularOptimization.DEBUG)
//            {
//                try
//                {
//                    framebuffer.saveColorBufferToFile(0, "PNG", new File(outputDirectory, "average.png"));
//                }
//                catch (IOException e)
//                {
//                    e.printStackTrace();
//                }
//            }

            float[] averages = framebuffer.getTextureReaderForColorAttachment(0).readFloatingPointRGBA();

            List<Vector3> centers = new KMeansClustering(new ColorArrayList(averages)).makeClusters(solution.getWeightsList());

            // Initialize weight validity.
            IntStream.range(0, averages.length / 4)
                .filter(p -> averages[4 * p + 3] > 0.0)
                .forEach(p -> solution.setWeightsValidity(p, true));

            // Output for debugging
            log.info("Refined centers:");
            for (int b = 0; b < specularBasisSettings.getBasisCount(); b++)
            {
                log.info(centers.get(b).toString());
            }
        }
        catch (FileNotFoundException e)
        {
            log.error("Error occurred while initializing specular fit:", e);
        }
    }

    public void saveDebugImage(SpecularDecomposition solution, File outputDirectory)
    {
        int width = solution.getTextureResolution().width;
        int height = solution.getTextureResolution().height;

        BufferedImage weightImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] weightDataPacked = new int[width * height];
        for (int p = 0; p < width * height; p++)
        {
            if (solution.areWeightsValid(p))
            {
                int bSelect = -1;

                for (int b = 0; b < specularBasisSettings.getBasisCount(); b++)
                {
                    if (solution.getWeights(p).get(b) > 0)
                    {
                        bSelect = b;
                    }
                }

                // Flip vertically
                int weightDataIndex = p % width + width * (height - p / width - 1);

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
            ImageIO.write(weightImg, "PNG", new File(outputDirectory, "k-means.png"));
        }
        catch (IOException e)
        {
            log.error("An error occurred saving debug image:", e);
        }
    }
}
