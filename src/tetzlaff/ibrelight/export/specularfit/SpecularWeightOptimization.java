/*
 *  Copyright (c) Michael Tetzlaff 2021
 *  Copyright (c) The Regents of the University of Minnesota 2019
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

import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.ibrelight.rendering.GraphicsStream;
import tetzlaff.optimization.NonNegativeWeightOptimization;

import static java.lang.Math.PI;

public class SpecularWeightOptimization
{
    private final SpecularFitSettings settings;

    private final NonNegativeWeightOptimization base;

    public SpecularWeightOptimization(SpecularFitSettings settings)
    {
        this.settings = settings;

        base = new NonNegativeWeightOptimization(settings.width * settings.height, settings.basisCount,
            Collections.singletonList(b -> 1.0), Collections.singletonList(1.0)); // Equality constraint to ensure that the weights sum up to 1.0.
    }

    public void execute(GraphicsStream<ReflectanceData> viewStream, SpecularFitSolution solution)
    {
        System.out.println("Building weight fitting matrices...");

        // Initially assume that all texels are invalid.
        solution.invalidateWeights();

        // Setup all the matrices for fitting weights (one per texel)
        base.buildMatrices(viewStream, new SpecularWeightModel(solution, settings),
            // If a pixel is valid in some view, mark it as such in the solution.
            p -> solution.setWeightsValidity(p, true));

        System.out.println("Finished building matrices; solving now...");

        // Optimize the weights and store the result in the SpecularFitSolution.
        base.optimizeWeights(solution::areWeightsValid, solution::setWeights);

        System.out.println("DONE!");

        if (SpecularOptimization.DEBUG)
        {
            // write out weight textures for debugging
            solution.saveWeightMaps();

            // write out diffuse texture for debugging
            solution.saveDiffuseMap(settings.additional.getFloat("gamma"));
        }
    }
}