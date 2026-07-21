/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SimpleMaterialBasis implements MaterialBasis
{
    private final List<DoubleVector3> diffuseColors;

    private final List<double[]> redBasis;
    private final List<double[]> greenBasis;
    private final List<double[]> blueBasis;

    private final List<String> names;

    private final List<DoubleVector3> disabledDiffuseColors;
    private final List<double[]> disabledRedBasis;
    private final List<double[]> disabledGreenBasis;
    private final List<double[]> disabledBlueBasis;

    private final List<String> disabledNames;

    private int materialCount;
    private final int specularResolution;

    public SimpleMaterialBasis(int materialCount, int specularResolution)
    {
        diffuseColors = IntStream.range(0, materialCount).mapToObj(b -> DoubleVector3.ZERO).collect(Collectors.toList());
        redBasis = IntStream.range(0, materialCount).mapToObj(b -> new double[specularResolution + 1]).collect(Collectors.toList());
        greenBasis = IntStream.range(0, materialCount).mapToObj(b -> new double[specularResolution + 1]).collect(Collectors.toList());
        blueBasis = IntStream.range(0, materialCount).mapToObj(b -> new double[specularResolution + 1]).collect(Collectors.toList());
        names = IntStream.range(0, materialCount).mapToObj(String::valueOf).collect(Collectors.toList());

        disabledDiffuseColors = new ArrayList<>(0);
        disabledRedBasis = new ArrayList<>(0);
        disabledGreenBasis = new ArrayList<>(0);
        disabledBlueBasis = new ArrayList<>(0);
        disabledNames = new ArrayList<>(0);

        this.materialCount = materialCount;
        this.specularResolution = specularResolution;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public SimpleMaterialBasis(DoubleVector3[] diffuseColors, List<double[]> redBasis, List<double[]> greenBasis, List<double[]> blueBasis)
    {
        this.diffuseColors = new ArrayList<>(List.of(diffuseColors));
        this.redBasis = redBasis;
        this.greenBasis = greenBasis;
        this.blueBasis = blueBasis;
        this.materialCount = redBasis.size();
        this.specularResolution = redBasis.get(0).length - 1;
        names = IntStream.range(0, materialCount).mapToObj(String::valueOf).collect(Collectors.toList());

        disabledDiffuseColors = new ArrayList<>(0);
        disabledRedBasis = new ArrayList<>(0);
        disabledGreenBasis = new ArrayList<>(0);
        disabledBlueBasis = new ArrayList<>(0);
        disabledNames = new ArrayList<>(0);
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public SimpleMaterialBasis(List<String> names, List<DoubleVector3> diffuseColors,
                               List<double[]> redBasis, List<double[]> greenBasis, List<double[]> blueBasis,
                               List<String> disabledNames, List<DoubleVector3> disabledDiffuseColors,
                               List<double[]> disabledRedBasis, List<double[]> disabledGreenBasis, List<double[]> disabledBlueBasis)
    {
        this.diffuseColors = diffuseColors;
        this.redBasis = redBasis;
        this.greenBasis = greenBasis;
        this.blueBasis = blueBasis;
        this.names = names;

        this.disabledDiffuseColors = disabledDiffuseColors;
        this.disabledRedBasis = disabledRedBasis;
        this.disabledGreenBasis = disabledGreenBasis;
        this.disabledBlueBasis = disabledBlueBasis;
        this.disabledNames = disabledNames;

        this.materialCount = redBasis.size() + disabledRedBasis.size();
        this.specularResolution = redBasis.get(0).length - 1;
    }

    @Override
    public String getName(int b)
    {
        return names.get(b);
    }

    @Override
    public String getDisplayName(int cardIndex)
    {
        if (cardIndex < names.size())
        {
            return names.get(cardIndex);
        }
        else
        {
            return disabledNames.get(cardIndex - names.size());
        }
    }

    @Override
    public DoubleVector3 getDiffuseColor(int b)
    {
        int index = names.indexOf(Integer.toString(b));
        if (index != -1)
        {
            return diffuseColors.get(index);
        }
        else
        {
            index = disabledNames.indexOf(Integer.toString(b));
            return disabledDiffuseColors.get(index);
        }
    }

    @Override
    public List<DoubleVector3> getDiffuseColors()
    {
        return Collections.unmodifiableList(diffuseColors);
    }

    @Override
    public double evaluateSpecularRed(int b, int m)
    {
        int index = names.indexOf(Integer.toString(b));
        if (index != -1)
        {
            return redBasis.get(index)[m];
        }
        else
        {
            index = disabledNames.indexOf(Integer.toString(b));
            return disabledRedBasis.get(index)[m];
        }
    }

    @Override
    public double evaluateSpecularGreen(int b, int m)
    {
        int index = names.indexOf(Integer.toString(b));
        if (index != -1)
        {
            return greenBasis.get(index)[m];
        }
        else
        {
            index = disabledNames.indexOf(Integer.toString(b));
            return disabledGreenBasis.get(index)[m];
        }
    }

    @Override
    public double evaluateSpecularBlue(int b, int m)
    {
        int index = names.indexOf(Integer.toString(b));
        if (index != -1)
        {
            return blueBasis.get(index)[m];
        }
        else
        {
            index = disabledNames.indexOf(Integer.toString(b));
            return disabledBlueBasis.get(index)[m];
        }
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
    public void deleteMaterial(int b)
    {
        redBasis.remove(b);
        greenBasis.remove(b);
        blueBasis.remove(b);
        diffuseColors.remove(b);
        names.remove(b);
        materialCount--;
    }

    @Override
    public void save(File outputDirectory, String filenameOverride)
    {
        SpecularFitSerializer.serializeBasisFunctions(redBasis.size() + disabledRedBasis.size(), specularResolution, this, outputDirectory, filenameOverride);
    }

    @Override
    public MaterialBasis copy()
    {
        // TODO: add disabled lists copy support
        return new SimpleMaterialBasis(diffuseColors.toArray(DoubleVector3[]::new),
            List.copyOf(redBasis), List.copyOf(greenBasis), List.copyOf(blueBasis));
    }

    @Override
    public void disableMaterial(int b)
    {
        // get persistent name
        int name = names.indexOf(Integer.toString(b));
        if (name != -1)
        {
            int index = Math.min(name, disabledNames.size());
            // copy to disabled lists
            disabledRedBasis.add(index, redBasis.get(name));
            disabledGreenBasis.add(index, greenBasis.get(name));
            disabledBlueBasis.add(index, blueBasis.get(name));
            disabledDiffuseColors.add(index, diffuseColors.get(name));
            disabledNames.add(index, names.get(name));
            // remove from enabled lists
            redBasis.remove(name);
            greenBasis.remove(name);
            blueBasis.remove(name);
            diffuseColors.remove(name);
            names.remove(name);
            materialCount--;
        }
    }

    @Override
    public void enableMaterial(int b)
    {
        // get persistent name
        int name = disabledNames.indexOf(Integer.toString(b));
        if (name != -1)
        {
            int index = Math.min(name, names.size());
            // add to enabled lists
            redBasis.add(index, disabledRedBasis.get(name));
            greenBasis.add(index, disabledGreenBasis.get(name));
            blueBasis.add(index, disabledBlueBasis.get(name));
            diffuseColors.add(index, disabledDiffuseColors.get(name));
            names.add(index, disabledNames.get(name));
            // remove from disabled lists
            disabledRedBasis.remove(name);
            disabledGreenBasis.remove(name);
            disabledBlueBasis.remove(name);
            disabledDiffuseColors.remove(name);
            disabledNames.remove(name);
            materialCount++;
        }
    }

    @Override
    public boolean getIsEnabled(int b)
    {
        return names.contains(Integer.toString(b));
    }

    /**
     * Sets an element of the red basis function
     * @param b the basis function to modify
     * @param m the discrete element of the basis function to modify
     * @param value the new value of red basis function b, at element m.
     */
    public void setRed(int b, int m, double value)
    {
        redBasis.get(b)[m] = value;
    }

    /**
     * Sets an element of the green basis function
     * @param b the basis function to modify
     * @param m the discrete element of the basis function to modify
     * @param value the new value of green basis function b, at element m.
     */
    public void setGreen(int b, int m, double value)
    {
        greenBasis.get(b)[m] = value;
    }

    /**
     * Sets an element of the blue basis function
     * @param b the basis function to modify
     * @param m the discrete element of the basis function to modify
     * @param value the new value of blue basis function b, at element m.
     */
    public void setBlue(int b, int m, double value)
    {
        blueBasis.get(b)[m] = value;
    }
}
