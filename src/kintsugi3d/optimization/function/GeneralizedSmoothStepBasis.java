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

package kintsugi3d.optimization.function;

import kintsugi3d.optimization.MatrixSystem;

import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.function.ObjIntConsumer;

/**
 * A basis function library that uses a smoothstep rather than a hard step.
 */
public class GeneralizedSmoothStepBasis implements BasisFunctions
{
    private final int resolution;
    private final double metallicity;
    private final int minSmoothstepWidth;
    private final int maxSmoothstepWidth;
    private final DoubleUnaryOperator smoothstep;

    /**
     * Constructs a "library" of smoothstep basis functions.
     * The actual smoothstep function can be customized.
     * The library will consist of a set of smoothsteps that evaluate to:
     *   1.0 when the parameter is 0,
     *   0.0 when the parameter is at the end of the optimized domain.
     *   some value between 0.0 and 1.0 when the parameter is within the optimzed domain.
     * Each smoothstep function in the library will start and end at a different location.
     * The endpoints will be spaced out evenly across the domain.
     * The starting point will generally be a fixed distance from the endpoint, but may be compressed when the
     * endpoint is close to 0.
     * @param resolution The number of discrete elements in the domain of the function to be optimized,
     *                   which determines the number of functions in the library.
     *                   An exception will be thrown if this number is less than 1.
     * @param metallicity The assumed "metallicity" of the function being optimized.
     *                    This value will be clamped between 0 and 1.
     * @param minSmoothstepWidth The minimum distance between the start and end of each smoothstep function.
     *                           This is used to determine the width of functions ending closest to the starting point.
     *                           Functions that would be narrower will be omitted from the basis; thus, the size of the
     *                           optimization domain (the number of functions in the library) is resolution - minSmoothstepWidth + 1.
     *                           A min width of 1 will include all possible functions in the optimization domain.
     *                           The min width will be clamped to 1 if set lower than 1.
     * @param maxSmoothstepWidth The typical distance between the start and end of each smoothstep function in the library.
     *                        Each smoothstep function will end (with a value of 0.0) at a different location
     *                        (spaced evenly across the domain) and will start (with a value of 1.0) at
     *                        endpoint - maxSmoothstepWidth.  If that difference comes out to be negative, the starting
     *                        point will be clamped to zero.  The effect of this is that some basis functions with a
     *                        smaller width be included in the library, up to minSmoothstepWidth.
     *                        This is helpful when optimizing a BRDF, for instance, which may have a steeper gradient
     *                        close to the specular peak.
     *                        A max width of 1 would correspond to a hard step function.
     *                        The max width will be clamped to minSmoothstepWidth if set lower than minSmoothstepWidth.
     * @param smoothstep The actual smoothstep function to be used.  This function should have accept a domain between
     *                   0.0 and 1.0 and map the values to a range that is also between 0.0 and 1.0, where an input of
     *                   0.0 evaluates to a result of 0.0, and an input of 1.0 evaluates to a result of 1.0.
     *                   It is intended for this function to be monotonically increasing.
     *                   Note that this function will effectively be flipped in practice since the basis functions are
     *                   assumed to evaluate to 1.0 for an input parameter of 0 and decresse down to 1.0 as that
     *                   parameter increases.
     */
    public GeneralizedSmoothStepBasis(int resolution, double metallicity, int minSmoothstepWidth,
                                      int maxSmoothstepWidth, DoubleUnaryOperator smoothstep)
    {
        if (resolution < 1)
        {
            throw new IllegalArgumentException("Resolution must be greater than zero.");
        }

        this.resolution = resolution;
        this.metallicity = Math.max(0.0, Math.min(1.0, metallicity));
        this.minSmoothstepWidth = Math.max(1, minSmoothstepWidth);
        this.maxSmoothstepWidth = Math.max(minSmoothstepWidth, maxSmoothstepWidth);
        this.smoothstep = smoothstep;
    }

    @Override
    public double evaluate(int functionIndex, int value)
    {
        // The function at index i always reaches 0.0 when value = i + minSmoothstepWidth.
        // If i + minSmoothstepWidth < maxSmoothstepWidth, then the smoothstep starts right away at m=0.
        // Otherwise, the smoothstep starts at m = i + minSmoothstepWidth - maxSmoothstepWidth.
        if (value < functionIndex + minSmoothstepWidth) // <=> functionIndex + minSmoothstepWidth - value > 0
        {
            // Assuming minSmoothstepWidth is 1:
            // functionIndex = 0: range of 1; [0, 1)
            // functionIndex = 1: range of 2; [0, 2)
            // etc.
            // functionIndex = maxSmoothstepWidth: use maxSmoothstepWidth; [0, maxSmoothstepWidth)
            // functionIndex = maxSmoothstepWidth + 1: [1, maxSmoothstepWidth + 1)
            // etc.
            int effectiveWidth = Math.min(maxSmoothstepWidth, functionIndex + minSmoothstepWidth);

            int domainIndex = minSmoothstepWidth + functionIndex - value;
            if (domainIndex < effectiveWidth)
            // <=> value > functionIndex + minSmoothstepWidth - effectiveWidth [left bound of the range]
            {
                // Value is within range, use smoothstep function.
                return smoothstep.applyAsDouble((double) domainIndex / (double) effectiveWidth);
            }
            else
            {
                // Value is beyond range to the left
                return 1.0;
            }
        }
        else
        {
            // Value is beyond range to the right
            return 0.0;
        }
    }

    @Override
    public int getFunctionCount()
    {
        return resolution - minSmoothstepWidth + 1;
    }

    @Override
    public int getOptimizedDomainSize()
    {
        return resolution;
    }

    @Override
    public double getMetallicity()
    {
        return this.metallicity;
    }

    private static double lerpHelper(double fLower, double fUpper, double sumBlendWeighted, double sumUnBlendWeighted)
    {
        return fLower * sumBlendWeighted + fUpper * (sumUnBlendWeighted - sumBlendWeighted);
    }

    @Override
    public void contributeToFittingSystem(int valueCurrent, int valueNext, int instanceCount,
                                          MatrixBuilderSums sums, MatrixSystem fittingSystem)
    {
        // For a particular valueCurrent, valueCurrent-minSmoothstepWidth is the last basis function that evaluates to 0.0 @ valueCurrent,
        // so valueCurrent - minSmoothstepWidth + maxSmoothstepWidth is the first basis function that evaluates to 1.0 @ valueCurrent.
        int domainEnd = Math.min(valueCurrent - minSmoothstepWidth + maxSmoothstepWidth, getFunctionCount());
        int domainStart = Math.max(0, valueCurrent - minSmoothstepWidth);

        // Add the running total to elements of the ATA matrix and the ATy vector corresponding to the next value
        // as well as any values skipped over.
        // These elements also need to get some more contributions that have blending weights that are yet to be
        // visited, but that will be handled later, when a sample is visited for some matrix elements, or the next time
        // the value changes for others.
        for (int b1 = 0; b1 < instanceCount; b1++)
        {
            // Contribute to weights for library functions in the range [valueCurrent - 1, domainEnd].
            // This range is inclusive on both ends due to linear interpolation.
            // i.e., the library function at i=domainEnd will be interpolating from 1 to a value less than 1 for
            // parameter values between valueCurrent and valueCurrent+1.
            for (int k = domainStart; k <= domainEnd && k < getFunctionCount(); k++)
            {
                int i = instanceCount * (k + 1) + b1;

                // fLower should always be greater than fUpper. (lower/upper refers to input parameter, not evaluation result)
                double fLower = evaluate(k, valueCurrent);
                double fUpper = evaluate(k, valueCurrent + 1);

                // Update ATy vector for blended terms.
                fittingSystem.addToRHS(i, 0, lerpHelper(fLower, fUpper,
                        sums.getWeightedAnalyticTimesObservedBlended(0, b1),
                        sums.getWeightedAnalyticTimesObserved(0, b1)));
                fittingSystem.addToRHS(i, 1, lerpHelper(fLower, fUpper,
                        sums.getWeightedAnalyticTimesObservedBlended(1, b1),
                        sums.getWeightedAnalyticTimesObserved(1, b1)));
                fittingSystem.addToRHS(i, 2, lerpHelper(fLower, fUpper,
                        sums.getWeightedAnalyticTimesObservedBlended(2, b1),
                        sums.getWeightedAnalyticTimesObserved(2, b1)));

                // Update ATA matrix for blended terms.
                for (int b2 = 0; b2 < instanceCount; b2++)
                {
                    // Top right and bottom left partitions of the matrix:
                    // row corresponds to constant coefficients and column corresponds to non-constant, or vice-versa.
                    double constNonConstCrossCoeff = lerpHelper(fLower, fUpper,
                            metallicity * sums.getWeightedAnalyticSquaredBlended(b1, b2)
                                + (1 - metallicity) * sums.getWeightedAnalyticBlended(b1, b2),
                            metallicity * sums.getWeightedAnalyticSquared(b1, b2)
                                + (1 - metallicity) * sums.getWeightedAnalytic(b1, b2));

                    // The matrix is symmetric so we also need to swap row and column and update that way.
                    fittingSystem.addToLHS(i, b2, constNonConstCrossCoeff);
                    fittingSystem.addToLHS(b2, i, constNonConstCrossCoeff);


                    // Bottom right partition of the matrix: row and column both correspond to non-constant:

                    // Matrix coefficients where both row and column are in the domain:
                    // Need a nested loop to consider every combination of library functions in the domain.
                    for (int k2 = domainStart; k2 <= domainEnd && k2 < getFunctionCount(); k2++)
                    {
                        int j = instanceCount * (k2 + 1) + b2;

                        double f2Lower = evaluate(k2, valueCurrent);
                        double f2Upper = evaluate(k2, valueCurrent + 1);

                        fittingSystem.addToLHS(i, j,
                            lerpHelper(fLower, fUpper,
                                lerpHelper(f2Lower, f2Upper,
                                    sums.getWeightedAnalyticSquaredBlendedSquared(b1, b2),
                                    sums.getWeightedAnalyticSquaredBlended(b1, b2)),
                                lerpHelper(f2Lower, f2Upper,
                                        sums.getWeightedAnalyticSquaredBlended(b1, b2),
                                        sums.getWeightedAnalyticSquared(b1, b2))));
                    }

                    // Matrix coefficients where row is in the domain but column isn't, and vice-versa.
                    // Need to consider every library function that comes after domainEnd.
                    for (int k2 = domainEnd + 1; k2 < getFunctionCount(); k2++)
                    {
                        int j = instanceCount * (k2 + 1) + b2;

                        double coeff = lerpHelper(fLower, fUpper,
                                sums.getWeightedAnalyticSquaredBlended(b1, b2),
                                sums.getWeightedAnalyticSquared(b1, b2));

                        // The matrix is symmetric so we also need to swap row and column and update that way.
                        fittingSystem.addToLHS(i, j, coeff);
                        fittingSystem.addToLHS(j, i, coeff);
                    }
                }
            }

            // Contribute to weights for library functions from current domainEnd+1 through next domainEnd+1.
            // These will all evaluate to 1.0 for the current value of m so we just need the total of their weights.
            // Since the non-blend-weighted sums continue to accumulate, contributions to later library functions
            // will be deferred until the sums are complete for those functions.
            // This loop usually would only run once, but could run multiple times if we skipped a few values.
            int nextDomainEnd = Math.min(valueNext - minSmoothstepWidth + maxSmoothstepWidth, resolution);
            for (int m1 = domainEnd + 1; m1 <= nextDomainEnd && m1 < getFunctionCount(); m1++) // TODO work out why m1 < functionCount is necessary
            {
                int i = instanceCount * (m1 + 1) + b1;

                // Update ATy vector
                fittingSystem.addToRHS(i, 0, sums.getWeightedAnalyticTimesObservedCumulative(0, b1));
                fittingSystem.addToRHS(i, 1, sums.getWeightedAnalyticTimesObservedCumulative(1, b1));
                fittingSystem.addToRHS(i, 2, sums.getWeightedAnalyticTimesObservedCumulative(2, b1));

                // Update ATA matrix
                for (int b2 = 0; b2 < instanceCount; b2++)
                {
                    // Top right and bottom left partitions of the matrix:
                    // row corresponds to diffuse coefficients and column corresponds to specular, or vice-versa.
                    // The matrix is symmetric so we also need to swap row and column and update that way.
                    fittingSystem.addToLHS(i, b2,
                            metallicity * sums.getWeightedAnalyticSquaredCumulative(b1, b2)
                                    + (1 - metallicity) * sums.getWeightedAnalyticCumulative(b1, b2));
                    fittingSystem.addToLHS(b2, i,
                            metallicity * sums.getWeightedAnalyticSquaredCumulative(b2, b1)
                                    + (1 - metallicity) * sums.getWeightedAnalyticCumulative(b2, b1));

                    // Bottom right partition of the matrix: row and column both correspond to specular.

                    // Handle "corner" case where m1 = m2 (don't want to repeat with row and column swapped as elements
                    // would then be duplicated).
                    int j = instanceCount * (m1 + 1) + b2;
                    fittingSystem.addToLHS(i, j, sums.getWeightedAnalyticSquaredCumulative(b1, b2));

                    // Visit every element of the microfacet distribution that comes after m1.
                    // This is because the form of ATA is such that the values in the matrix are determined by the lower
                    // of the two values.
                    for (int m2 = m1 + 1; m2 < getFunctionCount(); m2++)
                    {
                        j = instanceCount * (m2 + 1) + b2;

                        // Add the current running total to the appropriate location in the matrix.
                        // The matrix is symmetric so we also need to swap row and column and update that way.
                        fittingSystem.addToLHS(i, j, sums.getWeightedAnalyticSquaredCumulative(b1, b2));
                        fittingSystem.addToLHS(j, i, sums.getWeightedAnalyticSquaredCumulative(b2, b1));
                    }
                }
            }
        }
    }

    @Override
    public void evaluateSolution(double constantTerm, IntToDoubleFunction nonConstantSolution, ObjIntConsumer<Double> functionConsumer)
    {
        // Keep an array that stores the sum after adding the weight for each additional library function.
        // This is used to efficiently incorporate basis functions that are already at their max value for a particular element.
        // sums[i] = sum of library function weights in the range [i, resolution) + constant term.
        double[] sums = new double[resolution + 1];

        // Constant term
        sums[resolution] = constantTerm * metallicity;
        functionConsumer.accept(sums[resolution], resolution);

        // Loop from end down to 0.
        for (int m = resolution - 1; m >= 0; m--)
        {
            // Update the running total for future elements where the current basis function will be maxed out.
            sums[m] = sums[m + 1] + nonConstantSolution.applyAsDouble(m);

            // For a particular value of m, m-minSmoothstepWidth is the last basis function that evaluates to 0.0,
            // so m-minSmoothstepWidth + maxSmoothstepWidth is the first basis function that evaluates to 1.0.
            int domainEnd = Math.min(m - minSmoothstepWidth + maxSmoothstepWidth, getFunctionCount());

            // Accounts for library functions from domainEnd through the domain max.
            // These will all evaluate to 1.0 for the current value of m so we just need the total of their weights.
            double currentTotal = sums[domainEnd];

            // Accounts for library functions in the range [m, domainEnd).
            for (int i = domainEnd - 1; i >= m - minSmoothstepWidth + 1; i--)
            {
                // Evaluate library function i for input parameter m and add to weighted total.
                currentTotal += nonConstantSolution.applyAsDouble(i) * evaluate(i, m);
            }

            // Library functions 0 through m-1 should not be able to affect the element at index m since they will evaluate to 0.
            // Total now accounts for the whole range of library functions.
            functionConsumer.accept(currentTotal, m);
        }
    }
}
