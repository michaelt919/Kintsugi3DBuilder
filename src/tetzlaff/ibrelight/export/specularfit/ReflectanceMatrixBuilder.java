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

import java.util.stream.IntStream;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.util.ColorList;

import static org.ejml.dense.row.CommonOps_DDRM.multTransA;

/**
 * A helper class to maintain state necessary to efficiently build the matrix that can solve for reflectance.
 */
final class ReflectanceMatrixBuilder
{
    // Set to true to validate the implementation (should generally be turned off for much better efficiency).
    private static final boolean VALIDATE = true;

    private int mPrevious = 0;

    /**
     * Stores a running total (for each pair of basis functions) of the weighted sum of geometric factors.
     */
    private final SimpleMatrix weightedGeomSum;

    /**
     * Stores a running total (for each pair of basis functions) of the weighted sum of squared geometric factors.
     */
    private final SimpleMatrix weightedGeomSquaredSum;

    /**
     *  Stores a running total (for each pair of basis functions) of the weighted sum of squared geometric factors with additional linear interpolation weights.
     */
    private final SimpleMatrix weightedGeomSquaredBlendedSum;

    /**
     * Stores a running total (for each basis function of the weighted sum of reflectance measurements by color channel (red).
     */
    private final SimpleMatrix weightedGeomRedSum;

    /**
     * Stores a running total (for each basis function of the weighted sum of reflectance measurements by color channel (green).
     */
    private final SimpleMatrix weightedGeomGreenSum;

    /**
     * Stores a running total (for each basis function of the weighted sum of reflectance measurements by color channel (blue).
     */
    private final SimpleMatrix weightedGeomBlueSum;

    /**
     * Stores both the LHS and RHS of the system to be solved.
     */
    private final ReflectanceMatrixSystem contribution;

    /**
     * Color and visibility components of the samples
     */
    private final ColorList colorAndVisibility;

    /**
     * Halfway angles and geometric factors for the samples.
     */
    private final ColorList halfwayAndGeom;

    /**
     * Weight solution from the previous iteration.
     */
    private final SpecularFitSolution solution;

    /**
     * Settings to be used for the specular fit; associated with the SpecularFitSolution.
     */
    private final SpecularFitSettings settings;

    /**
     * Assumed metallicity of the material (affects handling of diffuse reflectance).
     */
    private final double metallicity;

    /**
     * Construct by accepting matrices where the final results will be stored.
     */
    ReflectanceMatrixBuilder(ColorList colorAndVisibility, ColorList halfwayAndGeom, SpecularFitSolution solution, ReflectanceMatrixSystem contribution, double metallicity)
    {
        this.contribution = contribution;

        this.solution = solution;

        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.colorAndVisibility = colorAndVisibility;

        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.halfwayAndGeom = halfwayAndGeom;

        this.metallicity = metallicity;

        settings = solution.getSettings();

        // Initialize running totals
        weightedGeomSum = new SimpleMatrix(solution.getSettings().basisCount, solution.getSettings().basisCount, DMatrixRMaj.class);
        weightedGeomSquaredSum = new SimpleMatrix(solution.getSettings().basisCount, solution.getSettings().basisCount, DMatrixRMaj.class);
        weightedGeomSquaredBlendedSum = new SimpleMatrix(solution.getSettings().basisCount, solution.getSettings().basisCount, DMatrixRMaj.class);
        weightedGeomRedSum = new SimpleMatrix(solution.getSettings().basisCount, 1, DMatrixRMaj.class);
        weightedGeomGreenSum = new SimpleMatrix(solution.getSettings().basisCount, 1, DMatrixRMaj.class);
        weightedGeomBlueSum = new SimpleMatrix(solution.getSettings().basisCount, 1, DMatrixRMaj.class);
    }

    public void execute()
    {
        // Sort pixel samples within a view by the halfway direction so that we avoid repeating the same additions over and over again
        IntStream.range(0, halfwayAndGeom.size())
            .filter(p -> colorAndVisibility.get(p).w > 0) // Eliminate pixels without valid samples
            .boxed() // Box integers to use custom sorting function
            .sorted((p1, p2) -> Float.compare(halfwayAndGeom.get(p1).x, halfwayAndGeom.get(p2).x)) // Should sort ascending to visit low m values first
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
        float halfwayIndex = halfwayAndGeom.get(p).x;
        float geomRatio = halfwayAndGeom.get(p).y;
        float addlWeight = halfwayAndGeom.get(p).z;

        // Calculate which discretized MDF element the current sample belongs to.
        double mExact = halfwayIndex * settings.microfacetDistributionResolution;
        int mFloor = Math.min(settings.microfacetDistributionResolution - 1, (int) Math.floor(mExact));

        // If mFloor changed, it's time to update the ATA matrix and ATy vector
        assert mPrevious <= mFloor : "mPrevious: " + mPrevious + " mFloor: " + mFloor + " mExact: " + mExact; // mFloor should be increasing over time due to sorting order.
        if (mFloor > mPrevious)
        {
            updateContributionFromRunningTotals(mFloor);

            // Zero out the blended sum after every time that mFloor changes,
            // since it should only apply to a single m-value (as opposed to the other sums which continue to accumulate).
            weightedGeomSquaredBlendedSum.zero();
        }

        // When floor and exact are the same, t = 1.0.  When exact is almost a whole increment greater than floor, t approaches 0.0.
        // If mFloor is clamped to MICROFACET_DISTRIBUTION_RESOLUTION -1, then mExact will be much larger, so t = 0.0.
        double t = Math.max(0.0, 1.0 + mFloor - mExact);

        double addlWeightSquared = addlWeight * addlWeight;

        double diffuseFactor = getDiffuseFactor(geomRatio);
        double diffuseFactorSquared = diffuseFactor * diffuseFactor;

        // Regardless of whether mFloor changed: Update running total for each pair of basis functions,
        // and add blended samples to elements where no work is saved by deferring the update to the matrix or vector.
        for (int b1 = 0; b1 < settings.basisCount; b1++)
        {
            // Updates to ATy

            double weightedReflectanceRed   = solution.getWeights(p).get(b1) * addlWeightSquared * colorAndVisibility.get(p).x;
            double weightedReflectanceGreen = solution.getWeights(p).get(b1) * addlWeightSquared * colorAndVisibility.get(p).y;
            double weightedReflectanceBlue  = solution.getWeights(p).get(b1) * addlWeightSquared * colorAndVisibility.get(p).z;

            // For each basis function: update the vector.
            // Top partition of the vector corresponds to diffuse coefficients
            contribution.vectorATyRed.set(b1, 0, contribution.vectorATyRed.get(b1, 0) + weightedReflectanceRed * diffuseFactor);
            contribution.vectorATyGreen.set(b1, 0, contribution.vectorATyGreen.get(b1, 0) + weightedReflectanceGreen * diffuseFactor);
            contribution.vectorATyBlue.set(b1, 0, contribution.vectorATyBlue.get(b1, 0) + weightedReflectanceBlue * diffuseFactor);

            int i = settings.basisCount * (mFloor + 1) + b1;

            if (mExact < settings.microfacetDistributionResolution)
            {
                double weightedGeomReflectanceRed = geomRatio * weightedReflectanceRed;
                double weightedGeomReflectanceGreen = geomRatio * weightedReflectanceGreen;
                double weightedGeomReflectanceBlue = geomRatio * weightedReflectanceBlue;

                // Bottom partition of the vector corresponds to specular coefficients.
                // Scale contribution due to current m-value by blending weight t to account for linear interpolation.
                // Accumulation due to greater m-values should already have been added to the vector the last time an m-value changed
                contribution.vectorATyRed.set(i, 0, contribution.vectorATyRed.get(i, 0) + t * weightedGeomReflectanceRed);
                contribution.vectorATyGreen.set(i, 0, contribution.vectorATyGreen.get(i, 0) + t * weightedGeomReflectanceGreen);
                contribution.vectorATyBlue.set(i, 0, contribution.vectorATyBlue.get(i, 0) + t * weightedGeomReflectanceBlue);

                // Update running totals.
                weightedGeomRedSum.set(b1, 0, weightedGeomRedSum.get(b1, 0) + weightedGeomReflectanceRed);
                weightedGeomGreenSum.set(b1, 0, weightedGeomGreenSum.get(b1, 0) + weightedGeomReflectanceGreen);
                weightedGeomBlueSum.set(b1, 0, weightedGeomBlueSum.get(b1, 0) + weightedGeomReflectanceBlue);
            }

            for (int b2 = 0; b2 < settings.basisCount; b2++)
            {
                // Updates to ATA

                // Top left partition of the matrix: row and column both correspond to diffuse coefficients
                double weightProduct = solution.getWeights(p).get(b1) * solution.getWeights(p).get(b2) * addlWeightSquared;
                contribution.matrixATA.set(b1, b2, contribution.matrixATA.get(b1, b2) + weightProduct * diffuseFactorSquared);

                if (mExact < settings.microfacetDistributionResolution)
                {
                    // Update non-squared total without blending weight.
                    double weightedGeom = weightProduct * geomRatio;
                    weightedGeomSum.set(b1, b2, weightedGeomSum.get(b1, b2) + weightedGeom);

                    // Update squared total without blending weight.
                    double weightedGeomSquared = weightedGeom * geomRatio;
                    weightedGeomSquaredSum.set(b1, b2, weightedGeomSquaredSum.get(b1, b2) + weightedGeomSquared);

                    // Update squared total with blending weight.
                    double weightedGeomSquaredBlended = t * weightedGeomSquared;
                    weightedGeomSquaredBlendedSum.set(b1, b2, weightedGeomSquaredBlendedSum.get(b1, b2) + weightedGeomSquaredBlended);

                    // Top right and bottom left partitions of the matrix:
                    // row corresponds to diffuse coefficients and column corresponds to specular, or vice-versa.
                    contribution.matrixATA.set(i, b2, contribution.matrixATA.get(i, b2) + t * weightedGeom * diffuseFactor);
                    contribution.matrixATA.set(b2, i, contribution.matrixATA.get(b2, i) + t * weightedGeom  * diffuseFactor);

                    // Bottom right partition of the matrix: row and column both correspond to specular.
                    // Update "corner" element with squared blending weight.
                    int j = settings.basisCount * (mFloor + 1) + b2;
                    contribution.matrixATA.set(i, j, contribution.matrixATA.get(i, j) + t * weightedGeomSquaredBlended);
                }
            }
        }

        // Update holder of previous mFloor value.
        mPrevious = mFloor;
    }

    /**
     * Updates the contribution matrix and vectors for a particular range of m-values, given certain running totals.
     * Usually called when building the reflectance matrix, after the m-value changes.
     * Also called at the end of that process to flush out the final set of running totals.
     * @param mCurrent The current "m" value of the sample that is being processed. Samples are to be visited in order of decreasing "m".
     */
    private void updateContributionFromRunningTotals(int mCurrent)
    {
        // Add the running total to elements of the ATA matrix and the ATy vector corresponding to the newly visited m
        // as well as any m-values skipped over.
        // These elements also need to get some more contributions that have blending weights that are yet to be visited,
        // but that will be handled later, when a sample is visited for some matrix elements, or the next time m changes for others.
        for (int b1 = 0; b1 < settings.basisCount; b1++)
        {
            // This loop usually would only one once, but could run multiple times if we skipped a few m values.
            for (int m1 = mPrevious + 1; m1 <= mCurrent; m1++)
            {
                int i = settings.basisCount * (m1 + 1) + b1;

                // Update ATy vector
                contribution.vectorATyRed.set(i, 0, contribution.vectorATyRed.get(i, 0) + weightedGeomRedSum.get(b1, 0));
                contribution.vectorATyGreen.set(i, 0, contribution.vectorATyGreen.get(i, 0) + weightedGeomGreenSum.get(b1, 0));
                contribution.vectorATyBlue.set(i, 0, contribution.vectorATyBlue.get(i, 0) + weightedGeomBlueSum.get(b1, 0));

                // Update ATA matrix
                for (int b2 = 0; b2 < settings.basisCount; b2++)
                {
                    // Top right and bottom left partitions of the matrix:
                    // row corresponds to diffuse coefficients and column corresponds to specular, or vice-versa.
                    // The matrix is symmetric so we also need to swap row and column and update that way.
                    contribution.matrixATA.set(i, b2, contribution.matrixATA.get(i, b2) +
                        metallicity * weightedGeomSquaredSum.get(b1, b2) + (1 - metallicity) * weightedGeomSum.get(b1, b2) / Math.PI);
                    contribution.matrixATA.set(b2, i, contribution.matrixATA.get(b2, i) +
                        metallicity * weightedGeomSquaredSum.get(b2, b1) + (1 - metallicity) * weightedGeomSum.get(b2, b1) / Math.PI);

                    // Bottom right partition of the matrix: row and column both correspond to specular.

                    // Handle "corner" case where m1 = m2 (don't want to repeat with row and column swapped as elements would then be duplicated).
                    int j = settings.basisCount * (m1 + 1) + b2;
                    contribution.matrixATA.set(i, j, contribution.matrixATA.get(i, j) + weightedGeomSquaredSum.get(b1, b2));

                    // Visit every element of the microfacet distribution that comes after m1.
                    // This is because the form of ATA is such that the values in the matrix are determined by the lower of the two m-values.
                    for (int m2 = m1 + 1; m2 < settings.microfacetDistributionResolution; m2++)
                    {
                        j = settings.basisCount * (m2 + 1) + b2;

                        // Add the current value of the running total to the appropriate location in the matrix.
                        // The matrix is symmetric so we also need to swap row and column and update that way.
                        contribution.matrixATA.set(i, j, contribution.matrixATA.get(i, j) + weightedGeomSquaredSum.get(b1, b2));
                        contribution.matrixATA.set(j, i, contribution.matrixATA.get(j, i) + weightedGeomSquaredSum.get(b2, b1));
                    }
                }
            }
        }

        // Add the total of recently visited samples with blending weights to elements of the ATA matrix corresponding to the old m.
        // Bottom right partition of the matrix: row and column both correspond to specular.
        for (int b1 = 0; b1 < settings.basisCount; b1++)
        {
            int i = settings.basisCount * (mPrevious + 1) + b1;

            for (int b2 = 0; b2 < settings.basisCount; b2++)
            {
                // The "corner case" was handled immediately when a sample was visited as it only affects a single element of the
                // matrix and thus no work is saved by waiting for m to change.

                // Visit every element of the microfacet distribution that comes after mPrevious.
                // This is because the form of ATA is such that the values in the matrix are determined by the lower of the two m-values.
                for (int m2 = mPrevious + 1; m2 < settings.microfacetDistributionResolution; m2++)
                {
                    int j = settings.basisCount * (m2 + 1) + b2;

                    // Add the current value of the running total with blending (linear interpolation) weights to the appropriate location in the matrix.
                    // The matrix is symmetric so we also need to swap row and column and update that way.
                    contribution.matrixATA.set(i, j, contribution.matrixATA.get(i, j) + weightedGeomSquaredBlendedSum.get(b1, b2));
                    contribution.matrixATA.set(j, i, contribution.matrixATA.get(j, i) + weightedGeomSquaredBlendedSum.get(b2, b1));
                }
            }
        }
    }

    private void validate()
    {
        // Calculate the matrix products the slow way to make sure that the implementation is correct.
        SimpleMatrix mA = new SimpleMatrix(colorAndVisibility.size(), settings.basisCount * (settings.microfacetDistributionResolution + 1), DMatrixRMaj.class);
        SimpleMatrix yRed = new SimpleMatrix(colorAndVisibility.size(), 1);
        SimpleMatrix yGreen = new SimpleMatrix(colorAndVisibility.size(), 1);
        SimpleMatrix yBlue = new SimpleMatrix(colorAndVisibility.size(), 1);

        for (int p = 0; p < colorAndVisibility.size(); p++)
        {
            if (colorAndVisibility.get(p).w > 0)
            {
                float halfwayIndex = halfwayAndGeom.get(p).x;
                float geomRatio = halfwayAndGeom.get(p).y;
                float addlWeight = halfwayAndGeom.get(p).z;

                // Calculate which discretized MDF element the current sample belongs to.
                double mExact = halfwayIndex * settings.microfacetDistributionResolution;
                int mFloor = Math.min(settings.microfacetDistributionResolution - 1, (int) Math.floor(mExact));

                yRed.set(p, addlWeight * colorAndVisibility.get(p).x);
                yGreen.set(p, addlWeight * colorAndVisibility.get(p).y);
                yBlue.set(p, addlWeight * colorAndVisibility.get(p).z);

                // When floor and exact are the same, t = 1.0.  When exact is almost a whole increment greater than floor, t approaches 0.0.
                // If mFloor is clamped to MICROFACET_DISTRIBUTION_RESOLUTION -1, then mExact will be much larger, so t = 0.0.
                double t = Math.max(0.0, 1.0 + mFloor - mExact);

                double diffuseFactor = getDiffuseFactor(geomRatio);

                for (int b = 0; b < settings.basisCount; b++)
                {
                    // diffuse
                    mA.set(p, b, addlWeight * solution.getWeights(p).get(b) * diffuseFactor);

                    if (mExact < settings.microfacetDistributionResolution)
                    {
                        int j = settings.basisCount * (mFloor + 1) + b;

                        // specular with blending for first non-zero element
                        mA.set(p, j, t * addlWeight * geomRatio * solution.getWeights(p).get(b));

                        for (int m = mFloor + 1; m < settings.microfacetDistributionResolution; m++)
                        {
                            j = settings.basisCount * (m + 1) + b;
                            // specular (no blending)
                            mA.set(p, j, addlWeight * geomRatio * solution.getWeights(p).get(b));
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
            assert Math.abs(vATyRed.get(i, 0) - contribution.vectorATyRed.get(i, 0)) <= vATyRed.get(i, 0) * 0.001;
            assert Math.abs(vATyGreen.get(i, 0) - contribution.vectorATyGreen.get(i, 0)) <= vATyGreen.get(i, 0) * 0.001;
            assert Math.abs(vATyBlue.get(i, 0) - contribution.vectorATyBlue.get(i, 0)) <= vATyBlue.get(i, 0) * 0.001;

            for (int j = 0; j < mATA.numCols(); j++)
            {
                assert Math.abs(mATA.get(i, j) - contribution.matrixATA.get(i, j)) <= mATA.get(i, j) * 0.001;
            }
        }
    }
}
