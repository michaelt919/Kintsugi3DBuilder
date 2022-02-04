package tetzlaff.optimization.function;

import tetzlaff.optimization.MatrixSystem;

import java.util.function.IntToDoubleFunction;
import java.util.function.ObjIntConsumer;

public class StepBasis implements BasisFunctions
{
    private final int resolution;
    private final double metallicity;

    public StepBasis(int resolution, double metallicity)
    {
        this.resolution = resolution;
        this.metallicity = metallicity;
    }

    @Override
    public double evaluate(int functionIndex, int value)
    {
        return value <= functionIndex ? 1.0 : 0.0;
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

    @Override
    public void contributeToFittingSystem(int valueCurrent, int valueNext, int instanceCount,
                                          MatrixBuilderSums sums, MatrixSystem fittingSystem)
    {
        // Add the running total to elements of the ATA matrix and the ATy vector corresponding to the next value
        // as well as any values skipped over.
        // These elements also need to get some more contributions that have blending weights that are yet to be
        // visited, but that will be handled later, when a sample is visited for some matrix elements, or the next time
        // the value changes for others.
        for (int b1 = 0; b1 < instanceCount; b1++)
        {
            int i = instanceCount * (valueCurrent + 1) + b1;

            // Update ATy vector for blended term.
            // Contribution due to current value scaled by blending weight t to account for linear interpolation.
            // Accumulation due to greater values should already have been added to the vector the last time a value changed
            fittingSystem.addToRHS(i, 0, sums.getWeightedAnalyticTimesObservedBlended(0, b1));
            fittingSystem.addToRHS(i, 1, sums.getWeightedAnalyticTimesObservedBlended(1, b1));
            fittingSystem.addToRHS(i, 2, sums.getWeightedAnalyticTimesObservedBlended(2, b1));

            // Add the total of recently visited samples with blending weights to elements of the ATA matrix corresponding
            // to the current value.
            for (int b2 = 0; b2 < instanceCount; b2++)
            {
                // Top right and bottom left partitions of the matrix:
                // row corresponds to diffuse coefficients and column corresponds to specular, or vice-versa.
                // The matrix is symmetric so we also need to swap row and column and update that way.
                fittingSystem.addToLHS(i, b2,
                        metallicity * sums.getWeightedAnalyticSquaredBlended(b1, b2)
                                + (1 - metallicity) * sums.getWeightedAnalyticBlended(b1, b2));
                fittingSystem.addToLHS(b2, i,
                        metallicity * sums.getWeightedAnalyticSquaredBlended(b1, b2)
                                + (1 - metallicity) * sums.getWeightedAnalyticBlended(b1, b2));

                // Bottom right partition of the matrix: row and column both correspond to specular:
                // Update "corner" element with squared blending weight.
                int j = instanceCount * (valueCurrent + 1) + b2;
                fittingSystem.addToLHS(i, j, sums.getWeightedAnalyticSquaredBlendedSquared(b1, b2));

                // Visit every element of the microfacet distribution that comes after the current value.
                // This is because the form of ATA is such that the values in the matrix are determined by the lower of the two values.
                for (int m2 = valueCurrent + 1; m2 < resolution; m2++)
                {
                    j = instanceCount * (m2 + 1) + b2;

                    // Add the current running total with blending (linear interpolation) weights
                    // to the appropriate location in the matrix.
                    // The matrix is symmetric so we also need to swap row and column and update that way.
                    fittingSystem.addToLHS(i, j, sums.getWeightedAnalyticSquaredBlended(b1, b2));
                    fittingSystem.addToLHS(j, i, sums.getWeightedAnalyticSquaredBlended(b2, b1));
                }
            }

            // Update the matrix for rows and columns corresponding to the next value and any values in between
            // the current value and the next value.
            // This loop usually would only run once, but could run multiple times if we skipped a few values.
            for (int m1 = valueCurrent + 1; m1 <= valueNext; m1++)
            {
                i = instanceCount * (m1 + 1) + b1;

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
        // Constant term
        double currentValue = constantTerm * metallicity;
        functionConsumer.accept(currentValue, resolution);

        // Remember the previous value (recursive definition).
        double prevValue = currentValue;

        // Loop from end down to 0.
        for (int m = resolution - 1; m >= 0; m--)
        {
            // Calculate the current value.
            currentValue = prevValue + nonConstantSolution.applyAsDouble(m);

            // f[m] = f[m+1] + optimized difference due to step (located at index m + 1 due to constant term at index 0).
            functionConsumer.accept(currentValue, m);

            // Remember the previous value (recursive definition).
            prevValue = currentValue;
        }
    }
}
