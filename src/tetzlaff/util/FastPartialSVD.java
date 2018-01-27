package tetzlaff.util;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import org.ejml.data.FMatrixRMaj;
import org.ejml.simple.SimpleMatrix;

import static org.ejml.dense.row.CommonOps_FDRM.*;

/**
 * Fast SVD using power iterations for when only a few singular values are needed.
 * Fairly standard algorithm; implemented using the appendix of Chen et al., "Light Field Mapping" for reference.
 */
public final class FastPartialSVD
{
    private final SimpleMatrix matrix;
    private int singularValueCount;
    private final float tolerance;
    private final int maxIterations;
    private final int maxAttempts;

    private final boolean transpose;
    private final SimpleMatrix u;
    private final SimpleMatrix v;
    private final float[] singularValues;

    public static FastPartialSVD compute(SimpleMatrix matrix, int singularValueCount)
    {
        return compute(matrix, singularValueCount, Math.ulp(1.0f), 1000, 3);
    }

    public static FastPartialSVD compute(SimpleMatrix matrix, int singularValueCount, float tolerance, int maxIterations, int maxAttempts)
    {
        FastPartialSVD svd = new FastPartialSVD(matrix, singularValueCount, tolerance, maxIterations, maxAttempts);
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

    public SimpleMatrix getError()
    {
        return this.matrix;
    }

    public float[] getSingularValues()
    {
        return Arrays.copyOf(singularValues, singularValueCount);
    }

    private FastPartialSVD(SimpleMatrix matrix, int singularValueCount, float tolerance, int maxIterations, int maxAttempts)
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
        this.maxAttempts = maxAttempts;

        this.u = new SimpleMatrix(this.matrix.numRows(), singularValueCount, FMatrixRMaj.class);
        this.v = new SimpleMatrix(this.matrix.numCols(), singularValueCount, FMatrixRMaj.class);
        this.singularValues = new float[singularValueCount];
    }

    private void compute()
    {
        if (this.matrix != null)
        {
            Random random = new SecureRandom();
            //SimpleMatrix a = new SimpleMatrix(this.matrix.numCols(), this.matrix.numCols(), FMatrixRMaj.class);
            double toleranceSq = tolerance * tolerance;

            // Use procedural framework to save memory for this step.
//            multInner(this.matrix.getMatrix(), a.getMatrix());

            for (int k = 0; k < singularValueCount; k++)
            {
                SimpleMatrix vk = SimpleMatrix.random32(this.matrix.numCols(), 1, -1, 1, random);
                divide(vk.getMatrix(), (float)vk.normF());

                double ev;
                double sqError;
                int numIterations;
                int numAttempts = 0;

                SimpleMatrix vkLast = new SimpleMatrix(this.matrix.numCols(), 1, FMatrixRMaj.class);
                SimpleMatrix diff = new SimpleMatrix(this.matrix.numCols(), 1, FMatrixRMaj.class);

                do
                {
                    numIterations = 0;

                    do
                    {
                        SimpleMatrix tmp = vkLast;
                        vkLast = vk;
                        vk = tmp;

                        multTransA(this.matrix.getMatrix(), this.matrix.mult(vkLast).getMatrix(), vk.getMatrix());

                        //vk = a.mult(vkLast);

                        ev = vk.normF();
                        divide(vk.getMatrix(), (float)ev); // Procedural framework: in place divide for efficiency
                        subtract(vk.getMatrix(), vkLast.getMatrix(), diff.getMatrix());
                        numIterations++;

                        sqError = dot(diff.getMatrix(), diff.getMatrix());
                    }
                    while (ev > 0.0 && sqError > toleranceSq && numIterations < maxIterations);

                    numAttempts++;
                }
                while(numIterations == maxIterations && numAttempts < maxAttempts);

                if (ev == 0.0)
                {
                    u.reshape(u.numRows(), k);
                    v.reshape(v.numRows(), k);
                    singularValueCount = k;
                }
                else if (sqError > toleranceSq)
                {
                    throw new RuntimeException("Max iterations exceeded. (Squared error: " + sqError + ')');
                }
                else
                {
                    float sv = (float)Math.sqrt(ev);
                    singularValues[k] = sv;
                    SimpleMatrix uk = matrix.mult(vk);
                    divide(uk.getMatrix(), (float)uk.normF()); // Procedural framework: in place divide for efficiency

                    for (int i = 0; i < u.numRows(); i++)
                    {
                        u.set(i, k, uk.get(i));
                    }

                    for (int i = 0; i < v.numRows(); i++)
                    {
                        v.set(i, k, vk.get(i));
                    }

                    FMatrixRMaj matrixTransposeTimesUk = new FMatrixRMaj(matrix.numCols(), 1);
                    multTransA(matrix.getMatrix(), uk.getMatrix(), matrixTransposeTimesUk);

//                    // Update matrix A = M'M (using procedural framework for efficiency)
//                    multAddTransB(-sv, matrixTransposeTimesUk, vk.getMatrix(), a.getMatrix());
//                    multAddTransB(-sv, vk.getMatrix(), matrixTransposeTimesUk, a.getMatrix());
//                    multAddTransB(ev, vk.getMatrix(), vk.getMatrix(), a.getMatrix());

                    // Update original matrix M (using procedural framework for efficiency)
                    multAddTransB(-sv, uk.getMatrix(), vk.getMatrix(), matrix.getMatrix());
                }
            }
        }
    }
}
