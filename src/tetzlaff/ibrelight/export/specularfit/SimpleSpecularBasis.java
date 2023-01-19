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

import java.util.stream.IntStream;

public class SimpleSpecularBasis implements SpecularBasis
{
    private final double[][] redBasis;
    private final double[][] greenBasis;
    private final double[][] blueBasis;

    public SimpleSpecularBasis(int basisCount, int microfacetDistributionResolution)
    {
        redBasis = IntStream.range(0, basisCount).mapToObj(b -> new double[microfacetDistributionResolution + 1]).toArray(double[][]::new);
        greenBasis = IntStream.range(0, basisCount).mapToObj(b -> new double[microfacetDistributionResolution + 1]).toArray(double[][]::new);
        blueBasis = IntStream.range(0, basisCount).mapToObj(b -> new double[microfacetDistributionResolution + 1]).toArray(double[][]::new);
    }

    @Override
    public double evaluateRed(int b, int m)
    {
        return redBasis[b][m];
    }

    @Override
    public double evaluateGreen(int b, int m)
    {
        return greenBasis[b][m];
    }

    @Override
    public double evaluateBlue(int b, int m)
    {
        return blueBasis[b][m];
    }

    /**
     * Sets an element of the red basis function
     * @param b the basis function to modify
     * @param m the discrete element of the basis function to modify
     * @param value the new value of red basis function b, at element m.
     */
    public void setRed(int b, int m, double value)
    {
        redBasis[b][m] = value;
    }

    /**
     * Sets an element of the green basis function
     * @param b the basis function to modify
     * @param m the discrete element of the basis function to modify
     * @param value the new value of green basis function b, at element m.
     */
    public void setGreen(int b, int m, double value)
    {
        greenBasis[b][m] = value;
    }

    /**
     * Sets an element of the blue basis function
     * @param b the basis function to modify
     * @param m the discrete element of the basis function to modify
     * @param value the new value of blue basis function b, at element m.
     */
    public void setBlue(int b, int m, double value)
    {
        blueBasis[b][m] = value;
    }
}
