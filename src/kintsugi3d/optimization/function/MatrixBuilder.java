/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.optimization.function;

import kintsugi3d.optimization.MatrixSystem;

import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Utility for optimization problems where the goal is to find a set of optimal basis functions
 * which are themselves in turn a linear combination of an underlying set of "library" basis functions.
 * The optimization problem itself is to find a least-squares solution to the equation:
 * SUM(over b from 1 to B) (w_b * f_b(p) * g(p)) = y(p)
 * f_b is the function that is being optimized.
 * w_b are weights, which are assumed to be fixed for this step.
 * g is an analytic function, which is assumed to be fixed for this step.
 * y is the observed value to be fitted to.
 */
public class MatrixBuilder
{
    private MatrixBuilderSample samplePrevious = null;

    /**
     * The number of "instances" being optimized in the fitting system.
     */
    private final int instanceCount;

    /**
     * The number of RHS observations per sample, i.e. the number of channels (like R,G,B for color).
     */
    private final int observationCount;

    private final MatrixBuilderSums sums;

    /**
     * Stores both the LHS and RHS of the system to be solved.
     * LHS = A'A
     * RHS = A'y, possibly for multiple channels (i.e. red, green, blue for color).
     */
    private final MatrixSystem contribution;

    /**
     * Underlying basis of "library" functions used to construct the actual functions being optimized.
     */
    private final BasisFunctions basisLibrary;

    /**
     * Assumed "metallicity" of the function being optimized.
     * Affects handling of the constant term -- i.e. diffuse for a BRDF.
     */
    private final double metallicity;

    /**
     * Construct by accepting matrices where the final results will be stored.
     * @param instanceCount The number of "instances" being optimized in the fitting system.
     * @param observationCount The number of RHS observations per sample, i.e. the number of channels (like R,G,B for color).
     * @param metallicity The "metallicity" of the function being optimized.  This has to do with the constant term
     *                    that is optimized alongside the weights for the basis functions.  A function that is 100%
     *                    "metallic" uses the same analytic factor for the constant term as for the basis functions.
     *                    A function that is 0% metallicy uses no analytic factor for the constant term
     *                    (just a simple constant).
     * @param contribution A reference to the object that will hold the matrices for the system to be solved for this
     *                     optimization problem.
     * @param basisLibrary The "library" of basis functions that can be used to construct the function being optimized.
     */
    public MatrixBuilder(int instanceCount, int observationCount, double metallicity,
                         BasisFunctions basisLibrary, MatrixSystem contribution)
    {
        this.instanceCount = instanceCount;
        this.observationCount = observationCount;

        this.contribution = contribution;

        this.basisLibrary = basisLibrary;
        this.metallicity = metallicity;

        // Initialize running totals
        sums = new MatrixBuilderSums(instanceCount, observationCount);
    }

    /**=
     * Gets the number of "instances" being optimized in the fitting system.
     * @return The number of instances being optimized.
     */
    public int getInstanceCount()
    {
        return instanceCount;
    }

    /**
     * Gets the number of RHS observations per sample, i.e. the number of channels (like R,G,B for color).
     * @return The number of observations per sample.
     */
    public int getObservationCount()
    {
        return observationCount;
    }

    /**
     * Gets the underlying basis of "library" functions used to construct the actual functions being optimized.
     * @return The basis function library.
     */
    public BasisFunctions getBasisLibrary()
    {
        return basisLibrary;
    }

    /**
     * Gets the assumed "metallicity" of the function being optimized.  This has to do with the constant term
     * that is optimized alongside the weights for the basis functions (i.e. diffuse for a BRDF).  A function that is
     * 100% "metallic" uses the same analytic factor for the constant term as for the basis functions.
     * A function that is 0% metallicy uses no analytic factor for the constant term (just a simple constant).
     * @return The metallicity being assumed.
     */
    public double getMetallicity()
    {
        return metallicity;
    }

    /**
     * Evaluates the "constant" term (i.e. diffuse for a BRDF), which may or may not have incorporated the analytic factor.
     * @param analyticFactor The analytic factor which needs to be applied for a "metallic" function.
     * @return The value of the "constant" term
     */
    private double getConstantTerm(double analyticFactor)
    {
        return metallicity * analyticFactor + (1 - metallicity);
    }

    /**
     * Builds the matrix from a stream of samples.
     * The samples will be sorted for efficiency and then applied to the matrix system.
     * This function should only be called once per instance of this class.
     * @param samples The stream of samples to process.
     */
    public void build(Stream<MatrixBuilderSample> samples)
    {
        // Sort samples so that we avoid repeating the same additions over and over again
        // Should sort ascending to visit low values first
        samples.sorted(Comparator.comparingDouble(sample -> sample.actual))
                .forEachOrdered(this::processSample);
        finish();
    }

    /**
     * Processes an individual sample and adds its contributions to the running totals.
     * When the sample's "bin" changes, these running totals will be applied to the matrix system.
     * IMPORTANT: It is assumed that the samples have been pre-sorted by value in order for this function to work
     * correctly.  Each sample must be in a "greater" bin than all previous samples passed to this function.
     * @param sample The new sample to handle
     */
    private void processSample(MatrixBuilderSample sample)
    {
        int sampleFloorPrevious = samplePrevious == null ? 0 : samplePrevious.floor;

        // mFloor should be increasing over time due to sorting order.
        assert sampleFloorPrevious <= sample.floor
                : "sampleFloorPrevious: " + sampleFloorPrevious + " sample.floor: " + sample.floor;

        // If sample.floor changed, it's time to update the ATA matrix and ATy vector
        if (sample.floor > sampleFloorPrevious)
        {
            // This is where most elements of the matrix get updated.
            // This function uses the running totals that have accumulated for the previous value
            // and updates the matrix accordingly.
            basisLibrary.contributeToFittingSystem(sampleFloorPrevious, sample.floor, instanceCount, sums, contribution);

            // Zero out the blended sums after every time that sample.floor changes,
            // since it should only apply to a single m-value (as opposed to the other sums which continue to accumulate).
            sums.clearNonCumulativeSums();
        }

        double constantTerm = getConstantTerm(sample.analytic);
        double constantTermSquared = constantTerm * constantTerm;

        if (sample.inOptimizedDomain)
        {
            // Update running totals
            // (this is what determines how the sample influences most elements of the matrix system)
            sums.accept(sample);
        }

        // Regardless of whether sample.floor changed: Update for constant term,
        // where no work is saved by deferring the update to the matrix or vector.
        for (int b1 = 0; b1 < instanceCount; b1++)
        {
            // Update to ATy
            for (int i = 0; i < observationCount; i++)
            {
                contribution.addToRHS(b1, i, sample.weightByInstance.applyAsDouble(b1)
                        * sample.sampleWeight * sample.observed[i] * constantTerm);
            }

            for (int b2 = 0; b2 < instanceCount; b2++)
            {
                // Updates to ATA
                // Top left partition of the matrix: row and column both correspond to constant-term coefficients
                // (i.e. diffuse for reflectance)
                double weightProduct = sample.weightByInstance.applyAsDouble(b1)
                        * sample.weightByInstance.applyAsDouble(b2) * sample.sampleWeight;
                contribution.addToLHS(b1, b2, weightProduct * constantTermSquared);
            }
        }

        // Update reference to previous sample.
        samplePrevious = sample;
    }

    /**
     * Finishes building the matrix system by adding the final running totals the matrix system.
     * This must always be called to add the contributions for the last bin of samples (right at the cutoff for the optimized domain)
     */
    private void finish()
    {
        if (samplePrevious != null)
        {
            // This function uses the running totals that have accumulated for the previous value
            // and updates the matrix accordingly.
            basisLibrary.contributeToFittingSystem(samplePrevious.floor,
                    basisLibrary.getOptimizedDomainSize() - 1, instanceCount, sums, contribution);
        }
    }
}
