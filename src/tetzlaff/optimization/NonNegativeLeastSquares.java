/*
 *  Copyright (c) Michael Tetzlaff 2021
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.optimization;

import org.ejml.data.SingularMatrixException;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.ejml.dense.row.CommonOps_DDRM.multTransA;

public final class NonNegativeLeastSquares
{
    private static SimpleMatrix solvePartial(
        SimpleMatrix mATA, SimpleMatrix vATb, boolean[] p, List<Integer> mapping, SimpleMatrix sOut, int constraintCount)
    {
        for (int index = 0; index < p.length; index++)
        {
            if (p[index])
            {
                mapping.add(index);
            }
        }

        // Add equality constraints if there are any.
        for (int i = mATA.numRows() - constraintCount; i < mATA.numRows(); i++)
        {
            mapping.add(i);
        }

        // Create versions of A'A and A'b containing only the rows and columns
        // corresponding to the free variables.
        SimpleMatrix mATA_P = new SimpleMatrix(mapping.size(), mapping.size());
        SimpleMatrix vATb_P = new SimpleMatrix(mapping.size(), 1);

        for (int i = 0; i < mATA_P.numRows(); i++)
        {
            vATb_P.set(i, vATb.get(mapping.get(i)));

            for (int j = 0; j < mATA_P.numCols(); j++)
            {
                mATA_P.set(i, j, mATA.get(mapping.get(i), mapping.get(j)));
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

    private static double minNonConstraint(SimpleMatrix s, int constraintCount)
    {
        return IntStream.range(0, s.numRows() - constraintCount)
            .mapToDouble(s::get)
            .min()
            .orElse(Double.NEGATIVE_INFINITY);
    }

    /**
     * Solves a non-negative least squares problem that minimizes ||Ax - b||^2
     * @param mA The matrix A
     * @param b The vector b
     * @param epsilon The allowed tolerance at which the algorithm will terminate.
     * @return The non-negative least squares solution.
     */
    public static SimpleMatrix solve(SimpleMatrix mA, SimpleMatrix b, double epsilon)
    {
        if (b.numCols() != 1 || b.numRows() != mA.numRows())
        {
            throw new IllegalArgumentException("b must be a column vector with the same number of rows as matrix A.");
        }

        // Precompute matrix products
        SimpleMatrix mATA = new SimpleMatrix(mA.numCols(), mA.numCols());
        SimpleMatrix vATb = new SimpleMatrix(mA.numCols(), 1);

        // Low level operations to avoid using unnecessary memory.
        multTransA(mA.getMatrix(), mA.getMatrix(), mATA.getMatrix());
        multTransA(mA.getMatrix(), b.getMatrix(), vATb.getMatrix());

        return solvePremultiplied(mATA, vATb, epsilon);
    }

    /**
     * Solves a non-negative least squares problem that minimizes ||Ax - b||^2, using the premultiplied form A'Ax - A'b.
     * @param mATA The matrix product A' (A transpose) times A.
     * @param vATb The product A' (A transpose) times b.
     * @param epsilon The allowed tolerance at which the algorithm will terminate.
     * @return The non-negative least squares solution.
     */
    public static SimpleMatrix solvePremultiplied(SimpleMatrix mATA, SimpleMatrix vATb, double epsilon)
    {
        return solvePremultipliedWithEqualityConstraints(mATA, vATb, epsilon, 0);
    }

    /**
     * Solves a non-negative least squares problem that minimizes ||Ax - b||^2, using the premultiplied form A'Ax - A'b.
     * This overload also supports additional equality constraints that must be satisfied in addition to non-negativity.
     * This constraints should be appended to the premultiplied form A'Ax - A'b to form an augmented linear system.
     * https://en.wikipedia.org/wiki/Quadratic_programming#Equality_constraints
     * @param augmentedATA The upper left partition of this matrix is the matrix product A' (A transpose) times A.
     *                     The bottom left partition of this matrix stores the LHS of the equality constraints.
     *                     The top right partition should have be the same as the bottom left, but transposed.
     *                     The bottom right partition should store all zeros.
     * @param augmentedATb The upper partition of this vector is the product A' (A transpose) times b.
     *                     The bottom partition of this vector is the RHS of the equality constraints.
     * @param epsilon The allowed tolerance at which the algorithm will terminate.
     * @param constraintCount The number of rows and columns of the provided linear system that are assumed to be constraints.
     *                        These are taken to be at the bottom and right of the matrix.
     *                        If the matrix (which should be square) has a total of n rows and n columns, and there are k constraints,
     *                        then it is assumed that the first n-k rows and columns are the premultiplied matrix A'A,
     *                        and the final k rows and columns are where the constraints are provided.
     * @return The non-negative least squares solution, augmented with the Lagrange multipliers for the equality constraints.
     */
    public static SimpleMatrix solvePremultipliedWithEqualityConstraints(
        SimpleMatrix augmentedATA, SimpleMatrix augmentedATb, double epsilon, int constraintCount)
    {
        if (augmentedATA.numCols() != augmentedATA.numRows())
        {
            throw new IllegalArgumentException("A'A must be a square matrix.");
        }

        if (augmentedATb.numCols() != 1 || augmentedATb.numRows() != augmentedATA.numRows())
        {
            throw new IllegalArgumentException("A'b must be a column vector with the same number of rows as matrix A'A.");
        }

        if (epsilon <= 0.0)
        {
            throw new IllegalArgumentException("Epsilon must be greater than zero.");
        }

        // Keep track of the set of free variables (where p[i] is true)
        // All other variables are fixed at zero.
        boolean[] p = new boolean[augmentedATA.numCols() - constraintCount];

        // Keep track of the number of free variables.
        int sizeP = 0;

        SimpleMatrix x = new SimpleMatrix(augmentedATA.numCols(), 1);
        SimpleMatrix w = augmentedATb.copy();

        int k = -1;
        double maxW;

        // Mapping from the set of free variables to the set of all variables.
        List<Integer> mapping = new ArrayList<>(augmentedATA.numRows());

        do
        {
            maxW = -1.0;

            for (int i = 0; i < w.numRows() - constraintCount; i++)
            {
                double value = w.get(i);
                if (!p[i] && value > maxW)
                {
                    k = i;
                    maxW = value;
                }
            }

            // Iterate until effectively no values of w are positive.
            if (maxW > epsilon || sizeP == 0)
            {
                p[k] = true;

                SimpleMatrix s = new SimpleMatrix(augmentedATA.numCols(), 1);

                // Clear the mapping so that it can be repopulated by solvePartial().
                mapping.clear();

                try
                {
                    // Populates the mapping, solves the system and copies it into s, and returns a vector containing only the free variables.
                    SimpleMatrix s_P = solvePartial(augmentedATA, augmentedATb, p, mapping, s, constraintCount);

                    // Update size of P based on the number of mappings, accounting for the space used for equality constraints at the end of the mappings.
                    sizeP = mapping.size() - constraintCount;

                    // Make sure that none of the free variables went negative.
                    while (minNonConstraint(s_P, constraintCount) < 0.0)
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
                                if (alphaCandidate <= alpha)
                                {
                                    alpha = alphaCandidate;
                                    j = mapping.get(i);
                                }
                            }
                        }

                        // x = x + alpha * (s - x)
                        CommonOps_DDRM.addEquals(x.getMatrix(), alpha, s.minus(x).getMatrix());

                        // Make sure that at least one previously positive value is set to zero.
                        // Because of round-off error, this is not necessarily guaranteed.
                        p[j] = false;
                        x.set(j, 0.0);

                        if (j == k)
                        {
                            // Avoid an infinite loop; treat all remaining values in w as insignificant.
                            maxW = 0.0;
                        }
                        else
                        {
                            for (int i = 0; i < x.numRows() - constraintCount; i++)
                            {
                                if (p[i] && x.get(i) <= 0.0)
                                {
                                    p[i] = false;
                                    x.set(i, 0.0); // Just in case it went slightly negative due to round-off error.
                                }
                            }
                        }

                        mapping.clear();
                        s.fill(0.0); // Set all elements to zero.

                        // Populates the mapping, solves the system and copies it into s, and returns a vector containing only the free variables.
                        s_P = solvePartial(augmentedATA, augmentedATb, p, mapping, s, constraintCount);

                        // Update size of P based on the number of mappings.
                        sizeP = mapping.size() - constraintCount;
                    }
                }
                catch(SingularMatrixException e)
                {
                    e.printStackTrace();

                    // Roll back and finish.
                    p[k] = false;
                    x.set(k, 0.0);

                    mapping.clear();
                    s.fill(0.0); // Set all elements to zero.

                    // Populates the mapping, solves the system and copies it into s, and returns a vector containing only the free variables.
                    solvePartial(augmentedATA, augmentedATb, p, mapping, s, constraintCount);

                    // Update size of P based on the number of mappings.
                    sizeP = mapping.size() - constraintCount;

                    // Avoid an infinite loop; treat all remaining values in w as insignificant.
                    maxW = 0.0;
                }

                x = s;
                w = augmentedATb.minus(augmentedATA.mult(x));
            }
        }
        while(sizeP < p.length && maxW > epsilon);
        // The second condition makes the loop terminate if the earlier if-statement with the same condition evaluated to false.

        return x;
    }

    public static void main(String... args)
    {
//        SimpleMatrix mA = new SimpleMatrix(10, 5, true
//        ,    0.8147,    0.1576,    0.6557,    0.7060,    0.4387
//        ,    0.9058,    0.9706,    0.0357,    0.0318,    0.3816
//        ,    0.1270,    0.9572,    0.8491,    0.2769,    0.7655
//        ,    0.9134,    0.4854,    0.9340,    0.0462,    0.7952
//        ,    0.6324,    0.8003,    0.6787,    0.0971,    0.1869
//        ,    0.0975,    0.1419,    0.7577,    0.8235,    0.4898
//        ,    0.2785,    0.4218,    0.7431,    0.6948,    0.4456
//        ,    0.5469,    0.9157,    0.3922,    0.3171,    0.6463
//        ,    0.9575,    0.7922,    0.6555,    0.9502,    0.7094
//        ,    0.9649,    0.9595,    0.1712,    0.0344,    0.7547
//        );
//
//        SimpleMatrix b = new SimpleMatrix(10, 1, true
//                ,    0.2760
//                ,    0.6797
//                ,    0.6551
//                ,    0.1626
//                ,    0.1190
//                ,    0.4984
//                ,    0.9597
//                ,    0.3404
//                ,    0.5853
//                ,    0.2238
//        );
//
//        // Result should be:
//        //        0
//        //   0.3594
//        //        0
//        //   0.5265
//        //        0
//
//        SimpleMatrix x = solve(mA, b, 0.001);
//        x.print();
    }
}
