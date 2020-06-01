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

package tetzlaff.ibrelight.export.nam2018;

import java.util.stream.IntStream;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;

/**
 * A helper class to maintain state necessary to efficiently build the matrix that can solve for reflectance.
 */
final class ReflectanceMatrixBuilder
{
    private static final double PI_SQUARED = Math.PI * Math.PI;

    private int mPrevious = Nam2018Request.MICROFACET_DISTRIBUTION_RESOLUTION;

    /**
     * Stores a running total (for each pair of basis functions) of the weighted sum of geometric factors.
     */
    private final SimpleMatrix weightedGeomSum = new SimpleMatrix(Nam2018Request.BASIS_COUNT, Nam2018Request.BASIS_COUNT, DMatrixRMaj.class);

    /**
     * Stores a running total (for each pair of basis functions) of the weighted sum of squared geometric factors.
     */
    private final SimpleMatrix weightedGeomSquaredSum = new SimpleMatrix(Nam2018Request.BASIS_COUNT, Nam2018Request.BASIS_COUNT, DMatrixRMaj.class);

    /**
     *  Stores a running total (for each pair of basis functions) of the weighted sum of squared geometric factors with additional linear interpolation weights.
     */
    private final SimpleMatrix weightedGeomSquaredBlendedSum = new SimpleMatrix(Nam2018Request.BASIS_COUNT, Nam2018Request.BASIS_COUNT, DMatrixRMaj.class);

    /**
     * Stores a running total (for each basis function of the weighted sum of reflectance measurements by color channel (red).
     */
    private final SimpleMatrix weightedGeomRedSum = new SimpleMatrix(Nam2018Request.BASIS_COUNT, 1, DMatrixRMaj.class);

    /**
     * Stores a running total (for each basis function of the weighted sum of reflectance measurements by color channel (green).
     */
    private final SimpleMatrix weightedGeomGreenSum = new SimpleMatrix(Nam2018Request.BASIS_COUNT, 1, DMatrixRMaj.class);

    /**
     * Stores a running total (for each basis function of the weighted sum of reflectance measurements by color channel (blue).
     */
    private final SimpleMatrix weightedGeomBlueSum = new SimpleMatrix(Nam2018Request.BASIS_COUNT, 1, DMatrixRMaj.class);

    /**
     * LHS
     */
    private final SimpleMatrix contributionATA;

    /**
     * RHS for red
     */
    private final SimpleMatrix contributionATyRed;

    /**
     * RHS for green
     */
    private final SimpleMatrix contributionATyGreen;

    /**
     * RHS for blue
     */
    private final SimpleMatrix contributionATyBlue;

    /**
     * Color and visibility components of the samples
     */
    private final float[] colorAndVisibility;

    /**
     * Halfway angles and geometric factors for the samples.
     */
    private final float[] halfwayAndGeom;

    /**
     * Weight solution from the previous iteration.
     */
    private final SimpleMatrix weightSolution;

    /**
     * Construct by accepting matrices where the final results will be stored.
     * @param contributionATA LHS
     * @param contributionATyRed RHS for red
     * @param contributionATyGreen RHS for green
     * @param contributionATyBlue RHS for blue
     */
    ReflectanceMatrixBuilder(float[] colorAndVisibility, float[] halfwayAndGeom, SimpleMatrix weightSolution, SimpleMatrix contributionATA,
        SimpleMatrix contributionATyRed, SimpleMatrix contributionATyGreen, SimpleMatrix contributionATyBlue)
    {
        this.contributionATA = contributionATA;
        this.contributionATyRed = contributionATyRed;
        this.contributionATyGreen = contributionATyGreen;
        this.contributionATyBlue = contributionATyBlue;

        this.weightSolution = weightSolution;

        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.colorAndVisibility = colorAndVisibility;

        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.halfwayAndGeom = halfwayAndGeom;
    }

    public void execute()
    {
        // Sort pixel samples within a view by the halfway direction so that we avoid repeating the same additions over and over again
        IntStream.range(0, halfwayAndGeom.length / 4)
            .filter(p -> colorAndVisibility[4 * p + 3] > 0) // Eliminate pixels without valid samples
            .boxed() // Box integers to use custom sorting function
            .sorted((p1, p2) -> -Float.compare(halfwayAndGeom[4 * p1], halfwayAndGeom[4 * p2])) // Should sort descending to visit high m values first
            .forEachOrdered(this::processSample);

        // Flush out final running totals into the contribution matrix and vectors.
        updateContributionFromRunningTotals(0);
    }

    private void processSample(int p)
    {
        // Calculate which discretized MDF element the current sample belongs to.
        double mExact = halfwayAndGeom[4 * p] * Nam2018Request.MICROFACET_DISTRIBUTION_RESOLUTION;
        int mFloor = Math.min(Nam2018Request.MICROFACET_DISTRIBUTION_RESOLUTION - 1, (int) Math.floor(mExact));

        // If mFloor changed, it's time to update the ATA matrix and ATy vector
        assert this.mPrevious >= mFloor; // mFloor should be decreasing over time due to sorting order.
        if (mFloor < this.mPrevious)
        {
            this.updateContributionFromRunningTotals(mFloor);

            // Zero out the blended sum after every time that mFloor changes,
            // since it should only apply to a single m-value (as opposed to the other sums which continue to accumulate).
            this.weightedGeomSquaredBlendedSum.zero();
        }

        // When floor and exact are the same, t = 1.0.  When exact is almost a whole increment greater than floor, t approaches 0.0.
        // If mFloor is clamped to MICROFACET_DISTRIBUTION_RESOLUTION -1, then mExact will be much larger, so t = 0.0.
        double t = Math.max(0.0, 1.0 + mFloor - mExact);

        // Regardless of whether mFloor changed: Update running total for each pair of basis functions,
        // and add blended samples to elements where no work is saved by deferring the update to the matrix or vector.
        for (int b1 = 0; b1 < Nam2018Request.BASIS_COUNT; b1++)
        {
            // Updates to ATy

            double weightedReflectanceRed   = weightSolution.get(b1, p) * colorAndVisibility[4 * p];
            double weightedReflectanceGreen = weightSolution.get(b1, p) * colorAndVisibility[4 * p + 1];
            double weightedReflectanceBlue  = weightSolution.get(b1, p) * colorAndVisibility[4 * p + 2];

            // For each basis function: update the vector.
            // Top partition of the vector corresponds to diffuse coefficients
            contributionATyRed.set(b1, 0, contributionATyRed.get(b1, 0) + weightedReflectanceRed / Math.PI);
            contributionATyGreen.set(b1, 0, contributionATyGreen.get(b1, 0) + weightedReflectanceGreen / Math.PI);
            contributionATyBlue.set(b1, 0, contributionATyBlue.get(b1, 0) + weightedReflectanceBlue / Math.PI);

            double weightedGeomReflectanceRed   = halfwayAndGeom[4 * p + 1] * weightedReflectanceRed;
            double weightedGeomReflectanceGreen = halfwayAndGeom[4 * p + 1] * weightedReflectanceGreen;
            double weightedGeomReflectanceBlue  = halfwayAndGeom[4 * p + 1] * weightedReflectanceBlue;

            // Bottom partition of the vector corresponds to specular coefficients.
            // Scale contribution due to current m-value by blending weight t to account for linear interpolation.
            // Accumulation due to greater m-values should already have been added to the vector the last time an m-value changed
            int i = Nam2018Request.BASIS_COUNT * (mFloor + 1) + b1;
            contributionATyRed.set(i, 0, contributionATyRed.get(i, 0) + t * weightedGeomReflectanceRed);
            contributionATyGreen.set(i, 0, contributionATyGreen.get(i, 0) + t * weightedGeomReflectanceGreen);
            contributionATyBlue.set(i, 0, contributionATyBlue.get(i, 0) + t * weightedGeomReflectanceBlue);

            // Update running totals.
            weightedGeomRedSum.set(b1, 0, weightedGeomRedSum.get(b1, 0) + weightedGeomReflectanceRed);
            weightedGeomGreenSum.set(b1, 0, weightedGeomGreenSum.get(b1, 0) + weightedGeomReflectanceGreen);
            weightedGeomBlueSum.set(b1, 0, weightedGeomBlueSum.get(b1, 0) + weightedGeomReflectanceBlue);

            for (int b2 = 0; b2 < Nam2018Request.BASIS_COUNT; b2++)
            {
                // Updates to ATA

                // Update non-squared total without blending weight.
                double weightProduct = weightSolution.get(b1, p) * weightSolution.get(b2, p);
                double weightedGeom = weightProduct * halfwayAndGeom[4 * p + 1];
                weightedGeomSum.set(b1, b2, weightedGeomSum.get(b1, b2) + weightedGeom);

                // Update squared total without blending weight.
                double weightedGeomSquared = weightedGeom * halfwayAndGeom[4 * p + 1];
                weightedGeomSquaredSum.set(b1, b2, weightedGeomSquaredSum.get(b1, b2) + weightedGeomSquared);

                // Update squared total with blending weight.
                double weightedGeomSquaredBlended = t * weightedGeomSquared;
                weightedGeomSquaredBlendedSum.set(b1, b2, weightedGeomSquaredSum.get(b1, b2) + weightedGeomSquaredBlended);

                // Top left partition of the matrix: row and column both correspond to diffuse coefficients
                contributionATA.set(b1, b2, contributionATA.get(b1, b2) + weightProduct / PI_SQUARED);

                // Top right and bottom left partitions of the matrix:
                // row corresponds to diffuse coefficients and column corresponds to specular, or vice-versa.
                contributionATA.set(i, b2, contributionATA.get(i, b2) + t * weightedGeom / Math.PI);
                contributionATA.set(b2, i, contributionATA.get(b2, i) + t * weightedGeom / Math.PI);

                // Bottom right partition of the matrix: row and column both correspond to specular.
                // Update "corner" element with squared blending weight.
                int j = Nam2018Request.BASIS_COUNT * (mFloor + 1) + b2;
                contributionATA.set(i, j, contributionATA.get(i, j) + t * weightedGeomSquaredBlended);
            }
        }

        // Update holder of previous mFloor value.
        this.mPrevious = mFloor;
    }

    /**
     * Updates the contribution matrix and vectors for a particular range of m-values, given certain running totals.
     * Usually called when building the reflectance matrix, after the m-value changes.
     * Also called at the end of that process to flush out the final set of running totals.
     * @param currentM The current "m" value of the sample that is being processed. Samples are to be visited in order of decreasing "m".
     */
    private void updateContributionFromRunningTotals(int currentM)
    {
        // Add the running total to elements of the ATA matrix and the ATy vector corresponding to the newly visited mFloor
        // as well as any m-values skipped over.
        // These elements also need to get some more contributions that have blending weights that are yet to be visited,
        // but that will be handled later, when a sample is visited for some matrix elements, or the next time mFloor changes for others.
        for (int b1 = 0; b1 < Nam2018Request.BASIS_COUNT; b1++)
        {
            // This loop usually would only one once, but could run multiple times if we skipped a few m values.
            for (int m1 = mPrevious - 1; m1 >= currentM; m1--)
            {
                int i = Nam2018Request.BASIS_COUNT * (m1 + 1) + b1;

                // Update ATy vector
                contributionATyRed.set(i, 0, contributionATyRed.get(i, 0)
                    + weightedGeomRedSum.get(b1, 0) / PI_SQUARED);
                contributionATyGreen.set(i, 0, contributionATyGreen.get(i, 0)
                    + weightedGeomGreenSum.get(b1, 0) / PI_SQUARED);
                contributionATyBlue.set(i, 0, contributionATyBlue.get(i, 0)
                    + weightedGeomBlueSum.get(b1, 0) / PI_SQUARED);

                // Update ATA matrix
                for (int b2 = 0; b2 < Nam2018Request.BASIS_COUNT; b2++)
                {
                    // Top right and bottom left partitions of the matrix:
                    // row corresponds to diffuse coefficients and column corresponds to specular, or vice-versa.
                    // The matrix is symmetric so we also need to swap row and column and update that way.
                    contributionATA.set(i, b2, contributionATA.get(i, b2) + weightedGeomSum.get(b1, b2) / Math.PI);
                    contributionATA.set(b2, i, contributionATA.get(b2, i) + weightedGeomSum.get(b1, b2) / Math.PI);


                    // Bottom right partition of the matrix: row and column both correspond to specular.

                    // Handle "corner" case where m1 = m2 (don't want to repeat with row and column swapped as elements would then be duplicated).
                    int j = Nam2018Request.BASIS_COUNT * (m1 + 1) + b2;
                    contributionATA.set(i, j, contributionATA.get(i, j) + weightedGeomSquaredSum.get(b1, b2));

                    // Visit every element of the microfacet distribution that is beyond m1.
                    // This is because the form of ATA is such that the values in the matrix are determined by the lower of the two m-values.
                    for (int m2 = m1 + 1; m2 < Nam2018Request.MICROFACET_DISTRIBUTION_RESOLUTION; m2++)
                    {
                        j = Nam2018Request.BASIS_COUNT * (m2 + 1) + b2;

                        // Add the current value of the running total to the appropriate location in the matrix.
                        // The matrix is symmetric so we also need to swap row and column and update that way.
                        contributionATA.set(i, j, contributionATA.get(i, j) + weightedGeomSquaredSum.get(b1, b2));
                        contributionATA.set(j, i, contributionATA.get(j, i) + weightedGeomSquaredSum.get(b1, b2));
                    }
                }
            }
        }

        // Add the total of recently visited samples with blending weights to elements of the ATA matrix corresponding to the old mFloor.
        // Bottom right partition of the matrix: row and column both correspond to specular.
        for (int b1 = 0; b1 < Nam2018Request.BASIS_COUNT; b1++)
        {
            int i = Nam2018Request.BASIS_COUNT * (mPrevious + 1) + b1;

            for (int b2 = 0; b2 < Nam2018Request.BASIS_COUNT; b2++)
            {
                // The "corner case" will be handled immediately when a sample is visited as it only affects a single element of the
                // matrix and thus no work is saved by waiting for mFloor to change.

                // Visit every element of the microfacet distribution that is beyond m1.
                // This is because the form of ATA is such that the values in the matrix are determined by the lower of the two m-values.
                for (int m2 = mPrevious + 1; m2 < Nam2018Request.MICROFACET_DISTRIBUTION_RESOLUTION; m2++)
                {
                    int j = Nam2018Request.BASIS_COUNT * (m2 + 1) + b2;

                    // Add the current value of the running total to the appropriate location in the matrix.
                    // The matrix is symmetric so we also need to swap row and column and update that way.
                    contributionATA.set(i, j, contributionATA.get(i, j) + weightedGeomSquaredSum.get(b1, b2));
                    contributionATA.set(j, i, contributionATA.get(j, i) + weightedGeomSquaredSum.get(b1, b2));
                }
            }
        }
    }
}
