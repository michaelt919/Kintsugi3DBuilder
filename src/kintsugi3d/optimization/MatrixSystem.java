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

import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.simple.SimpleMatrix;

import java.util.Arrays;
import java.util.stream.IntStream;

@SuppressWarnings("PublicField")
public class MatrixSystem
{
    /**
     * LHS
     */
    public final SimpleMatrix lhs;

    /**
     * RHS
     */
    public final SimpleMatrix[] rhs;

    public MatrixSystem(int matrixSize, int vectorCount, Class<?> matrixType)
    {
        lhs = new SimpleMatrix(matrixSize, matrixSize, matrixType);
        rhs = new SimpleMatrix[vectorCount];
        Arrays.setAll(rhs, i -> new SimpleMatrix(matrixSize, 1, matrixType));
    }

    public void addContribution(MatrixSystem contribution)
    {
        // Add the contribution into the main matrix and vectors.
        CommonOps_DDRM.addEquals(this.lhs.getMatrix(), contribution.lhs.getMatrix());
        for (int i = 0; i < rhs.length; i++)
        {
            CommonOps_DDRM.addEquals(this.rhs[i].getMatrix(), contribution.rhs[i].getMatrix());
        }
    }

    public void addToLHS(int row, int column, double amount)
    {
        lhs.set(row, column, lhs.get(row, column) + amount);
    }

    public void addToRHS(int row, int vectorIndex, double amount)
    {
        rhs[vectorIndex].set(row, 0, rhs[vectorIndex].get(row, 0) + amount);
    }

    public SimpleMatrix solve(int rhsIndex)
    {
        return lhs.solve(rhs[rhsIndex]);
    }

    public SimpleMatrix solveNonNegative(int rhsIndex, double toleranceScale)
    {
        double medianATy = IntStream.range(0, rhs[rhsIndex].getNumElements())
                .mapToDouble(rhs[rhsIndex]::get)
                .sorted()
                .skip(rhs[rhsIndex].getNumElements() / 2)
                .filter(x -> x > 0)
                .findFirst()
                .orElse(1.0);

        return NonNegativeLeastSquares.solvePremultiplied(lhs, rhs[rhsIndex],toleranceScale * medianATy);
    }
}
