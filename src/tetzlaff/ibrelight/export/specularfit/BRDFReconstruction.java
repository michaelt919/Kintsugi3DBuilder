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

import java.util.stream.IntStream;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.ibrelight.rendering.GraphicsStream;
import tetzlaff.optimization.Basis;
import tetzlaff.optimization.MatrixSystem;
import tetzlaff.util.Counter;
import tetzlaff.optimization.NonNegativeLeastSquares;

public class BRDFReconstruction
{
    private static final double NNLS_TOLERANCE_SCALE = 0.000000000001;

    private final SpecularFitSettings settings;
    private final Basis stepBasis;
    private final double metallicity;
    private final int matrixSize;

    public BRDFReconstruction(SpecularFitSettings settings, Basis stepBasis, double metallicity)
    {
        this.settings = settings;
        this.stepBasis = stepBasis;
        this.metallicity = metallicity;
        matrixSize = settings.basisCount * (settings.microfacetDistributionResolution + 1);
    }

    public void execute(GraphicsStream<ReflectanceData> viewStream, SpecularFitSolution solution)
    {
        System.out.println("Building reflectance fitting matrix...");
        MatrixSystem system = buildReflectanceMatrix(viewStream, solution);

        System.out.println("Finished building matrix; solving now...");
        double medianATyRed = IntStream.range(0, system.rhs[0].getNumElements()).mapToDouble(system.rhs[0]::get)
            .sorted().skip(system.rhs[0].getNumElements() / 2).filter(x -> x > 0).findFirst().orElse(1.0);
        double medianATyGreen = IntStream.range(0, system.rhs[1].getNumElements()).mapToDouble(system.rhs[1]::get)
            .sorted().skip(system.rhs[1].getNumElements() / 2).filter(x -> x > 0).findFirst().orElse(1.0);
        double medianATyBlue = IntStream.range(0, system.rhs[2].getNumElements()).mapToDouble(system.rhs[2]::get)
            .sorted().skip(system.rhs[2].getNumElements() / 2).filter(x -> x > 0).findFirst().orElse(1.0);
        SimpleMatrix brdfSolutionRed = NonNegativeLeastSquares.solvePremultiplied(system.lhs, system.rhs[0],
            NNLS_TOLERANCE_SCALE * medianATyRed);
        SimpleMatrix brdfSolutionGreen = NonNegativeLeastSquares.solvePremultiplied(system.lhs, system.rhs[1],
            NNLS_TOLERANCE_SCALE * medianATyGreen);
        SimpleMatrix brdfSolutionBlue = NonNegativeLeastSquares.solvePremultiplied(system.lhs, system.rhs[2],
            NNLS_TOLERANCE_SCALE * medianATyBlue);

        System.out.println("DONE!");

        for (int b = 0; b < settings.basisCount; b++)
        {
            int bCopy = b;

            // Only update if the BRDF has non-zero elements.
            if (IntStream.range(0, settings.microfacetDistributionResolution + 1).anyMatch(
                i -> brdfSolutionRed.get(bCopy + settings.basisCount * i) > 0
                    || brdfSolutionGreen.get(bCopy + settings.basisCount * i) > 0
                    || brdfSolutionBlue.get(bCopy + settings.basisCount * i) > 0))
            {
                DoubleVector3 baseColor = new DoubleVector3(brdfSolutionRed.get(b), brdfSolutionGreen.get(b), brdfSolutionBlue.get(b));
                solution.setDiffuseAlbedo(b, baseColor.times(1.0 - metallicity));

                solution.getSpecularRed().set(settings.microfacetDistributionResolution, b, baseColor.x * metallicity);
                solution.getSpecularGreen().set(settings.microfacetDistributionResolution, b, baseColor.y * metallicity);
                solution.getSpecularBlue().set(settings.microfacetDistributionResolution, b, baseColor.z * metallicity);

                for (int m = settings.microfacetDistributionResolution - 1; m >= 0; m--)
                {
                    // f[m] = f[m+1] + estimated difference (located at index m + 1 due to diffuse component at index 0).
                    solution.getSpecularRed().set(m, b, solution.getSpecularRed().get(m + 1, b) + brdfSolutionRed.get((m + 1) * settings.basisCount + b));
                    solution.getSpecularGreen().set(m, b, solution.getSpecularGreen().get(m + 1, b) + brdfSolutionGreen.get((m + 1) * settings.basisCount + b));
                    solution.getSpecularBlue().set(m, b, solution.getSpecularBlue().get(m + 1, b) + brdfSolutionBlue.get((m + 1) * settings.basisCount + b));
                }
            }
        }

        if (SpecularOptimization.DEBUG)
        {
            System.out.println();

            for (int b = 0; b < settings.basisCount; b++)
            {
                DoubleVector3 diffuseColor = new DoubleVector3(
                    brdfSolutionRed.get(b),
                    brdfSolutionGreen.get(b),
                    brdfSolutionBlue.get(b));
                System.out.println("Diffuse #" + b + ": " + diffuseColor);
            }

            System.out.println("Basis BRDFs:");

            for (int b = 0; b < settings.basisCount; b++)
            {
                System.out.print("Red#" + b);
                double redTotal = 0.0;
                for (int m = settings.microfacetDistributionResolution - 1; m >= 0; m--)
                {
                    System.out.print(", ");
                    redTotal += brdfSolutionRed.get((m + 1) * settings.basisCount + b);
                    System.out.print(redTotal);
                }

                System.out.println();

                System.out.print("Green#" + b);
                double greenTotal = 0.0;
                for (int m = settings.microfacetDistributionResolution - 1; m >= 0; m--)
                {
                    System.out.print(", ");
                    greenTotal += brdfSolutionGreen.get((m + 1) * settings.basisCount + b);
                    System.out.print(greenTotal);
                }
                System.out.println();

                System.out.print("Blue#" + b);
                double blueTotal = 0.0;
                for (int m = settings.microfacetDistributionResolution - 1; m >= 0; m--)
                {
                    System.out.print(", ");
                    blueTotal += brdfSolutionBlue.get((m + 1) * settings.basisCount + b);
                    System.out.print(blueTotal);
                }
                System.out.println();
            }

            System.out.println();
        }
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
                new ReflectanceMatrixBuilder(reflectanceData, solution, contribution, stepBasis, metallicity).execute();

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
