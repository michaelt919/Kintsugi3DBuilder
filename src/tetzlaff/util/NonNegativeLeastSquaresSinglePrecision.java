/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.util;

import java.util.ArrayList;
import java.util.List;

import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_FDRM;
import org.ejml.simple.SimpleMatrix;

import static org.ejml.dense.row.CommonOps_FDRM.elementMin;
import static org.ejml.dense.row.CommonOps_FDRM.multTransA;

public class NonNegativeLeastSquaresSinglePrecision
{
    private static SimpleMatrix solvePartial(SimpleMatrix mATA, SimpleMatrix vATb, boolean[] p, int sizeP, List<Integer> mapping, SimpleMatrix sOut)
    {
        for (int index = 0; index < p.length; index++)
        {
            if (p[index])
            {
                mapping.add(index);
            }
        }

        // Create versions of A'A and A'b containing only the rows and columns
        // corresponding to the free variables.
        SimpleMatrix mATA_P = new SimpleMatrix(mapping.size(), mapping.size(), FMatrixRMaj.class);
        SimpleMatrix vATb_P = new SimpleMatrix(mapping.size(), 1, FMatrixRMaj.class);

        for (int i = 0; i < mATA_P.numRows(); i++)
        {
            if (p[mapping.get(i)])
            {
                vATb_P.set(i, vATb.get(mapping.get(i)));

                for (int j = 0; j < mATA_P.numCols(); j++)
                {
                    if (p[mapping.get(j)])
                    {
                        mATA_P.set(i, j, mATA.get(mapping.get(i), mapping.get(j)));
                    }
                }
            }
        }

        // Solve the system.
        SimpleMatrix s_P = mATA_P.solve(vATb_P);

        // Copy the solution for the free variables into a vector containing the full solution,
        // including the variables fixed at zero.
        for (int i = 0; i < s_P.numRows(); i++)
        {
            sOut.set(mapping.get(i), s_P.get(i));
        }

        return s_P;
    }

    public SimpleMatrix solve(SimpleMatrix mA, SimpleMatrix b, double epsilon)
    {
        if (b.numCols() != 1 || b.numRows() != mA.numRows())
        {
            throw new IllegalArgumentException("b must be a column vector with the same number of rows as matrix A.");
        }

        // Precompute matrix products
        SimpleMatrix mATA = new SimpleMatrix(mA.numCols(), mA.numCols(), FMatrixRMaj.class);
        SimpleMatrix vATb = new SimpleMatrix(mA.numCols(), 1, FMatrixRMaj.class);

        // Low level operations to avoid using unnecessary memory.
        multTransA(mA.getMatrix(), mA.getMatrix(), mATA.getMatrix());
        multTransA(mA.getMatrix(), b.getMatrix(), vATb.getMatrix());

        return solvePremultiplied(mATA, vATb, epsilon);
    }

    public SimpleMatrix solvePremultiplied(SimpleMatrix mATA, SimpleMatrix vATb, double epsilon)
    {
        if (mATA.numCols() != mATA.numRows())
        {
            throw new IllegalArgumentException("A'A must be a square matrix.");
        }

        if (vATb.numCols() != 1 || vATb.numRows() != mATA.numRows())
        {
            throw new IllegalArgumentException("A'b must be a column vector with the same number of rows as matrix A'A.");
        }

        if (epsilon <= 0.0)
        {
            throw new IllegalArgumentException("Epsilon must be greater than zero.");
        }

        // Keep track of the set of free variables (where p[i] is true)
        // All other variables are fixed at zero.
        boolean[] p = new boolean[mATA.numCols()];

        // Keep track of the number of free variables.
        int sizeP = 0;

        SimpleMatrix x = new SimpleMatrix(mATA.numCols(), 1, FMatrixRMaj.class);
        SimpleMatrix w = vATb.copy();

        int k = -1;
        double maxW;

        do
        {
            maxW = 0.0;

            for (int i = 0; i < w.numRows(); i++)
            {
                double value = w.get(i);
                if (!p[i] && value > maxW)
                {
                    k = i;
                    maxW = value;
                }
            }

            // Iterate until effectively no values of w are positive.
            if (maxW > epsilon)
            {
                p[k] = true;

                SimpleMatrix s = new SimpleMatrix(mATA.numCols(), 1, FMatrixRMaj.class);

                // Mapping from the set of free variables to the set of all variables.
                List<Integer> mapping = new ArrayList<>(sizeP + 1);

                // Populates the mapping, sovles the system and copies it into s,
                // and returns a vector containing only the free variables.
                SimpleMatrix s_P = solvePartial(mATA, vATb, p, sizeP + 1, mapping, s);

                // Update size of P based on the number of mappings.
                sizeP = mapping.size();

                // Make sure that none of the free variables went negative.
                while(elementMin(s_P.getMatrix()) <= 0.0)
                {
                    double alpha = 1.0;
                    int j = -1;
                    for (int i = 0; i < sizeP; i++)
                    {
                        double sVal = s_P.get(i);

                        if (sVal <= 0.0)
                        {
                            double xVal = x.get(mapping.get(i));
                            double alphaCandidate = xVal / (xVal - sVal);
                            if (alphaCandidate < alpha)
                            {
                                alpha = alphaCandidate;
                                j = mapping.get(i);
                            }
                        }
                    }

                    // Several sources seem to indicate that alpha should be negated at this point:
                    //         alpha = -min(x_i / (x_i - s_i)) where s_i <= 0.
                    // (i.e. set alpha = -alpha in this implementation).
                    // However, this is not the way it was originally published by Lawson and Hanson,
                    // and it doesn't make sense to negate it, since alpha should vary between 0 and 1.

                    // x = x + alpha * (s - x)
                    CommonOps_FDRM.addEquals(x.getMatrix(), (float)alpha, s.minus(x).getMatrix());

                    // Make sure that at least one previously positive value is set to zero.
                    // Because of round-off error, this is not necessarily guaranteed.
                    p[j] = false;
                    x.set(j, 0.0);

                    for (int i = 0; i < x.numRows(); i++)
                    {
                        if (p[i] && x.get(i) <= 0.0)
                        {
                            p[i] = false;
                            x.set(i, 0.0); // Just in case it went slightly negative due to round-off error.
                        }
                    }

                    mapping.clear();
                    s.set(0.0); // Set all elements to zero.

                    // Populates the mapping, sovles the system and copies it into s,
                    // and returns a vector containing only the free variables.
                    s_P = solvePartial(mATA, vATb, p, sizeP, mapping, s);

                    // Update size of P based on the number of mappings.
                    sizeP = mapping.size();
                }

                x = s;
                w = vATb.minus(mATA.mult(x));
            }
        }
        while(sizeP < p.length && maxW > epsilon);
        // The second condition makes the loop terminate if the earlier if-statement with the same condition evaluated to false.

        return x;
    }

    public static void main(String... args)
    {
        SimpleMatrix mA = new SimpleMatrix(10, 5, true
        ,    0.8147,    0.1576,    0.6557,    0.7060,    0.4387
        ,    0.9058,    0.9706,    0.0357,    0.0318,    0.3816
        ,    0.1270,    0.9572,    0.8491,    0.2769,    0.7655
        ,    0.9134,    0.4854,    0.9340,    0.0462,    0.7952
        ,    0.6324,    0.8003,    0.6787,    0.0971,    0.1869
        ,    0.0975,    0.1419,    0.7577,    0.8235,    0.4898
        ,    0.2785,    0.4218,    0.7431,    0.6948,    0.4456
        ,    0.5469,    0.9157,    0.3922,    0.3171,    0.6463
        ,    0.9575,    0.7922,    0.6555,    0.9502,    0.7094
        ,    0.9649,    0.9595,    0.1712,    0.0344,    0.7547
        );

        SimpleMatrix b = new SimpleMatrix(10, 1, true
                ,    0.2760
                ,    0.6797
                ,    0.6551
                ,    0.1626
                ,    0.1190
                ,    0.4984
                ,    0.9597
                ,    0.3404
                ,    0.5853
                ,    0.2238
        );

        // Result should be:
        //        0
        //   0.3594
        //        0
        //   0.5265
        //        0

        NonNegativeLeastSquaresSinglePrecision solver = new NonNegativeLeastSquaresSinglePrecision();
        SimpleMatrix x = solver.solve(mA, b, 0.001);
        x.print();
    }
}
