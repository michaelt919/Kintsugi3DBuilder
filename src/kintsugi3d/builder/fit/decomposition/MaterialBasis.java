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

package kintsugi3d.builder.fit.decomposition;

import kintsugi3d.gl.vecmath.DoubleVector3;

import java.io.File;

public interface MaterialBasis // TODO: avoid use of anonymous classes, add copy() method to improve robustness
{
    /**
     * Gets the diffuse color for a particular basis function.
     * @param b the basis function for which to retrieve the diffuse color.
     * @return the diffuse color as a DoubleVector3 for basis function b.
     */
    DoubleVector3 getDiffuseColor (int b);

    /**
     * Evaluates a red basis function
     * @param b the basis function to evaluate
     * @param m the discrete element at which to evaluate the basis function
     * @return the value of red basis function b, at element m.
     */
    double evaluateSpecularRed(int b, int m);

    /**
     * Evaluates a green basis function
     * @param b the basis function to evaluate
     * @param m the discrete element at which to evaluate the basis function
     * @return the value of green basis function b, at element m.
     */
    double evaluateSpecularGreen(int b, int m);

    /**
     * Evaluates a blue basis function
     * @param b the basis function to evaluate
     * @param m the discrete element at which to evaluate the basis function
     * @return the value of blue basis function b, at element m.
     */
    double evaluateSpecularBlue(int b, int m);

    int getMaterialCount();
    int getSpecularResolution();

    void save(File outputDirectory);
}
