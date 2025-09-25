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

public class SpecularDecompositionFromScratch extends SpecularDecompositionBase
{
    private final List<DoubleVector3> diffuseAlbedos;
    private final SimpleMatrix specularRed;
    private final SimpleMatrix specularGreen;
    private final SimpleMatrix specularBlue;

    private final BasisSettings basisSettings;

    public SpecularDecompositionFromScratch(TextureResolution textureResolution, BasisSettings basisSettings)
    {
        super(textureResolution, basisSettings.getBasisCount());
        this.basisSettings = basisSettings;

        diffuseAlbedos = new ArrayList<>(this.basisSettings.getBasisCount());

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
            final int count = basisSettings.getBasisCount();
            final int resolution = basisSettings.getBasisResolution();

            @Override
            public DoubleVector3 getDiffuseColor(int b)
            {
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
                return specularRed.get(m, b);
            }

            @Override
            public double evaluateSpecularGreen(int b, int m)
            {
                return specularGreen.get(m, b);
            }

            @Override
            public double evaluateSpecularBlue(int b, int m)
            {
                return specularBlue.get(m, b);
            }

            @Override
            public int getMaterialCount()
            {
                return count;
            }

            @Override
            public int getSpecularResolution()
            {
                return resolution;
            }

            @Override
            public void deleteMaterial(int b)
            {
                diffuseAlbedos.remove(b);
                removeColumn(specularRed, b);
                removeColumn(specularGreen, b);
                removeColumn(specularBlue, b);
            }

            private void removeColumn(SimpleMatrix m, int b)
            {
                // Shift the columns over
                for (int j = b; j < m.numCols() - 1; j++)
                {
                    for (int i = 0; i < m.numRows(); i++)
                    {
                        m.set(i, j, m.get(i, j + 1));
                    }
                }

                // Remove the last column.
                m.reshape(m.numRows(), m.numCols() - 1);
            }

            @Override
            public void save(File outputDirectory, String filenameOverride)
            {
                SpecularFitSerializer.serializeBasisFunctions(count, resolution, this, outputDirectory, filenameOverride);
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
