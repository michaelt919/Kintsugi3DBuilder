package kintsugi3d.optimization.function;

import kintsugi3d.optimization.MatrixSystem;

import java.util.function.IntToDoubleFunction;
import java.util.function.ObjIntConsumer;

/**
 * Helper abstract class for setting up basis functions.
 * Implementations of contributeToFittingSystem() and evaluateSolution() are provided;
 * subclasses must provide evaluate(), getFirstFunctionIndexForDomainValue(), and getLastFunctionIndexForDomainValue()
 * An important assumption is that all functions are generally increasing from 0.0 to 1.0 across the explicit domain;
 * that is, a given function is 0.0 up to some inflection point,
 * then enters the explicitly defined domain up to another inflection point,
 * and finally is 1.0 beyond that second inflection point.
 * evalute() must be implemented correctly to meet these criteria or the implementations of contributeToFittingSystem()
 * and evaluateSolution() will not work correctly.
 */
public abstract class AbstractBasisFunctions implements BasisFunctions
{
    private final double metallicity;

    protected AbstractBasisFunctions(double metallicity)
    {
        this.metallicity = Math.max(0.0, Math.min(1.0, metallicity));
    }

    /**
     * Gets the index of the first function for which the specified value is in its explicit domain.
     * It is assumed that all functions with lower indices will implicitly map to 0.0 for the specified value.
     * @param value The value to consider for domain inclusion.
     * @return The index of the first function for which the specified value is in its explicit domain.
     */
    protected abstract int getFirstFunctionIndexForDomainValue(int value);

    /**
     * Gets the index of the last function for which the specified value is in its explicit domain.
     * It is assumed that all functions with higher indices will implicitly map to 1.0 for the specified value.
     * @param value The value to consider for domain inclusion.
     * @return The index of the last function for which the specified value is in its explicit domain.
     */
    protected abstract int getLastFunctionIndexForDomainValue(int value);

    private static double lerpHelper(double fLower, double fUpper, double sumBlendWeighted, double sumUnBlendWeighted)
    {
        return fLower * sumBlendWeighted + fUpper * (sumUnBlendWeighted - sumBlendWeighted);
    }

    @Override
    public double getMetallicity()
    {
        return this.metallicity;
    }

    @Override
    public void contributeToFittingSystem(int valueCurrent, int valueNext, int instanceCount,
                                          MatrixBuilderSums sums, MatrixSystem fittingSystem)
    {
        int kFirst = getFirstFunctionIndexForDomainValue(valueCurrent);
        int kLast = getLastFunctionIndexForDomainValue(valueCurrent);

        // Add the running total to elements of the ATA matrix and the ATy vector corresponding to the next value
        // as well as any values skipped over.
        // These elements also need to get some more contributions that have blending weights that are yet to be
        // visited, but that will be handled later, when a sample is visited for some matrix elements, or the next time
        // the value changes for others.
        for (int b1 = 0; b1 < instanceCount; b1++)
        {
            // Contribute to weights for library functions in the range [kFirst, kLast].
            // This range is inclusive on both ends due to linear interpolation.
            // i.e., the library function at i=kLast will be interpolating from 1 to a value less than 1 for
            // parameter values between valueCurrent and valueCurrent+1.
            for (int k = kFirst; k <= kLast && k < getFunctionCount(); k++)
            {
                int i = instanceCount * (k + 1) + b1;

                // fLower should always be greater than fUpper. (lower/upper refers to input parameter, not evaluation result)
                double fLower = evaluate(k, valueCurrent);
                double fUpper = evaluate(k, valueCurrent + 1);

                // Update ATy vector for blended terms.
                fittingSystem.addToRHS(i, 0, AbstractBasisFunctions.lerpHelper(fLower, fUpper,
                        sums.getWeightedAnalyticTimesObservedBlended(0, b1),
                        sums.getWeightedAnalyticTimesObserved(0, b1)));
                fittingSystem.addToRHS(i, 1, AbstractBasisFunctions.lerpHelper(fLower, fUpper,
                        sums.getWeightedAnalyticTimesObservedBlended(1, b1),
                        sums.getWeightedAnalyticTimesObserved(1, b1)));
                fittingSystem.addToRHS(i, 2, AbstractBasisFunctions.lerpHelper(fLower, fUpper,
                        sums.getWeightedAnalyticTimesObservedBlended(2, b1),
                        sums.getWeightedAnalyticTimesObserved(2, b1)));

                // Update ATA matrix for blended terms.
                for (int b2 = 0; b2 < instanceCount; b2++)
                {
                    // Top right and bottom left partitions of the matrix:
                    // row corresponds to constant coefficients and column corresponds to non-constant, or vice-versa.
                    double constNonConstCrossCoeff = AbstractBasisFunctions.lerpHelper(fLower, fUpper,
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
                    for (int k2 = kFirst; k2 <= kLast && k2 < getFunctionCount(); k2++)
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
                    // Need to consider every library function that comes after kLast.
                    for (int k2 = kLast + 1; k2 < getFunctionCount(); k2++)
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

            // Contribute to weights for library functions from current kLast+1 through next kLast+1.
            // These will all evaluate to 1.0 for the current value of m so we just need the total of their weights.
            // Since the non-blend-weighted sums continue to accumulate, contributions to later library functions
            // will be deferred until the sums are complete for those functions.
            // This loop usually would only run once, but could run multiple times if we skipped a few values.
            int nextKLast = getLastFunctionIndexForDomainValue(valueNext);
            for (int m1 = kLast + 1; m1 <= nextKLast && m1 < getFunctionCount(); m1++) // TODO work out why m1 < functionCount is necessary
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
        int functionCount = getFunctionCount();

        // Keep an array that stores the sum after adding the weight for each additional library function.
        // This is used to efficiently incorporate basis functions that are already at their max value for a particular element.
        // sums[i] = sum of library function weights in the range [i, functionCount) + constant term.
        double[] sums = new double[functionCount + 1];

        // Constant term
        sums[functionCount] = constantTerm * metallicity;
        functionConsumer.accept(sums[functionCount], functionCount);

        // Loop over functions from end down to 0.
        for (int k = functionCount - 1; k >= 0; k--)
        {
            // Update the running total for future elements where the current basis function will be maxed out.
            sums[k] = sums[k + 1] + nonConstantSolution.applyAsDouble(k);
        }

        for (int value = getOptimizedDomainSize() - 1; value >= 0; value--)
        {
            int kFirst = getFirstFunctionIndexForDomainValue(value);
            int kLast = getLastFunctionIndexForDomainValue(value);

            // Accounts for library functions from kLast through the domain max.
            // These will all evaluate to 1.0 for the current value so we just need the total of their weights.
            double currentTotal = sums[kLast];

            // Accounts for library functions in the range [kFirst + 1, kLast).
            for (int k = kLast - 1; k >= kFirst + 1; k--)
            {
                // Evaluate library function k for current value and add to weighted total.
                currentTotal += nonConstantSolution.applyAsDouble(k) * evaluate(k, value);
            }

            // Library functions 0 through value-1 should not be able to affect the element at index value since they will evaluate to 0.
            // Total now accounts for the whole range of library functions.
            functionConsumer.accept(currentTotal, value);
        }
    }
}
