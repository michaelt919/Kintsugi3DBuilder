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

import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.fit.ReflectanceData;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.builder.resources.ibr.stream.GraphicsStream;
import kintsugi3d.gl.vecmath.DoubleVector3;
import kintsugi3d.optimization.MatrixSystem;
import kintsugi3d.optimization.function.BasisFunctions;
import kintsugi3d.optimization.function.OptimizedFunctions;
import kintsugi3d.util.Counter;
import org.ejml.data.DMatrixRMaj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BRDFReconstruction
{
    private static final Logger log = LoggerFactory.getLogger(BRDFReconstruction.class);
    private static final double NNLS_TOLERANCE_SCALE = 0.000000000001;
    private final BasisFunctions stepBasis;
    private final int matrixSize;
    private final SpecularBasisSettings settings;

    public BRDFReconstruction(SpecularBasisSettings settings, BasisFunctions stepBasis)
    {
        this.stepBasis = stepBasis;
        this.settings = settings;
        matrixSize = this.settings.getBasisCount() * (this.settings.getBasisResolution() + 1);
    }

    public void execute(GraphicsStream<ReflectanceData> viewStream, SpecularDecompositionFromScratch solution, ProgressMonitor monitor)
    {
        log.info("Building reflectance fitting matrix...");
        MatrixSystem system = buildReflectanceMatrix(viewStream, solution, monitor);

        log.info("Finished building matrix; solving now...");

        OptimizedFunctions brdfSolution = OptimizedFunctions.solveSystemNonNegative(stepBasis, system, NNLS_TOLERANCE_SCALE);

        log.info("DONE!");

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

    private MatrixSystem buildReflectanceMatrix(GraphicsStream<ReflectanceData> viewStream, SpecularDecomposition solution, ProgressMonitor monitor)
    {
        // TODO add ProgressMonitor support to GraphicsStream.
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
                    log.info("Finished view " + counter.get() + '.');
                    counter.increment();
                }

                return contribution;
            })
            .collect(() -> new MatrixSystem(matrixSize, 3, DMatrixRMaj.class), MatrixSystem::addContribution);

        for (int b = 0; b < settings.getBasisCount(); b++)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("RHS, red for BRDF #").append(b).append(": ");

            sb.append(system.rhs[0].get(b));
            for (int m = 0; m < settings.getBasisResolution(); m++)
            {
                sb.append(", ");
                sb.append(system.rhs[0].get((m + 1) * settings.getBasisCount() + b));
            }
            sb.append('\n');

            sb.append("RHS, green for BRDF #").append(b).append(": ");

            sb.append(system.rhs[1].get(b));
            for (int m = 0; m < settings.getBasisResolution(); m++)
            {
                sb.append(", ");
                sb.append(system.rhs[1].get((m + 1) * settings.getBasisCount() + b));
            }
            sb.append('\n');

            sb.append("RHS, blue for BRDF #").append(b).append(": ");

            sb.append(system.rhs[2].get(b));
            for (int m = 0; m < settings.getBasisResolution(); m++)
            {
                sb.append(", ");
                sb.append(system.rhs[2].get((m + 1) * settings.getBasisCount() + b));
            }
            log.debug(sb.toString());
        }

        return system;
    }
}
