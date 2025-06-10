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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.export.specular.SpecularFitSerializer;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.gl.vecmath.DoubleVector3;
import kintsugi3d.gl.vecmath.Vector3;
import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpecularDecompositionFromScratch extends SpecularDecompositionBase
{
    private static final Logger log = LoggerFactory.getLogger(SpecularDecompositionFromScratch.class);

    private final DoubleVector3[] diffuseAlbedos;
    private final SimpleMatrix specularRed;
    private final SimpleMatrix specularGreen;
    private final SimpleMatrix specularBlue;

    private final SpecularBasisSettings specularBasisSettings;

    public SpecularDecompositionFromScratch(TextureResolution textureResolution, SpecularBasisSettings specularBasisSettings)
    {
        super(textureResolution, specularBasisSettings.getBasisCount());
        this.specularBasisSettings = specularBasisSettings;

        diffuseAlbedos = new DoubleVector3[this.specularBasisSettings.getBasisCount()];

        for (int i = 0; i < this.specularBasisSettings.getBasisCount(); i++)
        {
            diffuseAlbedos[i] = DoubleVector3.ZERO;
        }

        specularRed = new SimpleMatrix(
            this.specularBasisSettings.getBasisResolution() + 1,
            this.specularBasisSettings.getBasisCount(), DMatrixRMaj.class);
        specularGreen = new SimpleMatrix(
            this.specularBasisSettings.getBasisResolution() + 1,
            this.specularBasisSettings.getBasisCount(), DMatrixRMaj.class);
        specularBlue = new SimpleMatrix(
            this.specularBasisSettings.getBasisResolution() + 1,
            this.specularBasisSettings.getBasisCount(), DMatrixRMaj.class);
    }

    @Override
    public List<DoubleVector3> getDiffuseAlbedos()
    {
        return Collections.unmodifiableList(Arrays.asList(diffuseAlbedos));
    }

    @Override
    public MaterialBasis getMaterialBasis()
    {
        return new MaterialBasis()
        {
            final int count = specularBasisSettings.getBasisCount();
            final int resolution = specularBasisSettings.getBasisResolution();

            @Override
            public DoubleVector3 getDiffuseColor(int b)
            {
                return diffuseAlbedos[b];
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
            public void save(File outputDirectory)
            {
                SpecularFitSerializer.serializeBasisFunctions(count, resolution, this, outputDirectory);
            }
        };
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

    @Override
    public SpecularBasisSettings getSpecularBasisSettings()
    {
        return specularBasisSettings;
    }
}
