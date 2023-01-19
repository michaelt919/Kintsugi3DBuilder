/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

public interface SpecularBasis
{
    /**
     * Evaluates a red basis function
     * @param b the basis function to evaluate
     * @param m the discrete element at which to evaluate the basis function
     * @return the value of red basis function b, at element m.
     */
    double evaluateRed(int b, int m);

    /**
     * Evaluates a green basis function
     * @param b the basis function to evaluate
     * @param m the discrete element at which to evaluate the basis function
     * @return the value of green basis function b, at element m.
     */
    double evaluateGreen(int b, int m);

    /**
     * Evaluates a blue basis function
     * @param b the basis function to evaluate
     * @param m the discrete element at which to evaluate the basis function
     * @return the value of blue basis function b, at element m.
     */
    double evaluateBlue(int b, int m);
}
