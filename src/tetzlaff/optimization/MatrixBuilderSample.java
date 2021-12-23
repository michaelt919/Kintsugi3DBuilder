package tetzlaff.optimization;

import java.util.function.IntToDoubleFunction;

/**
 * A sample for optimization problems where the goal is to find a set of optimal basis functions
 * which are themselves in turn a linear combination of an underlying set of "library" basis functions.
 * The optimization problem itself is to find a least-squares solution to the equation:
 * SUM(over b from 1 to B) (w_b * f_b(p) * g(p)) = y(p)
 * f_b is the function that is being optimized.
 * w_b are weights, which are assumed to be fixed for this step.
 * g is an analytic function, which is assumed to be fixed for this step.
 * y is the observed value to be fitted to.
 */
public class MatrixBuilderSample
{
    /**
     * The analytic factor evaluated for the current sample.
     */
    public final double analytic;

    /**
     * A weight for the sample (in the sense of weighted least squares).
     */
    public final double sampleWeight;

    /**
     * Since the basis functions are represented in a tabular form, linear interpolation between elements is necessary.
     * This weight indicates how close this sample is to the closest preceding value in the table.
     * (0 = at the closest preceding value; 1 = at/approaching the next value after the closest preceding value)
     */
    public final double blendingWeight;

    /**
     * The weight of each basis function to be optimized for the current sample.
     */
    public final IntToDoubleFunction weightByInstance;

    /**
     * The observations at the current sample (i.e. an RGB color).
     */
    public final double[] observed;

    public MatrixBuilderSample(double analytic, double sampleWeight, double blendingWeight, IntToDoubleFunction weightByInstance, double... observed)
    {
        this.analytic = analytic;
        this.sampleWeight = sampleWeight;
        this.blendingWeight = blendingWeight;
        this.weightByInstance = weightByInstance;
        this.observed = observed;
    }
}
