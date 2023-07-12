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
    private final BasisFunctions stepBasis;
    private final int matrixSize;
    private final SpecularBasisSettings settings;

    public BRDFReconstruction(SpecularBasisSettings settings, BasisFunctions stepBasis)
    {
        this.stepBasis = stepBasis;
        this.settings = settings;
        matrixSize = this.settings.getBasisCount() * (this.settings.getMicrofacetDistributionResolution() + 1);
    }

    public void execute(GraphicsStream<ReflectanceData> viewStream, SpecularDecompositionFromScratch solution)
    {
        System.out.println("Building reflectance fitting matrix...");
        MatrixSystem system = buildReflectanceMatrix(viewStream, solution);

        System.out.println("Finished building matrix; solving now...");

        OptimizedFunctions brdfSolution = OptimizedFunctions.solveSystemNonNegative(stepBasis, system, NNLS_TOLERANCE_SCALE);

        System.out.println("DONE!");

        for (int b = 0; b < settings.getBasisCount(); b++)
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
////            // Hard code solution for debugging
////            solution.setDiffuseAlbedo(0, new DoubleVector3(1.0, 0.5, 0.25));
////            solution.getSpecularRed().setColumn(0, 0, 0.75, 0.0, 0.0, 0.0);
////            solution.getSpecularGreen().setColumn(0, 0, 0.75, 0.0, 0.0, 0.0);
////            solution.getSpecularBlue().setColumn(0, 0, 0.75, 0.0, 0.0, 0.0);
//
//            System.out.println();
//
//            for (int b = 0; b < settings.basisCount; b++)
//            {
//                System.out.println("Diffuse #" + b + ": " + solution.getDiffuseAlbedo(b));
//            }
//
//            System.out.println("Basis BRDFs:");
//
//            for (int b = 0; b < settings.basisCount; b++)
//            {
//                System.out.print("Red#" + b);
//
//                for (int m = settings.microfacetDistributionResolution - 1; m >= 0; m--)
//                {
//                    System.out.print(", ");
//                    System.out.print(solution.getSpecularRed().get(m));
//                }
//
//                System.out.println();
//
//                System.out.print("Green#" + b);
//
//                for (int m = settings.microfacetDistributionResolution - 1; m >= 0; m--)
//                {
//                    System.out.print(", ");
//                    System.out.print(solution.getSpecularGreen().get(m));
//                }
//                System.out.println();
//
//                System.out.print("Blue#" + b);
//
//                for (int m = settings.microfacetDistributionResolution - 1; m >= 0; m--)
//                {
//                    System.out.print(", ");
//                    System.out.print(solution.getSpecularBlue().get(m));
//                }
//                System.out.println();
//            }
//
//            System.out.println();
//        }
    }

    private MatrixSystem buildReflectanceMatrix(GraphicsStream<ReflectanceData> viewStream, SpecularDecomposition solution)
    {
        Counter counter = new Counter();
        MatrixSystem system =
            viewStream.map(reflectanceData ->
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

            for (int b = 0; b < settings.getBasisCount(); b++)
            {
                System.out.print("RHS, red for BRDF #" + b + ": ");

                System.out.print(system.rhs[0].get(b));
                for (int m = 0; m < settings.getMicrofacetDistributionResolution(); m++)
                {
                    System.out.print(", ");
                    System.out.print(system.rhs[0].get((m + 1) * settings.getBasisCount() + b));
                }
                System.out.println();

                System.out.print("RHS, green for BRDF #" + b + ": ");

                System.out.print(system.rhs[1].get(b));
                for (int m = 0; m < settings.getMicrofacetDistributionResolution(); m++)
                {
                    System.out.print(", ");
                    System.out.print(system.rhs[1].get((m + 1) * settings.getBasisCount() + b));
                }
                System.out.println();

                System.out.print("RHS, blue for BRDF #" + b + ": ");

                System.out.print(system.rhs[2].get(b));
                for (int m = 0; m < settings.getMicrofacetDistributionResolution(); m++)
                {
                    System.out.print(", ");
                    System.out.print(system.rhs[2].get((m + 1) * settings.getBasisCount() + b));
                }
                System.out.println();

                System.out.println();
            }
        }

        return system;
    }
}
