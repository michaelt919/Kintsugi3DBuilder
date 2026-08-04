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

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.fit.settings.BasisSettings;
import kintsugi3d.builder.io.specular.SpecularFitSerializer;
import kintsugi3d.gl.vecmath.DoubleVector3;
import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpecularDecompositionFromScratch extends SpecularDecompositionBase
{
    private final BasisSettings basisSettings;

    private final Map<Integer, DoubleVector3> diffuseAlbedos;
    private SimpleMatrix specularRed;
    private SimpleMatrix specularGreen;
    private SimpleMatrix specularBlue;

    private final Map<Integer, Integer> names;

    private final Map<Integer, DoubleVector3> disabledDiffuseAlbedos;
    private SimpleMatrix disabledSpecularRed;
    private SimpleMatrix disabledSpecularGreen;
    private SimpleMatrix disabledSpecularBlue;

    private final Map<Integer, Integer> disabledNames;

    private final SimpleMatrix zeroMatrix;

    public SpecularDecompositionFromScratch(TextureResolution textureResolution, BasisSettings basisSettings)
    {
        super(textureResolution, basisSettings.getBasisCount());
        this.basisSettings = basisSettings;

        diffuseAlbedos = new HashMap<>(this.basisSettings.getBasisCount());
        disabledDiffuseAlbedos = new HashMap<>(0);

        for (int i = 0; i < this.basisSettings.getBasisCount(); i++)
        {
            diffuseAlbedos.put(i, DoubleVector3.ZERO);
        }

        specularRed = new SimpleMatrix(
            this.basisSettings.getBasisResolution() + 1,
            this.basisSettings.getBasisCount(), DMatrixRMaj.class);
        specularGreen = new SimpleMatrix(
            this.basisSettings.getBasisResolution() + 1,
            this.basisSettings.getBasisCount(), DMatrixRMaj.class);
        specularBlue = new SimpleMatrix(
            this.basisSettings.getBasisResolution() + 1,
            this.basisSettings.getBasisCount(), DMatrixRMaj.class);

        disabledSpecularRed = new SimpleMatrix(
            this.basisSettings.getBasisResolution() + 1,
            this.basisSettings.getBasisCount(), DMatrixRMaj.class);
        disabledSpecularGreen = new SimpleMatrix(
            this.basisSettings.getBasisResolution() + 1,
            this.basisSettings.getBasisCount(), DMatrixRMaj.class);
        disabledSpecularBlue = new SimpleMatrix(
            this.basisSettings.getBasisResolution() + 1,
            this.basisSettings.getBasisCount(), DMatrixRMaj.class);

        zeroMatrix = new SimpleMatrix(
            this.basisSettings.getBasisResolution() + 1,
            this.basisSettings.getBasisCount(), DMatrixRMaj.class);
        zeroMatrix.zero();

        names = new HashMap<>(diffuseAlbedos.size());
        IntStream.range(0, diffuseAlbedos.size()).forEach(i -> names.put(i, i));
        disabledNames = new HashMap<>(0);
    }

    @Override
    public List<DoubleVector3> getDiffuseAlbedos()
    {
        return List.copyOf(diffuseAlbedos.values());
    }

    @Override
    public MaterialBasis getMaterialBasis()
    {
        return new MaterialBasis()
        {
            private int count = basisSettings.getBasisCount();
            private int disabledMaterialCount = 0;
            private final int resolution = basisSettings.getBasisResolution();

            @Override
            public String getDisplayName(int cardIndex)
            {
                List<Integer> tempList = new ArrayList<>(names.size() + disabledNames.size());
                tempList.addAll(names.values());
                tempList.addAll(disabledNames.values());
                tempList.sort(Integer::compareTo);
                return Integer.toString(tempList.get(cardIndex));
            }

            @Override
            public DoubleVector3 getDiffuseColor(int b)
            {
                // just get enabled ones
                List<DoubleVector3> tempList = new ArrayList<>(diffuseAlbedos.values());
                return tempList.get(b);
            }

            @Override
            public DoubleVector3 getEnabledDiffuseColor(int b)
            {
                return getDiffuseColor(b);
            }

            @Override
            public List<DoubleVector3> getDiffuseColors()
            {
                return List.copyOf(diffuseAlbedos.values());
            }

            @Override
            public double evaluateSpecularRed(int b, int m)
            {
                int index;
                if (names.get(b) == null)
                {
                    index = disabledNames.get(b);
                }
                else
                {
                    index = names.get(b);
                }
                return specularRed.get(m, index);
            }

            @Override
            public double evaluateEnabledSpecularRed(int b, int m)
            {
                return specularRed.get(m, b);
            }

            public double evaluateDisabledSpecularRed(int b, int m)
            {
                List<Integer> keys = new ArrayList<>(disabledNames.keySet());
                return disabledSpecularRed.get(m, keys.get(b));
            }

            @Override
            public double evaluateSpecularGreen(int b, int m)
            {
                int index;
                if (names.get(b) == null)
                {
                    index = disabledNames.get(b);
                }
                else
                {
                    index = names.get(b);
                }
                return specularGreen.get(m, index);
            }

            @Override
            public double evaluateEnabledSpecularGreen(int b, int m)
            {
                return specularGreen.get(m, b);
            }

            public double evaluateDisabledSpecularGreen(int b, int m)
            {
                List<Integer> keys = new ArrayList<>(disabledNames.keySet());
                return disabledSpecularGreen.get(m, keys.get(b));
            }

            @Override
            public double evaluateSpecularBlue(int b, int m)
            {
                int index;
                if (names.get(b) == null)
                {
                    index = disabledNames.get(b);
                }
                else
                {
                    index = names.get(b);
                }
                return specularBlue.get(m, index);
            }

            @Override
            public double evaluateEnabledSpecularBlue(int b, int m)
            {
                return specularBlue.get(m, b);
            }

            public double evaluateDisabledSpecularBlue(int b, int m)
            {
                List<Integer> keys = new ArrayList<>(disabledNames.keySet());
                return disabledSpecularBlue.get(m, keys.get(b));
            }

            @Override
            public int getMaterialCount()
            {
                return count;
            }

            @Override
            public int getDisabledMaterialCount()
            {
                return disabledMaterialCount;
            }

            @Override
            public int getSpecularResolution()
            {
                return resolution;
            }

            @Override
            public void deleteMaterial(int b)
            {
                int index;
                if (names.get(b) == null)
                {
                    index = disabledNames.get(b);
                    disabledSpecularRed = removeColumn(disabledSpecularRed, disabledNames.get(index));
                    disabledSpecularGreen = removeColumn(disabledSpecularGreen, disabledNames.get(index));
                    disabledSpecularBlue = removeColumn(disabledSpecularBlue, disabledNames.get(index));
                    specularRed =  removeColumn(specularRed, disabledNames.get(index));
                    specularGreen =  removeColumn(specularGreen, disabledNames.get(index));
                    specularBlue = removeColumn(specularBlue, disabledNames.get(index));
                    disabledDiffuseAlbedos.remove(index);
                    disabledNames.remove(index);
                }
                else
                {
                    index = names.get(b);
                    specularRed = removeColumn(specularRed, names.get(index));
                    specularGreen = removeColumn(specularGreen, names.get(index));
                    specularBlue = removeColumn(specularBlue, names.get(index));
                    disabledSpecularRed = removeColumn(disabledSpecularRed, names.get(index));
                    disabledSpecularGreen = removeColumn(disabledSpecularGreen, names.get(index));
                    disabledSpecularBlue = removeColumn(disabledSpecularBlue, names.get(index));
                    names.remove(index);
                    diffuseAlbedos.remove(index);
                }
                count--;
            }

            @Override
            public void disableMaterial(int b)
            {
                if (names.get(b) != null)
                {
                    int name = names.get(b);
                    // add to disabled lists
                    disabledSpecularRed = addColumn(disabledSpecularRed, specularRed, name);
                    disabledSpecularGreen = addColumn(disabledSpecularGreen, specularGreen, name);
                    disabledSpecularBlue = addColumn(disabledSpecularBlue, specularBlue, name);
                    disabledSpecularRed = removeColumn(disabledSpecularRed, name + 1);
                    disabledSpecularGreen = removeColumn(disabledSpecularGreen, name + 1);
                    disabledSpecularBlue = removeColumn(disabledSpecularBlue, name + 1);
                    disabledDiffuseAlbedos.put(name, diffuseAlbedos.get(name));
                    disabledNames.put(name, name);
                    // remove from enabled lists
                    specularRed = removeColumn(specularRed, name);
                    specularGreen = removeColumn(specularGreen, name);
                    specularBlue = removeColumn(specularBlue, name);
                    specularRed = addColumn(specularRed, zeroMatrix, name);
                    specularGreen = addColumn(specularGreen, zeroMatrix, name);
                    specularBlue = addColumn(specularBlue, zeroMatrix, name);
                    diffuseAlbedos.remove(name);
                    names.remove(name);
                    count--;
                    disabledMaterialCount++;
                }
            }

            @Override
            public void enableMaterial(int b)
            {
                if (disabledNames.get(b) != null)
                {
                    int name = disabledNames.get(b);
                    // add to enabled lists
                    specularRed = addColumn(specularRed, disabledSpecularRed, name);
                    specularGreen = addColumn(specularGreen, disabledSpecularGreen, name);
                    specularBlue = addColumn(specularBlue, disabledSpecularBlue, name);
                    specularRed = removeColumn(specularRed, name + 1);
                    specularGreen = removeColumn(specularGreen, name + 1);
                    specularBlue = removeColumn(specularBlue, name + 1);
                    diffuseAlbedos.put(name, disabledDiffuseAlbedos.get(name));
                    names.put(name, disabledNames.get(name));
                    // remove from disabled lists
                    disabledSpecularRed = removeColumn(disabledSpecularRed, name);
                    disabledSpecularGreen = removeColumn(disabledSpecularGreen, name);
                    disabledSpecularBlue = removeColumn(disabledSpecularBlue, name);
                    disabledSpecularRed = addColumn(disabledSpecularRed, zeroMatrix, name);
                    disabledSpecularGreen = addColumn(disabledSpecularGreen, zeroMatrix, name);
                    disabledSpecularBlue = addColumn(disabledSpecularBlue, zeroMatrix, name);
                    disabledDiffuseAlbedos.remove(name);
                    disabledNames.remove(name);
                    count++;
                    disabledMaterialCount--;
                }
            }

            @Override
            public boolean getIsEnabled(int b)
            {
//                return names.get(b) != null;
                List<Integer> tempNames = new ArrayList<>(names.values());
                tempNames.addAll(disabledNames.values());
                tempNames.sort(Integer::compareTo);
                return names.containsKey(tempNames.get(b));
            }

            private SimpleMatrix removeColumn(SimpleMatrix m, int b)
            {
                SimpleMatrix result = new SimpleMatrix(m.numRows(), m.numCols() - 1, DMatrixRMaj.class);

                // Columns before the one being removed
                for (int j = 0; j < b; j++)
                {
                    for (int i = 0; i < m.numRows(); i++)
                    {
                        result.set(i, j, m.get(i, j));
                    }
                }

                // Columns after the one being removed
                for (int j = b; j < result.numCols(); j++)
                {
                    for (int i = 0; i < m.numRows(); i++)
                    {
                        result.set(i, j, m.get(i, j + 1));
                    }
                }

                return result;
            }

            private SimpleMatrix addColumn(SimpleMatrix m, SimpleMatrix source, int b)
            {
                SimpleMatrix result = new SimpleMatrix(m.numRows(), m.numCols() + 1, DMatrixRMaj.class);

                // Columns before the one being added
                for (int j = 0; j < b; j++)
                {
                    for (int i = 0; i < m.numRows(); i++)
                    {
                        result.set(i, j, m.get(i, j));
                    }
                }

                // Column to add
                for (int i = 0; i < m.numRows(); i++)
                {
                    result.set(i, b, source.get(i, b));
                }

                // Columns after the one being removed
                for (int j = b + 1; j < result.numCols(); j++)
                {
                    for (int i = 0; i < m.numRows(); i++)
                    {
                        result.set(i, j, m.get(i, j - 1));
                    }
                }

                return result;
            }

            @Override
            public void save(File outputDirectory, String filenameOverride)
            {
                SpecularFitSerializer.serializeBasisFunctions(count, resolution, this, outputDirectory, filenameOverride);
            }

            @Override
            public MaterialBasis copy()
            {
                List<double[]> redBasis = IntStream.range(0, count)
                    .mapToObj(b ->
                        IntStream.range(0, resolution + 1)
                            .mapToDouble(m -> evaluateEnabledSpecularRed(b, m))
                            .toArray())
                    .collect(Collectors.toList());

                List<double[]> greenBasis = IntStream.range(0, count)
                    .mapToObj(b ->
                        IntStream.range(0, resolution + 1)
                            .mapToDouble(m -> evaluateEnabledSpecularGreen(b, m))
                            .toArray())
                    .collect(Collectors.toList());

                List<double[]> blueBasis = IntStream.range(0, count)
                    .mapToObj(b ->
                        IntStream.range(0, resolution + 1)
                            .mapToDouble(m -> evaluateEnabledSpecularBlue(b, m))
                            .toArray())
                    .collect(Collectors.toList());

                List<double[]> disabledRedBasis = IntStream.range(0, disabledMaterialCount)
                    .mapToObj(b ->
                        IntStream.range(0, resolution + 1)
                            .mapToDouble(m -> evaluateDisabledSpecularRed(b, m))
                            .toArray())
                    .collect(Collectors.toList());

                List<double[]> disabledGreenBasis = IntStream.range(0, disabledMaterialCount)
                    .mapToObj(b ->
                        IntStream.range(0, resolution + 1)
                            .mapToDouble(m -> evaluateDisabledSpecularGreen(b, m))
                            .toArray())
                    .collect(Collectors.toList());

                List<double[]> disabledBlueBasis = IntStream.range(0, disabledMaterialCount)
                    .mapToObj(b ->
                        IntStream.range(0, resolution + 1)
                            .mapToDouble(m -> evaluateDisabledSpecularBlue(b, m))
                            .toArray())
                    .collect(Collectors.toList());

                return new SimpleMaterialBasis(
                    new ArrayList<>(names.values()), new ArrayList<>(diffuseAlbedos.values()),
                    redBasis, greenBasis, blueBasis,
                    new ArrayList<>(disabledNames.values()), new ArrayList<>(disabledDiffuseAlbedos.values()),
                    disabledRedBasis, disabledGreenBasis, disabledBlueBasis);
            }
        };
    }

    public void setDiffuseAlbedo(int basisIndex, DoubleVector3 diffuseAlbedo)
    {
        diffuseAlbedos.replace(basisIndex, diffuseAlbedo);
    }

    public SimpleMatrix getSpecularRed()
    {
        return specularRed;
    }

    public SimpleMatrix getSpecularGreen()
    {
        return specularGreen;
    }

    public SimpleMatrix getSpecularBlue()
    {
        return specularBlue;
    }
}
