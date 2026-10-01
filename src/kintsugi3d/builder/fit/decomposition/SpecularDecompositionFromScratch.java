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

import kintsugi3d.builder.core.texture.TextureResolution;
import kintsugi3d.builder.io.specular.SpecularFitSerializer;
import kintsugi3d.gl.vecmath.DoubleVector3;
import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpecularDecompositionFromScratch extends SpecularDecompositionBase
{
    private final int materialCount;
    private final int basisResolution;

    private final DoubleVector3[] diffuseAlbedos;
    private final SimpleMatrix specularRed;
    private final SimpleMatrix specularGreen;
    private final SimpleMatrix specularBlue;

    public SpecularDecompositionFromScratch(TextureResolution textureResolution, int materialCount, int basisResolution)
    {
        super(textureResolution, materialCount);
        this.materialCount = materialCount;
        this.basisResolution = basisResolution;

        diffuseAlbedos = new DoubleVector3[materialCount];
        Arrays.fill(diffuseAlbedos, DoubleVector3.ZERO);

        specularRed = new SimpleMatrix(basisResolution + 1, materialCount, DMatrixRMaj.class);
        specularGreen = new SimpleMatrix(basisResolution + 1, materialCount, DMatrixRMaj.class);
        specularBlue = new SimpleMatrix(basisResolution + 1, materialCount, DMatrixRMaj.class);
    }

    @Override
    public MaterialBasis getMaterialBasis()
    {
        return new OptimizableMaterialBasis();
    }

    public void setDiffuseAlbedo(int basisIndex, DoubleVector3 diffuseAlbedo)
    {
        diffuseAlbedos[basisIndex] = diffuseAlbedo;
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

    private final class OptimizableMaterialBasis implements MaterialBasis
    {
        @Override
        public Collection<BasisMaterialInfo> getMaterials()
        {
            return getIndexableMaterialList();
        }

        @Override
        public List<BasisMaterialInfo> getIndexableMaterialList()
        {
            return IntStream.range(0, this.getEnabledMaterialCount())
                .mapToObj(b ->
                    new BasisMaterialInfo()
                    {
                        @Override
                        public DoubleVector3 getDiffuseColor()
                        {
                            return OptimizableMaterialBasis.this.getDiffuseColor(b);
                        }

                        @Override
                        public int getResolution()
                        {
                            return OptimizableMaterialBasis.this.getSpecularResolution();
                        }

                        @Override
                        public double evaluateSpecularRed(int m)
                        {
                            return OptimizableMaterialBasis.this.evaluateSpecularRed(b, m);
                        }

                        @Override
                        public double evaluateSpecularGreen(int m)
                        {
                            return OptimizableMaterialBasis.this.evaluateSpecularGreen(b, m);
                        }

                        @Override
                        public double evaluateSpecularBlue(int m)
                        {
                            return OptimizableMaterialBasis.this.evaluateSpecularBlue(b, m);
                        }

                        @Override
                        public String getName()
                        {
                            return String.format("%02d", b);
                        }

                        @Override
                        public String getFriendlyName()
                        {
                            return String.format("Material %d", b);
                        }

                        @Override
                        public boolean isEnabled()
                        {
                            return true;
                        }

                        @Override
                        public int getGPUIndex()
                        {
                            return b;
                        }
                    })
                .collect(Collectors.toList());
        }

        @Override
        public BasisMaterialInfo getMaterial(String materialName)
        {
            try
            {
                int index = Integer.parseInt(materialName);
                if (index >= 0 && index < materialCount)
                {
                    return getIndexableMaterialList().get(index);
                }
                else
                {
                    return null;
                }
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }

        @Override
        public int getMaterialCount()
        {
            return materialCount;
        }

        @Override
        public int getEnabledMaterialCount()
        {
            return materialCount;
        }

        @Override
        public int getSpecularResolution()
        {
            return basisResolution;
        }

        DoubleVector3 getDiffuseColor(int b)
        {
            return diffuseAlbedos[b];
        }

        double evaluateSpecularRed(int b, int m)
        {
            return specularRed.get(m, b);
        }

        double evaluateSpecularGreen(int b, int m)
        {
            return specularGreen.get(m, b);
        }

        double evaluateSpecularBlue(int b, int m)
        {
            return specularBlue.get(m, b);
        }

        @Override
        public void save(File outputDirectory, String filenameOverride)
        {
            SpecularFitSerializer.serializeBasisFunctions(basisResolution, this, outputDirectory, filenameOverride);
        }

        @Override
        public MutableMaterialBasis copy()
        {
            List<double[]> redBasis = IntStream.range(0, materialCount)
                .mapToObj(b ->
                    IntStream.range(0, basisResolution + 1)
                        .mapToDouble(m -> evaluateSpecularRed(b, m))
                        .toArray())
                .collect(Collectors.toList());

            List<double[]> greenBasis = IntStream.range(0, materialCount)
                .mapToObj(b ->
                    IntStream.range(0, basisResolution + 1)
                        .mapToDouble(m -> evaluateSpecularGreen(b, m))
                        .toArray())
                .collect(Collectors.toList());

            List<double[]> blueBasis = IntStream.range(0, materialCount)
                .mapToObj(b ->
                    IntStream.range(0, basisResolution + 1)
                        .mapToDouble(m -> evaluateSpecularBlue(b, m))
                        .toArray())
                .collect(Collectors.toList());

            return new SimpleMaterialBasis(diffuseAlbedos, redBasis, greenBasis, blueBasis);
        }
    }
}
