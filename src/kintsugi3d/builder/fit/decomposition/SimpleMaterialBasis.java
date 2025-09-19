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

import kintsugi3d.builder.io.specular.SpecularFitSerializer;
import kintsugi3d.gl.vecmath.DoubleVector3;

import java.io.File;
import java.util.stream.IntStream;

public class SimpleMaterialBasis implements MaterialBasis
{
    private final DoubleVector3[] diffuseColors;

    private final double[][] redBasis;
    private final double[][] greenBasis;
    private final double[][] blueBasis;

    private final int materialCount;
    private final int specularResolution;

    public SimpleMaterialBasis(int materialCount, int specularResolution)
    {
        diffuseColors = IntStream.range(0, materialCount).mapToObj(b -> DoubleVector3.ZERO).toArray(DoubleVector3[]::new);
        redBasis = IntStream.range(0, materialCount).mapToObj(b -> new double[specularResolution + 1]).toArray(double[][]::new);
        greenBasis = IntStream.range(0, materialCount).mapToObj(b -> new double[specularResolution + 1]).toArray(double[][]::new);
        blueBasis = IntStream.range(0, materialCount).mapToObj(b -> new double[specularResolution + 1]).toArray(double[][]::new);
        this.materialCount = materialCount;
        this.specularResolution = specularResolution;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public SimpleMaterialBasis(DoubleVector3[] diffuseColors, double[][] redBasis, double[][] greenBasis, double[][] blueBasis)
    {
        this.diffuseColors = diffuseColors;
        this.redBasis = redBasis;
        this.greenBasis = greenBasis;
        this.blueBasis = blueBasis;
        this.materialCount = redBasis.length;
        this.specularResolution = redBasis[0].length - 1;
    }

    @Override
    public DoubleVector3 getDiffuseColor(int b)
    {
        return diffuseColors[b];
    }

    @Override
    public double evaluateSpecularRed(int b, int m)
    {
        return redBasis[b][m];
    }

    @Override
    public double evaluateSpecularGreen(int b, int m)
    {
        return greenBasis[b][m];
    }

    @Override
    public double evaluateSpecularBlue(int b, int m)
    {
        return blueBasis[b][m];
    }

    @Override
    public int getMaterialCount()
    {
        return materialCount;
    }

    @Override
    public int getSpecularResolution()
    {
        return specularResolution;
    }

    @Override
    public void save(File outputDirectory, String filenameOverride)
    {
        SpecularFitSerializer.serializeBasisFunctions(materialCount, specularResolution, this, outputDirectory, filenameOverride);
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
