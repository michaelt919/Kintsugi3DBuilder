package tetzlaff.optimization.function;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;

import java.util.Arrays;

/**
 * Stores running totals to assist with optimization problems where the goal is to find a set of optimal basis functions
 * which are themselves in turn a linear combination of an underlying set of "library" basis functions.
 * The optimization problem itself is to find a least-squares solution to the equation:
 * SUM(over b from 1 to B) (w_b * f_b(p) * g(p)) = y(p)
 * f_b is the function that is being optimized.
 * w_b are weights, which are assumed to be fixed for this step.
 * g is an analytic function, which is assumed to be fixed for this step.
 * y is the observed value to be fitted to.
 */
public class MatrixBuilderSums
{
    /**
     * Stores a running total (for each pair of basis functions) of the weighted sum of analytic functions.
     * This total will NOT be cleared when clearNonCumulativeSums() is called.
     */
    private final SimpleMatrix weightedAnalyticCumulative;
    /**
     * Stores a running total (for each pair of basis functions) of the weighted sum of analytic functions.
     * This total WILL be cleared when clearNonCumulativeSums() is called.
     */
    private final SimpleMatrix weightedAnalytic;

    /**
     * Stores a running total (for each pair of basis functions) of the weighted sum of analytic functions.
     * This total WILL be cleared when clearNonCumulativeSums() is called.
     */
    private final SimpleMatrix weightedAnalyticBlended;

    /**
     * Stores a running total (for each pair of basis functions) of the weighted sum of squared analytic functions.
     * This total will NOT be cleared when clearNonCumulativeSums() is called.
     */
    private final SimpleMatrix weightedAnalyticSquaredCumulative;

    /**
     * Stores a running total (for each pair of basis functions) of the weighted sum of squared analytic functions.
     * This total WILL be cleared when clearNonCumulativeSums() is called.
     */
    private final SimpleMatrix weightedAnalyticSquared;

    /**
     *  Stores a running total (for each pair of basis functions) of the weighted sum of squared analytic functions
     *  with additional linear interpolation weights.
     * This total WILL be cleared when clearNonCumulativeSums() is called.
     */
    private final SimpleMatrix weightedAnalyticSquaredBlended;
    /**
     *  Stores a running total (for each pair of basis functions) of the weighted sum of squared analytic functions
     *  with additional linear interpolation weights which are squared (i.e. multiplication by t^2).
     * This total WILL be cleared when clearNonCumulativeSums() is called.
     */
    private final SimpleMatrix weightedAnalyticSquaredBlendedSquared;

    /**
     * Stores a running total (for each basis function) of the weighted sum of observed values times the analytic function.
     * This total will NOT be cleared when clearNonCumulativeSums() is called.
     */
    private final SimpleMatrix[] weightedAnalyticTimesObservedCumulative;

    /**
     * Stores a running total (for each basis function) of the weighted sum of observed values times the analytic function.
     * This total WILL be cleared when clearNonCumulativeSums() is called.
     */
    private final SimpleMatrix[] weightedAnalyticTimesObserved;

    /**
     * Stores a running total (for each basis function) of the weighted sum of observed values times the analytic function
     * with additional linear interpolation weights.
     * This total WILL be cleared when clearNonCumulativeSums() is called.
     */
    private final SimpleMatrix[] weightedAnalyticTimesObservedBlended;

    /**
     * Initialize running totals.
     * @param instanceCount The number of function "instances" being optimized, i.e. the number of "optimized" basis
     *                      functions derived from the set of "library" basis functions.
     * @param observationCount The number of observations for each sample (i.e. 3 for RGB colors)
     */
    public MatrixBuilderSums(int instanceCount, int observationCount)
    {
        weightedAnalyticCumulative = new SimpleMatrix(instanceCount, instanceCount, DMatrixRMaj.class);
        weightedAnalytic = new SimpleMatrix(instanceCount, instanceCount, DMatrixRMaj.class);
        weightedAnalyticBlended = new SimpleMatrix(instanceCount, instanceCount, DMatrixRMaj.class);
        weightedAnalyticSquaredCumulative = new SimpleMatrix(instanceCount, instanceCount, DMatrixRMaj.class);
        weightedAnalyticSquared = new SimpleMatrix(instanceCount, instanceCount, DMatrixRMaj.class);
        weightedAnalyticSquaredBlended = new SimpleMatrix(instanceCount, instanceCount, DMatrixRMaj.class);
        weightedAnalyticSquaredBlendedSquared = new SimpleMatrix(instanceCount, instanceCount, DMatrixRMaj.class);

        weightedAnalyticTimesObservedCumulative = new SimpleMatrix[observationCount];
        Arrays.setAll(weightedAnalyticTimesObservedCumulative, i -> new SimpleMatrix(instanceCount, 1, DMatrixRMaj.class));

        weightedAnalyticTimesObserved = new SimpleMatrix[observationCount];
        Arrays.setAll(weightedAnalyticTimesObserved, i -> new SimpleMatrix(instanceCount, 1, DMatrixRMaj.class));

        weightedAnalyticTimesObservedBlended = new SimpleMatrix[observationCount];
        Arrays.setAll(weightedAnalyticTimesObservedBlended, i -> new SimpleMatrix(instanceCount, 1, DMatrixRMaj.class));
    }

    public void accept(MatrixBuilderSample sample)
    {
        int instanceCount = weightedAnalyticCumulative.numRows();

        for (int b1 = 0; b1 < instanceCount; b1++)
        {
            double singleWeightedAnalyticSample = sample.analytic * sample.weightByInstance.applyAsDouble(b1) * sample.sampleWeight;

            for (int i = 0; i < weightedAnalyticTimesObservedCumulative.length; i++)
            {
                double weightedAnalyticTimesObservedSample = singleWeightedAnalyticSample * sample.observed[i];
                addWeightedAnalyticTimesObserved(i, b1, weightedAnalyticTimesObservedSample);
                addWeightedAnalyticTimesObservedBlended(i, b1, sample.blendingWeight * weightedAnalyticTimesObservedSample);
            }

            for (int b2 = 0; b2 < instanceCount; b2++)
            {
                // Update non-squared totals without blending weight.
                double weightedAnalyticSample = singleWeightedAnalyticSample * sample.weightByInstance.applyAsDouble(b2);
                addWeightedAnalytic(b1, b2, weightedAnalyticSample);

                // Update non-squared total with blending weight.
                addWeightedAnalyticBlended(b1, b2, sample.blendingWeight * weightedAnalyticSample);

                // Update squared totals without blending weight.
                double weightedAnalyticSquared = weightedAnalyticSample * sample.analytic;
                addWeightedAnalyticSquared(b1, b2, weightedAnalyticSquared);

                // Update squared total with blending weight.
                double weightedAnalyticSquaredBlendedSample = sample.blendingWeight * weightedAnalyticSquared;
                addWeightedAnalyticSquaredBlended(b1, b2, weightedAnalyticSquaredBlendedSample);

                // Update squared total with blending weight squared.
                addWeightedAnalyticSquaredBlendedSquared(b1, b2, sample.blendingWeight * weightedAnalyticSquaredBlendedSample);
            }
        }
    }

    /**
     * Clears all the non-cumulative sums.
     * This should be done whenever the interpolation endpoints change and all the non-cumulative sums are used to
     * update the fitting matrix being built, whereas the cumulative sums continue to accumulate after that occurs.
     */
    public void clearNonCumulativeSums()
    {
        weightedAnalytic.zero();
        weightedAnalyticBlended.zero();
        weightedAnalyticSquared.zero();
        weightedAnalyticSquaredBlended.zero();
        weightedAnalyticSquaredBlendedSquared.zero();
        for (SimpleMatrix vector : weightedAnalyticTimesObserved)
        {
            vector.zero();
        }
        for (SimpleMatrix vector : weightedAnalyticTimesObservedBlended)
        {
            vector.zero();
        }
    }

    public double getWeightedAnalyticCumulative(int row, int column)
    {
        return weightedAnalyticCumulative.get(row, column);
    }

    public double getWeightedAnalytic(int row, int column)
    {
        return weightedAnalytic.get(row, column);
    }

    public double getWeightedAnalyticBlended(int row, int column)
    {
        return weightedAnalyticBlended.get(row, column);
    }

    public double getWeightedAnalyticSquaredCumulative(int row, int column)
    {
        return weightedAnalyticSquaredCumulative.get(row, column);
    }

    public double getWeightedAnalyticSquared(int row, int column)
    {
        return weightedAnalyticSquared.get(row, column);
    }

    public double getWeightedAnalyticSquaredBlended(int row, int column)
    {
        return weightedAnalyticSquaredBlended.get(row, column);
    }

    public double getWeightedAnalyticSquaredBlendedSquared(int row, int column)
    {
        return weightedAnalyticSquaredBlendedSquared.get(row, column);
    }

    public double getWeightedAnalyticTimesObservedCumulative(int observationIndex, int instanceIndex)
    {
        return weightedAnalyticTimesObservedCumulative[observationIndex].get(instanceIndex, 0);
    }

    public double getWeightedAnalyticTimesObserved(int observationIndex, int instanceIndex)
    {
        return weightedAnalyticTimesObserved[observationIndex].get(instanceIndex, 0);
    }

    public double getWeightedAnalyticTimesObservedBlended(int observationIndex, int instanceIndex)
    {
        return weightedAnalyticTimesObservedBlended[observationIndex].get(instanceIndex, 0);
    }

    /**
     * Updates both cumulative and non-cumulative totals.
     * @param row
     * @param column
     * @param amount
     */
    private void addWeightedAnalytic(int row, int column, double amount)
    {
        weightedAnalytic.set(row, column, weightedAnalytic.get(row, column) + amount);
        weightedAnalyticCumulative.set(row, column, weightedAnalyticCumulative.get(row, column) + amount);
    }

    private void addWeightedAnalyticBlended(int row, int column, double amount)
    {
        weightedAnalyticBlended.set(row, column, weightedAnalyticBlended.get(row, column) + amount);
    }

    /**
     * Updates both cumulative and non-cumulative totals.
     * @param row
     * @param column
     * @param amount
     */
    private void addWeightedAnalyticSquared(int row, int column, double amount)
    {
        weightedAnalyticSquared.set(row, column, weightedAnalyticSquared.get(row, column) + amount);
        weightedAnalyticSquaredCumulative.set(row, column, weightedAnalyticSquaredCumulative.get(row, column) + amount);
    }

    private void addWeightedAnalyticSquaredBlended(int row, int column, double amount)
    {
        weightedAnalyticSquaredBlended.set(row, column, weightedAnalyticSquaredBlended.get(row, column) + amount);
    }

    private void addWeightedAnalyticSquaredBlendedSquared(int row, int column, double amount)
    {
        weightedAnalyticSquaredBlendedSquared.set(row, column, weightedAnalyticSquaredBlendedSquared.get(row, column) + amount);
    }

    /**
     * Updates both cumulative and non-cumulative totals.
     * @param observationIndex
     * @param instanceIndex
     * @param amount
     */
    public void addWeightedAnalyticTimesObserved(int observationIndex, int instanceIndex, double amount)
    {
        weightedAnalyticTimesObserved[observationIndex].set(instanceIndex, 0,
            weightedAnalyticTimesObserved[observationIndex].get(instanceIndex, 0) + amount);
        weightedAnalyticTimesObservedCumulative[observationIndex].set(instanceIndex, 0,
            weightedAnalyticTimesObservedCumulative[observationIndex].get(instanceIndex, 0) + amount);
    }

    public void addWeightedAnalyticTimesObservedBlended(int observationIndex, int instanceIndex, double amount)
    {
        weightedAnalyticTimesObservedBlended[observationIndex].set(instanceIndex, 0,
            weightedAnalyticTimesObservedBlended[observationIndex].get(instanceIndex, 0) + amount);
    }
}
