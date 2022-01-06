package tetzlaff.optimization.function;

import tetzlaff.optimization.MatrixSystem;

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
    private final int transitionRange;
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
     * @param metallicity The assumed "metallicity" of the function being optimized.
     * @param transitionRange The distance between the start and end of each smoothstep function in the library.
     *                        Each smoothstep function will end (with a value of 0.0) at a different location
     *                        (spaced evenly across the domain) and will start (with a value of 1.0) at
     *                        endpoint - transitionRange.  If that difference comes out to be negative, the starting
     *                        point will be clamped to zero.  The effect of this is that some basis functions with a
     *                        smaller transition range will be included in the library.  This is helpful when optimizing
     *                        a BRDF, for instance, which may have a steeper gradient close to the specular peak.
     *                        A transition range of 1 would correspond to a hard step function.
     * @param smoothstep The actual smoothstep function to be used.  This function should have accept a domain between
     *                   0.0 and 1.0 and map the values to a range that is also between 0.0 and 1.0, where an input of
     *                   0.0 evaluates to a result of 0.0, and an input of 1.0 evaluates to a result of 1.0.
     *                   It is intended for this function to be monotonically increasing.
     *                   Note that this function will effectively be flipped in practice since the basis functions are
     *                   assumed to evaluate to 1.0 for an input parameter of 0 and decresse down to 1.0 as that
     *                   parameter increases.
     */
    public GeneralizedSmoothStepBasis(int resolution, double metallicity,
                                      int transitionRange, DoubleUnaryOperator smoothstep)
    {
        this.resolution = resolution;
        this.metallicity = metallicity;
        this.transitionRange = transitionRange;
        this.smoothstep = smoothstep;
    }

    @Override
    public double evaluate(int functionIndex, int value)
    {
        // The function at index i always reaches 0.0 when value = i + 1.
        // If i + 1 < transitionRange, then the smoothstep starts right away at m=0.
        // Otherwise, the smoothstep starts at m = (i + 1) - transitionRange.
        if (value < functionIndex + 1) // <=> functionIndex + 1 - value > 0
        {
            // functionIndex = 0: range of 1; [0, 1)
            // functionIndex = 1: range of 2; [0, 2)
            // etc.
            // functionIndex = transitionRange: use transitionRange; [0, transitionRange)
            // functionIndex = transitionRange + 1: [1, transitionRange + 1)
            // etc.
            int effectiveTransitionRange = Math.min(transitionRange, functionIndex + 1);

            // Value is within transition range, use smoothstep function.
            if (functionIndex + 1 - value < effectiveTransitionRange)
            // <=> value > functionIndex + 1 - effectiveTransitionRange [left bound of the transitionRange]
            {
                return smoothstep.applyAsDouble((double) (functionIndex + 1 - value) / (double) effectiveTransitionRange);
            }
            else
            {
                // Value is beyond transition range to the left
                return 1.0;
            }
        }
        else
        {
            // Value is beyond transition range to the right
            return 0.0;
        }
    }

    @Override
    public int getFunctionCount()
    {
        return resolution;
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
        // For a particular valueCurrent, if i = valueCurrent-1 is the index of the last basis function that evaluates
        // to 0.0 at valueCurrent, then i = valueCurrent-1 + transitionRange is the index of the first basis function
        // that evaluates to 1.0 at valueCurrent.
        int transitionEnd = Math.min(valueCurrent - 1 + transitionRange, resolution);

        // Add the running total to elements of the ATA matrix and the ATy vector corresponding to the next value
        // as well as any values skipped over.
        // These elements also need to get some more contributions that have blending weights that are yet to be
        // visited, but that will be handled later, when a sample is visited for some matrix elements, or the next time
        // the value changes for others.
        for (int b1 = 0; b1 < instanceCount; b1++)
        {
            // Contribute to weights for library functions in the range [valueCurrent - 1, transitionEnd].
            // This range is inclusive on both ends due to linear interpolation.
            // i.e., the library function at i=transitionEnd will be interpolating from 1 to a value less than 1 for
            // parameter values between valueCurrent and valueCurrent+1.
            for (int k = valueCurrent; k <= transitionEnd; k++)
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

                    // Matrix coefficients where both row and column correspond to transition range:
                    // Need a nested loop to consider every combination of library functions in the range.
                    for (int k2 = valueCurrent; k2 <= transitionEnd; k2++)
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

                    // Matrix coefficients where row is in the transition range but column isn't, and vice-versa.
                    // Need to consider every library function that comes after transitionEnd.
                    for (int k2 = transitionEnd + 1; k2 < resolution; k2++) {
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

            // Contribute to weights for library functions from current transitionEnd+1 through next transitionEnd+1.
            // These will all evaluate to 1.0 for the current value of m so we just need the total of their weights.
            // Since the non-blend-weighted sums continue to accumulate, contributions to later library functions
            // will be deferred until the sums are complete for those functions.
            // This loop usually would only run once, but could run multiple times if we skipped a few values.
            int nextTransitionEnd = Math.min(valueNext - 1 + transitionRange, resolution);
            for (int m1 = transitionEnd + 1; m1 <= nextTransitionEnd; m1++)
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
                    for (int m2 = m1 + 1; m2 < resolution; m2++)
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

            // For a particular value of m, if m-1 is the last basis function that evaluates to 0.0,
            // then m-1 + transitionRange is the first basis function that evaluates to 1.0.
            int transitionEnd = Math.min(m - 1 + transitionRange, resolution);

            // Accounts for library functions from transitionEnd through the domain max.
            // These will all evaluate to 1.0 for the current value of m so we just need the total of their weights.
            double currentTotal = sums[transitionEnd];

            // Accounts for library functions in the range [m, transitionEnd).
            for (int i = transitionEnd - 1; i >= m; i--)
            {
                // Evaluate library function i for input parameter m and add to weighted total.
                currentTotal += nonConstantSolution.applyAsDouble(i) * evaluate(i, m);
            }

            // Library functions 0 through m-1 should not be able to affect the element at index m since they will evaluate to 0.
            // Total now accounts for the whole rang eof library functions.
            functionConsumer.accept(currentTotal, m);
        }
    }
}
