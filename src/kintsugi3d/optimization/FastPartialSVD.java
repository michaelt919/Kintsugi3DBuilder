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

package kintsugi3d.optimization;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import org.ejml.data.FMatrix;
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
        this.transpose = matrix.numCols() > matrix.numRows();

        this.matrix = matrix;

        this.singularValueCount = singularValueCount;
        this.tolerance = tolerance;
        this.maxIterations = maxIterations;
        this.maxAttempts = maxAttempts;

        this.u = new SimpleMatrix(this.transpose ? this.matrix.numCols() : this.matrix.numRows(), singularValueCount, FMatrixRMaj.class);
        this.v = new SimpleMatrix(this.transpose ? this.matrix.numRows() : this.matrix.numCols(), singularValueCount, FMatrixRMaj.class);
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
                SimpleMatrix vk = SimpleMatrix.random32(this.v.numRows(), 1, -1, 1, random);
                divide(vk.getMatrix(), (float)vk.normF());

                double ev;
                double sqError;
                int numIterations;
                int numAttempts = 0;

                SimpleMatrix vkLast = new SimpleMatrix(this.v.numRows(), 1, FMatrixRMaj.class);
                SimpleMatrix diff = new SimpleMatrix(this.v.numRows(), 1, FMatrixRMaj.class);
                SimpleMatrix intermediateProduct = new SimpleMatrix(u.numRows(), 1, FMatrix.class);

                do
                {
                    numIterations = 0;

                    do
                    {
                        SimpleMatrix tmp = vkLast;
                        vkLast = vk;
                        vk = tmp;

                        if (transpose)
                        {
                            multTransA(this.matrix.getMatrix(), vkLast.getMatrix(), intermediateProduct.getMatrix());
                            mult(this.matrix.getMatrix(), intermediateProduct.getMatrix(), vk.getMatrix());
                        }
                        else
                        {
                            mult(this.matrix.getMatrix(), vkLast.getMatrix(), intermediateProduct.getMatrix());
                            multTransA(this.matrix.getMatrix(), intermediateProduct.getMatrix(), vk.getMatrix());
                        }

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

                    SimpleMatrix uk = new SimpleMatrix(this.u.numRows(), 1, FMatrixRMaj.class);

                    if (transpose)
                    {
                        multTransA(matrix.getMatrix(), vk.getMatrix(), uk.getMatrix());
                    }
                    else
                    {
                        mult(matrix.getMatrix(), vk.getMatrix(), uk.getMatrix());
                    }

                    divide(uk.getMatrix(), (float)uk.normF()); // Procedural framework: in place divide for efficiency

                    for (int i = 0; i < u.numRows(); i++)
                    {
                        u.set(i, k, uk.get(i));
                    }

                    for (int i = 0; i < v.numRows(); i++)
                    {
                        v.set(i, k, vk.get(i));
                    }

                    FMatrixRMaj matrixTransposeTimesUk = new FMatrixRMaj(this.v.numRows(), 1);

                    if (transpose)
                    {
                        mult(matrix.getMatrix(), uk.getMatrix(), matrixTransposeTimesUk);
                    }
                    else
                    {
                        multTransA(matrix.getMatrix(), uk.getMatrix(), matrixTransposeTimesUk);
                    }

//                    // Update matrix A = M'M (using procedural framework for efficiency)
//                    multAddTransB(-sv, matrixTransposeTimesUk, vk.getMatrix(), a.getMatrix());
//                    multAddTransB(-sv, vk.getMatrix(), matrixTransposeTimesUk, a.getMatrix());
//                    multAddTransB(ev, vk.getMatrix(), vk.getMatrix(), a.getMatrix());

                    // Update original matrix M (using procedural framework for efficiency)
                    if (transpose)
                    {
                        multAddTransB(-sv, vk.getMatrix(), uk.getMatrix(), matrix.getMatrix());
                    }
                    else
                    {
                        multAddTransB(-sv, uk.getMatrix(), vk.getMatrix(), matrix.getMatrix());
                    }
                }
            }
        }
    }
}
