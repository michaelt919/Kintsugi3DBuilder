/*
 *  Copyright (c) Michael Tetzlaff 2020
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

import java.util.function.IntToDoubleFunction;
import java.util.stream.IntStream;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.optimization.Basis;
import tetzlaff.optimization.MatrixBuilderSample;
import tetzlaff.optimization.MatrixBuilderSums;
import tetzlaff.optimization.MatrixSystem;

import static org.ejml.dense.row.CommonOps_DDRM.multTransA;

/**
 * A helper class to maintain state necessary to efficiently build the matrix that can solve for reflectance.
 */
final class ReflectanceMatrixBuilder
{
    // Set to true to validate the implementation (should generally be turned off for much better efficiency).
    private static final boolean VALIDATE = true;

    private int mPrevious = 0;

    private final MatrixBuilderSums sums;

    /**
     * Stores both the LHS and RHS of the system to be solved.
     * LHS = A'A
     * RHS = A'y for red, green, blue
     */
    private final MatrixSystem contribution;

    /**
     * Reflectance information for all the data.
     */
    private final ReflectanceData reflectanceData;

    /**
     * Weight solution from the previous iteration.
     */
    private final SpecularFitSolution solution;

    /**
     * Settings to be used for the specular fit; associated with the SpecularFitSolution.
     */
    private final SpecularFitSettings settings;

    /**
     * Underlying basis of step-like functions used to construct the actual basis functions used for per-pixel fitting.
     */
    private final Basis stepBasis;

    /**
     * Assumed metallicity of the material (affects handling of diffuse reflectance).
     */
    private final double metallicity;

    /**
     * Construct by accepting matrices where the final results will be stored.
     */
    ReflectanceMatrixBuilder(ReflectanceData reflectanceData, SpecularFitSolution solution,
                             MatrixSystem contribution, Basis stepBasis, double metallicity)
    {
        this.contribution = contribution;

        this.solution = solution;

        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.reflectanceData = reflectanceData;
        this.stepBasis = stepBasis;
        this.metallicity = metallicity;

        settings = solution.getSettings();

        // Initialize running totals
        sums = new MatrixBuilderSums(solution.getSettings().basisCount, 3);

    }

    public void execute()
    {
        // Sort pixel samples within a view by the halfway direction so that we avoid repeating the same additions over and over again
        IntStream.range(0, reflectanceData.size())
            .filter(p -> reflectanceData.getVisibility(p) > 0) // Eliminate pixels without valid samples
            .boxed() // Box integers to use custom sorting function
            .sorted((p1, p2) -> Float.compare(reflectanceData.getHalfwayIndex(p1), reflectanceData.getHalfwayIndex(p2))) // Should sort ascending to visit low m values first
            .forEachOrdered(this::processSample);

        if (VALIDATE)
        {
            validate();
        }
    }

    private double getDiffuseFactor(double geomRatio)
    {
        return metallicity * geomRatio + (1 - metallicity) / Math.PI;
    }

    private void processSample(int p)
    {
        float halfwayIndex = reflectanceData.getHalfwayIndex(p);
        float geomRatio = reflectanceData.getGeomRatio(p);
        float addlWeight = reflectanceData.getAdditionalWeight(p);
        Vector3 color = reflectanceData.getColor(p);

        // Calculate which discretized MDF element the current sample belongs to.
        double mExact = halfwayIndex * settings.microfacetDistributionResolution;
        int mFloor = Math.min(settings.microfacetDistributionResolution - 1, (int) Math.floor(mExact));

        // mFloor should be increasing over time due to sorting order.
        assert mPrevious <= mFloor : "mPrevious: " + mPrevious + " mFloor: " + mFloor + " mExact: " + mExact;

        // If mFloor changed, it's time to update the ATA matrix and ATy vector
        if (mFloor > mPrevious)
        {
            stepBasis.contributeToFittingSystem(mFloor, mPrevious, settings.basisCount, sums, contribution);

            // Zero out the blended sums after every time that mFloor changes,
            // since it should only apply to a single m-value (as opposed to the other sums which continue to accumulate).
            sums.clearBlendedSums();
        }

        // When floor and exact are the same, t = 1.0.  When exact is almost a whole increment greater than floor, t approaches 0.0.
        // If mFloor is clamped to MICROFACET_DISTRIBUTION_RESOLUTION -1, then mExact will be much larger, so t = 0.0.
        double t = Math.max(0.0, 1.0 + mFloor - mExact);

        double addlWeightSquared = addlWeight * addlWeight;

        double diffuseFactor = getDiffuseFactor(geomRatio);
        double diffuseFactorSquared = diffuseFactor * diffuseFactor;

        if (mExact < settings.microfacetDistributionResolution)
        {
            sums.accept(new MatrixBuilderSample(geomRatio, addlWeight, t, b -> solution.getWeights(p).get(b),
                    color.x, color.y, color.z));
        }

        // TODO: move the update for the diffuse partition out of here too.

        // Regardless of whether mFloor changed: Update running total for each pair of basis functions,
        // and add blended samples to elements where no work is saved by deferring the update to the matrix or vector.
        for (int b1 = 0; b1 < settings.basisCount; b1++)
        {
            // Updates to ATy

            double weightedReflectanceRed   = solution.getWeights(p).get(b1) * addlWeightSquared * color.x;
            double weightedReflectanceGreen = solution.getWeights(p).get(b1) * addlWeightSquared * color.y;
            double weightedReflectanceBlue  = solution.getWeights(p).get(b1) * addlWeightSquared * color.z;

            // For each basis function: update the diffuse coefficients on the RHS.
            // Top partition of the vector corresponds to diffuse coefficients
            contribution.addToRHS(b1, 0, weightedReflectanceRed * diffuseFactor);
            contribution.addToRHS(b1, 1, weightedReflectanceGreen * diffuseFactor);
            contribution.addToRHS(b1, 2, weightedReflectanceBlue * diffuseFactor);

            int i = settings.basisCount * (mFloor + 1) + b1;

//            if (mExact < settings.microfacetDistributionResolution)
//            {
//                double weightedGeomReflectanceRed = geomRatio * weightedReflectanceRed;
//                double weightedGeomReflectanceGreen = geomRatio * weightedReflectanceGreen;
//                double weightedGeomReflectanceBlue = geomRatio * weightedReflectanceBlue;

//                // Bottom partition of the vector corresponds to specular coefficients.
//                // Scale contribution due to current m-value by blending weight t to account for linear interpolation.
//                // Accumulation due to greater m-values should already have been added to the vector the last time an m-value changed
//                contribution.addToRHS(i, 0, t * weightedGeomReflectanceRed);
//                contribution.addToRHS(i, 1, t * weightedGeomReflectanceGreen);
//                contribution.addToRHS(i, 2, t * weightedGeomReflectanceBlue);

//                // Update running totals.
//                sums.addWeightedAnalyticTimesObserved(0, b1, weightedGeomReflectanceRed);
//                sums.addWeightedAnalyticTimesObserved(1, b1, weightedGeomReflectanceGreen);
//                sums.addWeightedAnalyticTimesObserved(2, b1, weightedGeomReflectanceBlue);
//            }

            for (int b2 = 0; b2 < settings.basisCount; b2++)
            {
                // Updates to ATA

                // Top left partition of the matrix: row and column both correspond to diffuse coefficients
                double weightProduct = solution.getWeights(p).get(b1) * solution.getWeights(p).get(b2) * addlWeightSquared;
                contribution.addToLHS(b1, b2, weightProduct * diffuseFactorSquared);

//                if (mExact < settings.microfacetDistributionResolution)
//                {
////                    // Update non-squared total without blending weight.
//                    double weightedGeom = weightProduct * geomRatio;
////                    sums.weightedAnalyticSum.set(b1, b2, sums.weightedAnalyticSum.get(b1, b2) + weightedGeom);
////
////                    // Update squared total without blending weight.
////                    double weightedGeomSquared = weightedGeom * geomRatio;
////                    sums.weightedAnalyticSquaredSum.set(b1, b2, sums.weightedAnalyticSquaredSum.get(b1, b2) + weightedGeomSquared);
////
////                    // Update squared total with blending weight.
////                    double weightedGeomSquaredBlended = t * weightedGeomSquared;
////                    sums.weightedAnalyticSquaredBlendedSum.set(b1, b2, sums.weightedAnalyticSquaredBlendedSum.get(b1, b2) + weightedGeomSquaredBlended);
//
//                    // Top right and bottom left partitions of the matrix:
//                    // row corresponds to diffuse coefficients and column corresponds to specular, or vice-versa.
////                    contribution.addToLHS(i, b2, t * weightedGeom * diffuseFactor);
////                    contribution.addToLHS(b2, i, t * weightedGeom  * diffuseFactor);
//
////                    // Bottom right partition of the matrix: row and column both correspond to specular.
////                    // Update "corner" element with squared blending weight.
////                    int j = settings.basisCount * (mFloor + 1) + b2;
////                    contribution.addToLHS(i, j, t * weightedGeomSquaredBlended);
//                }
            }
        }

        // Update holder of previous mFloor value.
        mPrevious = mFloor;
    }

//    /**
//     * Updates the contribution matrix and vectors for a particular range of m-values, given certain running totals.
//     * Usually called when building the reflectance matrix, after the m-value changes.
//     * Also called at the end of that process to flush out the final set of running totals.
//     * @param mCurrent The current "m" value of the sample that is being processed. Samples are to be visited in order of decreasing "m".
//     */
//    private void updateContributionFromRunningTotals(int mCurrent)
//    {
//        // Add the running total to elements of the ATA matrix and the ATy vector corresponding to the newly visited m
//        // as well as any m-values skipped over.
//        // These elements also need to get some more contributions that have blending weights that are yet to be visited,
//        // but that will be handled later, when a sample is visited for some matrix elements, or the next time m changes for others.
//        for (int b1 = 0; b1 < settings.basisCount; b1++)
//        {
//            // This loop usually would only one once, but could run multiple times if we skipped a few m values.
//            for (int m1 = mPrevious + 1; m1 <= mCurrent; m1++)
//            {
//                int i = settings.basisCount * (m1 + 1) + b1;
//
//                // Update ATy vectors
//                contribution.rhs[0].set(i, 0, contribution.rhs[0].get(i, 0) + sums.weightedAnalyticTimesObserved[0].get(b1, 0));
//                contribution.rhs[1].set(i, 0, contribution.rhs[1].get(i, 0) + sums.weightedAnalyticTimesObserved[1].get(b1, 0));
//                contribution.rhs[2].set(i, 0, contribution.rhs[2].get(i, 0) + sums.weightedAnalyticTimesObserved[2].get(b1, 0));
//
//                // Update ATA matrix
//                for (int b2 = 0; b2 < settings.basisCount; b2++)
//                {
//                    // Top right and bottom left partitions of the matrix:
//                    // row corresponds to diffuse coefficients and column corresponds to specular, or vice-versa.
//                    // The matrix is symmetric so we also need to swap row and column and update that way.
//                    contribution.lhs.set(i, b2, contribution.lhs.get(i, b2) +
//                        metallicity * sums.weightedAnalyticSquaredSum.get(b1, b2) + (1 - metallicity) * sums.weightedAnalyticSum.get(b1, b2) / Math.PI);
//                    contribution.lhs.set(b2, i, contribution.lhs.get(b2, i) +
//                        metallicity * sums.weightedAnalyticSquaredSum.get(b2, b1) + (1 - metallicity) * sums.weightedAnalyticSum.get(b2, b1) / Math.PI);
//
//                    // Bottom right partition of the matrix: row and column both correspond to specular.
//
//                    // Handle "corner" case where m1 = m2 (don't want to repeat with row and column swapped as elements would then be duplicated).
//                    int j = settings.basisCount * (m1 + 1) + b2;
//                    contribution.lhs.set(i, j, contribution.lhs.get(i, j) + sums.weightedAnalyticSquaredSum.get(b1, b2));
//
//                    // Visit every element of the microfacet distribution that comes after m1.
//                    // This is because the form of ATA is such that the values in the matrix are determined by the lower of the two m-values.
//                    for (int m2 = m1 + 1; m2 < settings.microfacetDistributionResolution; m2++)
//                    {
//                        j = settings.basisCount * (m2 + 1) + b2;
//
//                        // Add the current value of the running total to the appropriate location in the matrix.
//                        // The matrix is symmetric so we also need to swap row and column and update that way.
//                        contribution.lhs.set(i, j, contribution.lhs.get(i, j) + sums.weightedAnalyticSquaredSum.get(b1, b2));
//                        contribution.lhs.set(j, i, contribution.lhs.get(j, i) + sums.weightedAnalyticSquaredSum.get(b2, b1));
//                    }
//                }
//            }
//        }
//
//        // Add the total of recently visited samples with blending weights to elements of the ATA matrix corresponding to the old m.
//        // Bottom right partition of the matrix: row and column both correspond to specular.
//        for (int b1 = 0; b1 < settings.basisCount; b1++)
//        {
//            int i = settings.basisCount * (mPrevious + 1) + b1;
//
//            for (int b2 = 0; b2 < settings.basisCount; b2++)
//            {
//                // The "corner case" was handled immediately when a sample was visited as it only affects a single element of the
//                // matrix and thus no work is saved by waiting for m to change.
//
//                // Visit every element of the microfacet distribution that comes after mPrevious.
//                // This is because the form of ATA is such that the values in the matrix are determined by the lower of the two m-values.
//                for (int m2 = mPrevious + 1; m2 < settings.microfacetDistributionResolution; m2++)
//                {
//                    int j = settings.basisCount * (m2 + 1) + b2;
//
//                    // Add the current value of the running total with blending (linear interpolation) weights to the appropriate location in the matrix.
//                    // The matrix is symmetric so we also need to swap row and column and update that way.
//                    contribution.lhs.set(i, j, contribution.lhs.get(i, j) + sums.weightedAnalyticSquaredBlendedSum.get(b1, b2));
//                    contribution.lhs.set(j, i, contribution.lhs.get(j, i) + sums.weightedAnalyticSquaredBlendedSum.get(b2, b1));
//                }
//            }
//        }
//    }

    private void validate()
    {
        // Calculate the matrix products the slow way to make sure that the implementation is correct.
        SimpleMatrix mA = new SimpleMatrix(reflectanceData.size(), settings.basisCount * (settings.microfacetDistributionResolution + 1), DMatrixRMaj.class);
        SimpleMatrix yRed = new SimpleMatrix(reflectanceData.size(), 1);
        SimpleMatrix yGreen = new SimpleMatrix(reflectanceData.size(), 1);
        SimpleMatrix yBlue = new SimpleMatrix(reflectanceData.size(), 1);

        for (int p = 0; p < reflectanceData.size(); p++)
        {
            if (reflectanceData.getVisibility(p) > 0)
            {
                float halfwayIndex = reflectanceData.getHalfwayIndex(p);
                float geomRatio = reflectanceData.getGeomRatio(p);
                float addlWeight = reflectanceData.getAdditionalWeight(p);
                Vector3 color = reflectanceData.getColor(p);

                // Calculate which discretized MDF element the current sample belongs to.
                double mExact = halfwayIndex * settings.microfacetDistributionResolution;
                int mFloor = Math.min(settings.microfacetDistributionResolution - 1, (int) Math.floor(mExact));

                yRed.set(p, addlWeight * color.x);
                yGreen.set(p, addlWeight * color.y);
                yBlue.set(p, addlWeight * color.z);

                // When floor and exact are the same, t = 1.0.  When exact is almost a whole increment greater than floor, t approaches 0.0.
                // If mFloor is clamped to MICROFACET_DISTRIBUTION_RESOLUTION -1, then mExact will be much larger, so t = 0.0.
                double t = Math.max(0.0, 1.0 + mFloor - mExact);

                double diffuseFactor = getDiffuseFactor(geomRatio);

                for (int b = 0; b < settings.basisCount; b++)
                {
                    // diffuse
                    mA.set(p, b, addlWeight * solution.getWeights(p).get(b) * diffuseFactor);

                    // specular
                    if (mExact < settings.microfacetDistributionResolution)
                    {
                        // Iterate over the available step functions in the basis.
                        for (int s = 0; s < settings.microfacetDistributionResolution; s++)
                        {
                            // Evaluate each step function twice, to the left and right of the current sample.
                            double fFloor = stepBasis.evaluate(s, mFloor);
                            double fCeil = stepBasis.evaluate(s, mFloor + 1);

                            // Blend between the two sampled locations.
                            // In the case of a simple step function, fFloor + fCeil will be either 1 or 0
                            // except at a boundary, where fFloor will be 1 and fCeil will be 0.
                            double fInterp = fFloor * t + fCeil * (1 - t);

                            // Index of the column where the coefficient will be stored in the big matrix.
                            int j = settings.basisCount * (s + 1) + b;

                            // specular with blending between the two sampled locations.
                            mA.set(p, j, addlWeight * geomRatio * solution.getWeights(p).get(b) * fInterp);
                        }
                    }
                }
            }
        }

        SimpleMatrix mATA = new SimpleMatrix(mA.numCols(), mA.numCols());
        SimpleMatrix vATyRed = new SimpleMatrix(mA.numCols(), 1);
        SimpleMatrix vATyGreen = new SimpleMatrix(mA.numCols(), 1);
        SimpleMatrix vATyBlue = new SimpleMatrix(mA.numCols(), 1);

        // Low level operations to avoid using unnecessary memory.
        multTransA(mA.getMatrix(), mA.getMatrix(), mATA.getMatrix());
        multTransA(mA.getMatrix(), yRed.getMatrix(), vATyRed.getMatrix());
        multTransA(mA.getMatrix(), yGreen.getMatrix(), vATyGreen.getMatrix());
        multTransA(mA.getMatrix(), yBlue.getMatrix(), vATyBlue.getMatrix());

        for (int i = 0; i < mATA.numRows(); i++)
        {
            assert Math.abs(vATyRed.get(i, 0) - contribution.rhs[0].get(i, 0)) <= vATyRed.get(i, 0) * 0.001;
            assert Math.abs(vATyGreen.get(i, 0) - contribution.rhs[1].get(i, 0)) <= vATyGreen.get(i, 0) * 0.001;
            assert Math.abs(vATyBlue.get(i, 0) - contribution.rhs[2].get(i, 0)) <= vATyBlue.get(i, 0) * 0.001;

            for (int j = 0; j < mATA.numCols(); j++)
            {
                assert Math.abs(mATA.get(i, j) - contribution.lhs.get(i, j)) <= mATA.get(i, j) * 0.001;
            }
        }
    }
}
