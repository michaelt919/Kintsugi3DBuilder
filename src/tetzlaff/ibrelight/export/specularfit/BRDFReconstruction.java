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

import org.ejml.data.DMatrixRMaj;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.ibrelight.rendering.resources.GraphicsStream;
import tetzlaff.optimization.function.BasisFunctions;
import tetzlaff.optimization.MatrixSystem;
import tetzlaff.optimization.function.OptimizedFunctions;
import tetzlaff.util.Counter;

public class BRDFReconstruction
{
    private static final double NNLS_TOLERANCE_SCALE = 0.000000000001;

    private final SpecularFitSettings settings;
    private final BasisFunctions stepBasis;
    private final int matrixSize;

    public BRDFReconstruction(SpecularFitSettings settings, BasisFunctions stepBasis)
    {
        this.settings = settings;
        this.stepBasis = stepBasis;
        matrixSize = settings.basisCount * (settings.microfacetDistributionResolution + 1);
    }

    public void execute(GraphicsStream<ReflectanceData> viewStream, SpecularFitSolution solution)
    {
        System.out.println("Building reflectance fitting matrix...");
        MatrixSystem system = buildReflectanceMatrix(viewStream, solution);

        System.out.println("Finished building matrix; solving now...");

        OptimizedFunctions brdfSolution = OptimizedFunctions.solveSystemNonNegative(stepBasis, system, NNLS_TOLERANCE_SCALE);

        System.out.println("DONE!");

        for (int b = 0; b < settings.basisCount; b++)
        {
            int bCopy = b;

            // Only update if the BRDF has non-zero elements.
            if (brdfSolution.isInstanceNonZero(b))
            {
                solution.setDiffuseAlbedo(b, new DoubleVector3(
                        brdfSolution.getTrueConstantTerm(b, 0) * Math.PI,
                        brdfSolution.getTrueConstantTerm(b, 1) * Math.PI,
                        brdfSolution.getTrueConstantTerm(b, 2) * Math.PI));

                brdfSolution.evaluateNonConstantSolution(b, 0,
                        (value, m) -> solution.getSpecularRed().set(m, bCopy, value));
                brdfSolution.evaluateNonConstantSolution(b, 1,
                        (value, m) -> solution.getSpecularGreen().set(m, bCopy, value));
                brdfSolution.evaluateNonConstantSolution(b, 2,
                        (value, m) -> solution.getSpecularBlue().set(m, bCopy, value));
            }
        }

//        if (SpecularOptimization.DEBUG)
//        {
//            System.out.println();
//
//            for (int b = 0; b < settings.basisCount; b++)
//            {
//                DoubleVector3 diffuseColor = new DoubleVector3(
//                    brdfSolutionRed.get(b) * Math.PI,
//                    brdfSolutionGreen.get(b) * Math.PI,
//                    brdfSolutionBlue.get(b) * Math.PI);
//                System.out.println("Diffuse #" + b + ": " + diffuseColor);
//            }
//
//            System.out.println("Basis BRDFs:");
//
//            for (int b = 0; b < settings.basisCount; b++)
//            {
//                System.out.print("Red#" + b);
//                double redTotal = 0.0;
//                for (int m = settings.microfacetDistributionResolution - 1; m >= 0; m--)
//                {
//                    System.out.print(", ");
//                    redTotal += brdfSolutionRed.get((m + 1) * settings.basisCount + b);
//                    System.out.print(redTotal);
//                }
//
//                System.out.println();
//
//                System.out.print("Green#" + b);
//                double greenTotal = 0.0;
//                for (int m = settings.microfacetDistributionResolution - 1; m >= 0; m--)
//                {
//                    System.out.print(", ");
//                    greenTotal += brdfSolutionGreen.get((m + 1) * settings.basisCount + b);
//                    System.out.print(greenTotal);
//                }
//                System.out.println();
//
//                System.out.print("Blue#" + b);
//                double blueTotal = 0.0;
//                for (int m = settings.microfacetDistributionResolution - 1; m >= 0; m--)
//                {
//                    System.out.print(", ");
//                    blueTotal += brdfSolutionBlue.get((m + 1) * settings.basisCount + b);
//                    System.out.print(blueTotal);
//                }
//                System.out.println();
//            }
//
//            System.out.println();
//        }
    }

    private MatrixSystem buildReflectanceMatrix(GraphicsStream<ReflectanceData> viewStream, SpecularFitSolution solution)
    {
        Counter counter = new Counter();

        MatrixSystem system = viewStream
            .map(reflectanceData ->
            {
                // Create scratch space for the thread handling this view.
                MatrixSystem contribution = new MatrixSystem(matrixSize, 3, DMatrixRMaj.class);

                // Get the contributions from the current view.
                new ReflectanceMatrixBuilder(reflectanceData, solution, settings.getMetallicity(), stepBasis, contribution).execute();

                synchronized (counter)
                {
                    System.out.println("Finished view " + counter.get() + '.');
                    counter.increment();
                }

                return contribution;
            })
            .collect(() -> new MatrixSystem(matrixSize, 3, DMatrixRMaj.class), MatrixSystem::addContribution);

        if (SpecularOptimization.DEBUG)
        {
            System.out.println();

            for (int b = 0; b < settings.basisCount; b++)
            {
                System.out.print("RHS, red for BRDF #" + b + ": ");

                System.out.print(system.rhs[0].get(b));
                for (int m = 0; m < settings.microfacetDistributionResolution; m++)
                {
                    System.out.print(", ");
                    System.out.print(system.rhs[0].get((m + 1) * settings.basisCount + b));
                }
                System.out.println();

                System.out.print("RHS, green for BRDF #" + b + ": ");

                System.out.print(system.rhs[1].get(b));
                for (int m = 0; m < settings.microfacetDistributionResolution; m++)
                {
                    System.out.print(", ");
                    System.out.print(system.rhs[1].get((m + 1) * settings.basisCount + b));
                }
                System.out.println();

                System.out.print("RHS, blue for BRDF #" + b + ": ");

                System.out.print(system.rhs[2].get(b));
                for (int m = 0; m < settings.microfacetDistributionResolution; m++)
                {
                    System.out.print(", ");
                    System.out.print(system.rhs[2].get((m + 1) * settings.basisCount + b));
                }
                System.out.println();

                System.out.println();
            }
        }

        return system;
    }
}
