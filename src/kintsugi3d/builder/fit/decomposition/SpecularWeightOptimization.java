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

package kintsugi3d.builder.fit.decomposition;

import java.util.Collections;

import kintsugi3d.builder.fit.ReflectanceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.builder.resources.ibr.stream.GraphicsStream;
import kintsugi3d.optimization.NonNegativeWeightOptimization;

public class SpecularWeightOptimization
{
    private static final Logger log = LoggerFactory.getLogger(SpecularWeightOptimization.class);
    private final NonNegativeWeightOptimization base;
    private final TextureResolution textureResolution;
    private final SpecularBasisSettings specularBasisSettings;

    private final int weightBlockSize;

    public SpecularWeightOptimization(TextureResolution textureResolution, SpecularBasisSettings specularBasisSettings,
          int weightBlockSize)
    {
        this.textureResolution = textureResolution;
        this.specularBasisSettings = specularBasisSettings;
        this.weightBlockSize = weightBlockSize;
        base = new NonNegativeWeightOptimization(weightBlockSize, this.specularBasisSettings.getBasisCount(),
            Collections.singletonList(b -> 1.0), Collections.singletonList(1.0)); // Equality constraint to ensure that the weights sum up to 1.0.
    }

    public SpecularWeightOptimization(TextureResolution textureResolution, SpecularBasisSettings specularBasisSettings)
    {
        // Default weight block size (only one weight block)
        this(textureResolution, specularBasisSettings, textureResolution.width * textureResolution.height);
    }

    public int getWeightBlockSize()
    {
        return weightBlockSize;
    }

    public void execute(GraphicsStream<ReflectanceData> viewStream, SpecularDecomposition solution, int pStart)
    {
        log.info("Building weight fitting matrices...");

        // Setup all the matrices for fitting weights (one per texel)
        base.buildMatrices(viewStream, new SpecularWeightModel(solution, this.specularBasisSettings),
            // If a pixel is valid in some view, mark it as such in the solution.
            p -> solution.setWeightsValidity(p, true),
            pStart, Math.min(pStart + weightBlockSize, textureResolution.width * textureResolution.height));

        // Dampen so that it doesn't "snap" to the optimal solution right away.
        // TODO expose the damping factor as a setting.
//        base.dampenWithPreviousSolution(1.0, p -> b -> solution.getWeights(pStart + p).get(b));

        log.info("Finished building matrices; solving now...");

        // Optimize the weights and store the result in the SpecularDecomposition.
        if (pStart + weightBlockSize > textureResolution.width * textureResolution.height)
        {
            base.optimizeWeights(p -> solution.areWeightsValid(pStart + p),
                (p, weights) ->
                {
                    solution.setWeights(pStart + p, weights);
//                    solution.setWeights(pStart + p,
//                        weights.extractMatrix(0, weights.numRows() - 1, 0, 1).scale(0.5)
//                            .plus(solution.getWeights(pStart + p).scale(0.5)));
                },
                NonNegativeWeightOptimization.DEFAULT_TOLERANCE_SCALE, textureResolution.width * textureResolution.height - pStart);
        }
        else
        {
            base.optimizeWeights(p -> solution.areWeightsValid(pStart + p),
                (p, weights) ->
                {
                    solution.setWeights(pStart + p, weights);
//                    solution.setWeights(pStart + p,
//                        weights.extractMatrix(0, weights.numRows() - 1, 0, 1).scale(0.5)
//                            .plus(solution.getWeights(pStart + p).scale(0.5)));
                });
        }

        log.info("DONE!");
    }

    public void execute(GraphicsStream<ReflectanceData> viewStream, SpecularDecomposition solution)
    {
        // Start at p=0 by default
        execute(viewStream, solution, 0);
    }
}