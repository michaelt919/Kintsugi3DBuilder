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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpecularDecompositionFromScratch extends SpecularDecompositionBase
{
    private final BasisSettings basisSettings;

    private final List<DoubleVector3> diffuseAlbedos;
    private SimpleMatrix specularRed;
    private SimpleMatrix specularGreen;
    private SimpleMatrix specularBlue;

    private final List<String> names;

    private final List<DoubleVector3> disabledDiffuseAlbedos;
    private SimpleMatrix disabledSpecularRed;
    private SimpleMatrix disabledSpecularGreen;
    private SimpleMatrix disabledSpecularBlue;

    private final List<String> disabledNames;

    public SpecularDecompositionFromScratch(TextureResolution textureResolution, BasisSettings basisSettings)
    {
        super(textureResolution, basisSettings.getBasisCount());
        this.basisSettings = basisSettings;

        diffuseAlbedos = new ArrayList<>(this.basisSettings.getBasisCount());
        disabledDiffuseAlbedos = new ArrayList<>(0);

        for (int i = 0; i < this.basisSettings.getBasisCount(); i++)
        {
            diffuseAlbedos.add(DoubleVector3.ZERO);
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

        names = IntStream.range(0, diffuseAlbedos.size()).mapToObj(String::valueOf).collect(Collectors.toList());
        disabledNames = new ArrayList<>(0);
    }

    @Override
    public List<DoubleVector3> getDiffuseAlbedos()
    {
        return Collections.unmodifiableList(diffuseAlbedos);
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
            public int getName(int b)
            {
                return Integer.parseInt(names.get(b));
            }

            @Override
            public String getDisplayName(int cardIndex)
            {
                int index = names.indexOf(Integer.toString(cardIndex));
                if (index != -1)
                {
                    return names.get(cardIndex);
                }
                else
                {
                    index = disabledNames.indexOf(Integer.toString(cardIndex));
                    return disabledNames.get(index);
                }
            }

            @Override
            public DoubleVector3 getDiffuseColor(int b)
            {
//                int index = names.indexOf(Integer.toString(b));
//                if (index != -1)
//                {
//                    return diffuseAlbedos.get(index);
//                }
//                else
//                {
//                    index = disabledNames.indexOf(Integer.toString(b));
//                    return disabledDiffuseAlbedos.get(index);
//                }
                return diffuseAlbedos.get(b);
            }

            @Override
            public List<DoubleVector3> getDiffuseColors()
            {
                return Collections.unmodifiableList(diffuseAlbedos);
            }

            @Override
            public double evaluateSpecularRed(int b, int m)
            {
                int index = names.indexOf(Integer.toString(b));
                if (index != -1)
                {
                    return specularRed.get(m, index);
                }
                else
                {
                    index = disabledNames.indexOf(Integer.toString(b));
                    return specularRed.get(m, index);
                }
            }

            @Override
            public double evaluateEnabledSpecularRed(int b, int m)
            {
                return specularRed.get(m, b);
            }

            @Override
            public double evaluateSpecularGreen(int b, int m)
            {
                int index = names.indexOf(Integer.toString(b));
                if (index != -1)
                {
                    return specularGreen.get(m, index);
                }
                else
                {
                    index = disabledNames.indexOf(Integer.toString(b));
                    return specularGreen.get(m, index);
                }
            }

            @Override
            public double evaluateEnabledSpecularGreen(int b, int m)
            {
                return specularGreen.get(m, b);
            }

            @Override
            public double evaluateSpecularBlue(int b, int m)
            {
                int index = names.indexOf(Integer.toString(b));
                if (index != -1)
                {
                    return specularBlue.get(m, index);
                }
                else
                {
                    index = disabledNames.indexOf(Integer.toString(b));
                    return specularBlue.get(m, index);
                }
            }

            @Override
            public double evaluateEnabledSpecularBlue(int b, int m)
            {
                return specularBlue.get(m, b);
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
                int index = names.indexOf(Integer.toString(b));
                if (index != -1)
                {
                    specularRed = removeColumn(specularRed, Integer.parseInt(names.get(index)));
                    specularGreen = removeColumn(specularGreen, Integer.parseInt(names.get(index)));
                    specularBlue = removeColumn(specularBlue, Integer.parseInt(names.get(index)));
                    disabledSpecularRed = removeColumn(specularRed, Integer.parseInt(names.get(index)));
                    disabledSpecularGreen = removeColumn(specularGreen, Integer.parseInt(names.get(index)));
                    disabledSpecularBlue = removeColumn(specularBlue, Integer.parseInt(names.get(index)));
                    names.remove(index);
                    diffuseAlbedos.remove(index);
                }
                else
                {
                    index = disabledNames.indexOf(Integer.toString(b));
                    disabledSpecularRed = removeColumn(specularRed, Integer.parseInt(disabledNames.get(index)));
                    disabledSpecularGreen = removeColumn(specularGreen, Integer.parseInt(disabledNames.get(index)));
                    disabledSpecularBlue = removeColumn(specularBlue, Integer.parseInt(disabledNames.get(index)));
                    specularRed = removeColumn(specularRed, Integer.parseInt(disabledNames.get(index)));
                    specularGreen = removeColumn(specularGreen, Integer.parseInt(disabledNames.get(index)));
                    specularBlue = removeColumn(specularBlue, Integer.parseInt(disabledNames.get(index)));
                    disabledDiffuseAlbedos.remove(index);
                    disabledNames.remove(index);
                }
                count--;
            }

            @Override
            public void disableMaterial(int b)
            {
                int name = names.indexOf(Integer.toString(b));
                if (name != -1)
                {
                    int index = Math.min(name, disabledNames.size());
                    // add to disabled lists
                    disabledSpecularRed = addColumn(disabledSpecularRed, specularRed, name);
                    disabledSpecularGreen = addColumn(disabledSpecularGreen, specularGreen, name);
                    disabledSpecularBlue = addColumn(disabledSpecularBlue, specularBlue, name);
                    disabledDiffuseAlbedos.add(index, diffuseAlbedos.get(name));
                    disabledNames.add(index, names.get(name));
                    // remove from enabled lists
                    specularRed = removeColumn(specularRed, name);
                    specularGreen = removeColumn(specularGreen, name);
                    specularBlue = removeColumn(specularBlue, name);
                    diffuseAlbedos.remove(name);
                    names.remove(name);
                    count--;
                    disabledMaterialCount++;
                }
            }

            @Override
            public void enableMaterial(int b)
            {
                int name = disabledNames.indexOf(Integer.toString(b));
                if (name != -1)
                {
                    int index = Math.min(name, names.size());
                    // add to enabled lists
                    specularRed = addColumn(specularRed, disabledSpecularRed, name);
                    specularGreen = addColumn(specularGreen, disabledSpecularGreen, name);
                    specularBlue = addColumn(specularBlue, disabledSpecularBlue, name);
                    diffuseAlbedos.add(index, disabledDiffuseAlbedos.get(name));
                    names.add(index, disabledNames.get(name));
                    // remove from disabled lists
                    disabledSpecularRed = removeColumn(disabledSpecularRed, name);
                    disabledSpecularGreen = removeColumn(disabledSpecularGreen, name);
                    disabledSpecularBlue = removeColumn(disabledSpecularBlue, name);
                    disabledDiffuseAlbedos.remove(name);
                    disabledNames.remove(name);
                    count++;
                    disabledMaterialCount--;
                }
            }

            @Override
            public boolean getIsEnabled(int b)
            {
                return names.contains(Integer.toString(b));
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
                            .mapToDouble(m -> evaluateSpecularRed(b, m))
                            .toArray())
                    .collect(Collectors.toList());

                List<double[]> greenBasis = IntStream.range(0, count)
                    .mapToObj(b ->
                        IntStream.range(0, resolution + 1)
                            .mapToDouble(m -> evaluateSpecularGreen(b, m))
                            .toArray())
                    .collect(Collectors.toList());

                List<double[]> blueBasis = IntStream.range(0, count)
                    .mapToObj(b ->
                        IntStream.range(0, resolution + 1)
                            .mapToDouble(m -> evaluateSpecularBlue(b, m))
                            .toArray())
                    .collect(Collectors.toList());

                return new SimpleMaterialBasis(
                    diffuseAlbedos.toArray(DoubleVector3[]::new), redBasis, greenBasis, blueBasis);
            }
        };
    }

    public void setDiffuseAlbedo(int basisIndex, DoubleVector3 diffuseAlbedo)
    {
        diffuseAlbedos.set(basisIndex, diffuseAlbedo);
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
