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

package kintsugi3d.builder.fit;

import kintsugi3d.builder.fit.decomposition.SpecularDecomposition;
import kintsugi3d.builder.fit.settings.BasisSettings;
import kintsugi3d.builder.resources.project.ReadonlyGraphicsResources;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.optimization.KMeansClustering;
import kintsugi3d.util.ColorArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

public class SpecularFitInitializer<ContextType extends Context<ContextType>>
{
    private static final Logger LOG = LoggerFactory.getLogger(SpecularFitInitializer.class);
    private final ReadonlyGraphicsResources<ContextType> resources;
    private final BasisSettings basisSettings;

    public SpecularFitInitializer(ReadonlyGraphicsResources<ContextType> resources, BasisSettings basisSettings)
    {
        this.resources = resources;
        this.basisSettings = basisSettings;
    }

    private ProgramObject<ContextType> createAverageProgram(SpecularFitProgramFactory<ContextType> programFactory) throws IOException
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
                    .createFramebufferObject();
            Drawable<ContextType> drawable = resources.createDrawable(averageProgram))
        {

            LOG.info("Clustering to initialize weights...");

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
            LOG.info("Refined centers:");
            for (int b = 0; b < basisSettings.getBasisCount(); b++)
            {
                LOG.info(centers.get(b).toString());
            }
        }
        catch (IOException e)
        {
            LOG.error("Error occurred while initializing specular fit:", e);
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

                for (int b = 0; b < basisSettings.getBasisCount(); b++)
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
            LOG.error("An error occurred saving debug image:", e);
        }
    }
}
