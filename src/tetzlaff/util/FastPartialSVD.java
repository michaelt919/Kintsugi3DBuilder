package tetzlaff.util;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import org.ejml.simple.SimpleMatrix;

import static org.ejml.dense.row.CommonOps_DDRM.*;

/**
 * Fast SVD using power iterations for when only a few singular values are needed.
 * Fairly standard algorithm; implemented using the appendix of Chen et al., "Light Field Mapping" for reference.
 */
public final class FastPartialSVD
{
    private SimpleMatrix matrix;
    private int singularValueCount;
    private final double tolerance;
    private final int maxIterations;

    private final boolean transpose;
    private final SimpleMatrix u;
    private final SimpleMatrix v;
    private final double[] singularValues;


    public static FastPartialSVD compute(SimpleMatrix matrix, int singularValueCount)
    {
        return compute(matrix, singularValueCount, Math.ulp(1.0), 1000);
    }

    public static FastPartialSVD compute(SimpleMatrix matrix, int singularValueCount, double tolerance, int maxIterations)
    {
        FastPartialSVD svd = new FastPartialSVD(matrix, singularValueCount, tolerance, maxIterations);
        svd.compute();
        return svd;
    }

    public SimpleMatrix getU()
    {
        return transpose ? v : u;
    }

    public SimpleMatrix getV()
    {
        return transpose ? u : v;
    }

    public double[] getSingularValues()
    {
        return Arrays.copyOf(singularValues, singularValueCount);
    }

    private FastPartialSVD(SimpleMatrix matrix, int singularValueCount, double tolerance, int maxIterations)
    {
        if (matrix.numCols() > matrix.numRows())
        {
            this.transpose = true;
            this.matrix = matrix.transpose();
        }
        else
        {
            this.transpose = false;
            this.matrix = matrix.copy();
        }

        this.singularValueCount = singularValueCount;
        this.tolerance = tolerance;
        this.maxIterations = maxIterations;

        this.u = new SimpleMatrix(this.matrix.numRows(), singularValueCount);
        this.v = new SimpleMatrix(this.matrix.numCols(), singularValueCount);
        this.singularValues = new double[singularValueCount];
    }

    private void compute()
    {
        if (this.matrix != null)
        {
            Random random = new SecureRandom();
            for (int k = 0; k < singularValueCount; k++)
            {
                SimpleMatrix a = new SimpleMatrix(this.matrix.numCols(), this.matrix.numCols());

                // Use procedural framework to save memory for this step.
                multInner(this.matrix.getMatrix(), a.getMatrix());

                SimpleMatrix vk = SimpleMatrix.random64(this.matrix.numCols(), 1, -1, 1, random);
                vk = vk.divide(vk.normF());

                SimpleMatrix vkLast;
                double lambda;
                int numIterations = 0;

                do
                {
                    vkLast = vk;
                    vk = a.mult(vkLast);
                    lambda = vk.normF();
                    divide(vk.getMatrix(), lambda); // Procedural framework: in place divide for efficiency
                    numIterations++;
                }
                while (lambda > 0.0 && dot(vkLast.getMatrix(), vk.getMatrix()) < 1.0 - tolerance && numIterations < maxIterations);

                if (numIterations == maxIterations)
                {
                    throw new RuntimeException("Max iterations exceeded. (Convergence: " + dot(vkLast.getMatrix(), vk.getMatrix()) + ')');
                }
                else if (lambda == 0.0)
                {
                    u.reshape(u.numRows(), k);
                    v.reshape(v.numRows(), k);
                    singularValueCount = k;
                }
                else
                {
                    singularValues[k] = lambda;
                    u.setColumn(k, 0, matrix.mult(vk.divide(lambda)).matrix_F64().data);
                    v.setColumn(k, 0, vk.matrix_F64().data);

                    for (int i = 0; i < this.matrix.numRows(); i++)
                    {
                        for (int j = 0; j < this.matrix.numCols(); j++)
                        {
                            this.matrix.set(i, j, this.matrix.get(i, j) - this.u.get(i, k) * this.v.get(j, k));
                        }
                    }
                }
            }
        }

        this.matrix = null;
    }
}
