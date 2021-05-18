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
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.util.ColorList;
import tetzlaff.util.NonNegativeLeastSquares;

public class BRDFReconstruction
{
    private static final double NNLS_TOLERANCE_SCALE = 0.000000000001;

    private final SpecularFitSettings settings;
    private final double metallicity;
    private final int matrixSize;

    public BRDFReconstruction(SpecularFitSettings settings, double metallicity)
    {
        this.settings = settings;
        this.metallicity = metallicity;
        matrixSize = settings.basisCount * (settings.microfacetDistributionResolution + 1);
    }

    public <ContextType extends Context<ContextType>> void execute(IBRResources<ContextType> resources,
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, SpecularFitSolution solution)
    {
        System.out.println("Building reflectance fitting matrix...");
        ReflectanceMatrixSystem system = buildReflectanceMatrix(resources, drawable, framebuffer, solution);

        System.out.println("Finished building matrix; solving now...");
        double medianATyRed = IntStream.range(0, system.vectorATyRed.getNumElements()).mapToDouble(system.vectorATyRed::get)
            .sorted().skip(system.vectorATyRed.getNumElements() / 2).filter(x -> x > 0).findFirst().orElse(1.0);
        double medianATyGreen = IntStream.range(0, system.vectorATyGreen.getNumElements()).mapToDouble(system.vectorATyGreen::get)
            .sorted().skip(system.vectorATyGreen.getNumElements() / 2).filter(x -> x > 0).findFirst().orElse(1.0);
        double medianATyBlue = IntStream.range(0, system.vectorATyBlue.getNumElements()).mapToDouble(system.vectorATyBlue::get)
            .sorted().skip(system.vectorATyBlue.getNumElements() / 2).filter(x -> x > 0).findFirst().orElse(1.0);
        SimpleMatrix brdfSolutionRed = NonNegativeLeastSquares.solvePremultiplied(system.matrixATA, system.vectorATyRed,
            NNLS_TOLERANCE_SCALE * medianATyRed);
        SimpleMatrix brdfSolutionGreen = NonNegativeLeastSquares.solvePremultiplied(system.matrixATA, system.vectorATyGreen,
            NNLS_TOLERANCE_SCALE * medianATyGreen);
        SimpleMatrix brdfSolutionBlue = NonNegativeLeastSquares.solvePremultiplied(system.matrixATA, system.vectorATyBlue,
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

        if (SpecularFitRequest.DEBUG)
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

    private <ContextType extends Context<ContextType>> ReflectanceMatrixSystem buildReflectanceMatrix(IBRResources<ContextType> resources,
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, SpecularFitSolution solution)
    {
        class Counter
        {
            int count = 0;
        }
        Counter counter = new Counter();

        ReflectanceMatrixSystem system = resources.parallelStream(drawable, framebuffer, 2)
            .map(framebufferData ->
            {
                ColorList colorAndVisibility = framebufferData[0];
                ColorList halfwayAndGeom = framebufferData[1];

                // Create scratch space for the thread handling this view.
                ReflectanceMatrixSystem contribution = new ReflectanceMatrixSystem(matrixSize, DMatrixRMaj.class);

                // Get the contributions from the current view.
                new ReflectanceMatrixBuilder(colorAndVisibility, halfwayAndGeom, solution, contribution, metallicity).execute();

                synchronized (counter)
                {
                    System.out.println("Finished view " + counter.count + '.');
                    counter.count++;
                }

                return contribution;
            })
            .collect(() -> new ReflectanceMatrixSystem(matrixSize, DMatrixRMaj.class), ReflectanceMatrixSystem::addContribution);

        if (SpecularFitRequest.DEBUG)
        {
            System.out.println();

            for (int b = 0; b < settings.basisCount; b++)
            {
                System.out.print("RHS, red for BRDF #" + b + ": ");

                System.out.print(system.vectorATyRed.get(b));
                for (int m = 0; m < settings.microfacetDistributionResolution; m++)
                {
                    System.out.print(", ");
                    System.out.print(system.vectorATyRed.get((m + 1) * settings.basisCount + b));
                }
                System.out.println();

                System.out.print("RHS, green for BRDF #" + b + ": ");

                System.out.print(system.vectorATyGreen.get(b));
                for (int m = 0; m < settings.microfacetDistributionResolution; m++)
                {
                    System.out.print(", ");
                    System.out.print(system.vectorATyGreen.get((m + 1) * settings.basisCount + b));
                }
                System.out.println();

                System.out.print("RHS, blue for BRDF #" + b + ": ");

                System.out.print(system.vectorATyBlue.get(b));
                for (int m = 0; m < settings.microfacetDistributionResolution; m++)
                {
                    System.out.print(", ");
                    System.out.print(system.vectorATyBlue.get((m + 1) * settings.basisCount + b));
                }
                System.out.println();

                System.out.println();
            }
        }

        return system;
    }
}
