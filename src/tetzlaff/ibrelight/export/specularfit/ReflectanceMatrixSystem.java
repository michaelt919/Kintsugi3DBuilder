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

package tetzlaff.ibrelight.export.specularfit;

import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.simple.SimpleMatrix;

@SuppressWarnings("PublicField")
public class ReflectanceMatrixSystem
{
    /**
     * LHS
     */
    public final SimpleMatrix matrixATA;

    /**
     * RHS for red
     */
    public final SimpleMatrix vectorATyRed;

    /**
     * RHS for green
     */
    public final SimpleMatrix vectorATyGreen;

    /**
     * RHS for blue
     */
    public final SimpleMatrix vectorATyBlue;

    public ReflectanceMatrixSystem(int matrixSize, Class<?> matrixType)
    {
        matrixATA = new SimpleMatrix(matrixSize, matrixSize, matrixType);
        vectorATyRed = new SimpleMatrix(matrixSize, 1, matrixType);
        vectorATyGreen = new SimpleMatrix(matrixSize, 1, matrixType);
        vectorATyBlue = new SimpleMatrix(matrixSize, 1, matrixType);
    }

    public void addContribution(ReflectanceMatrixSystem contribution)
    {
        // Add the contribution into the main matrix and vectors.
        CommonOps_DDRM.addEquals(this.matrixATA.getMatrix(), contribution.matrixATA.getMatrix());
        CommonOps_DDRM.addEquals(this.vectorATyBlue.getMatrix(), contribution.vectorATyRed.getMatrix());
        CommonOps_DDRM.addEquals(this.vectorATyGreen.getMatrix(), contribution.vectorATyGreen.getMatrix());
        CommonOps_DDRM.addEquals(this.vectorATyBlue.getMatrix(), contribution.vectorATyBlue.getMatrix());
    }
}
