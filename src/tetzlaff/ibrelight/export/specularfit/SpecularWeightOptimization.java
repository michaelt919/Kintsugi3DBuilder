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

import java.util.Collections;

import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.rendering.resources.GraphicsStream;
import tetzlaff.optimization.NonNegativeWeightOptimization;

public class SpecularWeightOptimization
{

    private final NonNegativeWeightOptimization base;
    private final TextureFitSettings textureFitSettings;
    private final SpecularBasisSettings specularBasisSettings;
    private final int weightBlockSize;

    public SpecularWeightOptimization(TextureFitSettings textureFitSettings, SpecularBasisSettings specularBasisSettings,
          int weightBlockSize)
    {
        this.textureFitSettings = textureFitSettings;
        this.specularBasisSettings = specularBasisSettings;
        this.weightBlockSize = weightBlockSize;
        base = new NonNegativeWeightOptimization(weightBlockSize, this.specularBasisSettings.getBasisCount(),
            Collections.singletonList(b -> 1.0), Collections.singletonList(1.0)); // Equality constraint to ensure that the weights sum up to 1.0.
    }

    public void execute(GraphicsStream<ReflectanceData> viewStream, SpecularDecomposition solution, int pStart)
    {
        System.out.println("Building weight fitting matrices...");

        // Setup all the matrices for fitting weights (one per texel)
        base.buildMatrices(viewStream, new SpecularWeightModel(solution, this.specularBasisSettings),
            // If a pixel is valid in some view, mark it as such in the solution.
            p -> solution.setWeightsValidity(p, true),
            pStart, Math.min(pStart + weightBlockSize, textureFitSettings.width * textureFitSettings.height));

        // Dampen so that it doesn't "snap" to the optimal solution right away.
        // TODO expose the damping factor as a setting.
//        base.dampenWithPreviousSolution(1.0, p -> b -> solution.getWeights(pStart + p).get(b));

        System.out.println("Finished building matrices; solving now...");

        // Optimize the weights and store the result in the SpecularFitSolution.
        if (pStart + weightBlockSize > textureFitSettings.width * textureFitSettings.height)
        {
            base.optimizeWeights(p -> solution.areWeightsValid(pStart + p),
                (p, weights) ->
                {
                    solution.setWeights(pStart + p, weights);
//                    solution.setWeights(pStart + p,
//                        weights.extractMatrix(0, weights.numRows() - 1, 0, 1).scale(0.5)
//                            .plus(solution.getWeights(pStart + p).scale(0.5)));
                },
                NonNegativeWeightOptimization.DEFAULT_TOLERANCE_SCALE, textureFitSettings.width * textureFitSettings.height - pStart);
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

        System.out.println("DONE!");
    }
}