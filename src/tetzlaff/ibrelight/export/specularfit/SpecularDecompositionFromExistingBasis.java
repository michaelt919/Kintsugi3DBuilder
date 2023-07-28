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

import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.export.specularfit.settings.SpecularBasisSettings;

import java.io.File;

public class SpecularDecompositionFromExistingBasis extends SpecularDecompositionBase
{
    private final SpecularDecomposition existingBasis;

    public SpecularDecompositionFromExistingBasis(TextureFitSettings textureFitSettings, SpecularDecomposition existingBasis)
    {
        super(textureFitSettings, existingBasis.getSpecularBasisSettings().getBasisCount());
        this.existingBasis = existingBasis;
    }

    @Override
    public double evaluateRed(int b, int m)
    {
        return existingBasis.evaluateRed(b, m);
    }

    @Override
    public double evaluateGreen(int b, int m)
    {
        return existingBasis.evaluateGreen(b, m);
    }

    @Override
    public double evaluateBlue(int b, int m)
    {
        return existingBasis.evaluateBlue(b, m);
    }

    @Override
    public DoubleVector3 getDiffuseAlbedo(int basisIndex)
    {
        return existingBasis.getDiffuseAlbedo(basisIndex);
    }

    @Override
    public void saveBasisFunctions(File outputDirectory)
    {
        existingBasis.saveBasisFunctions(outputDirectory);
    }

    @Override
    public void saveDiffuseMap(double gamma, File outputDirectory)
    {
        existingBasis.saveDiffuseMap(gamma, outputDirectory);
    }

    @Override
    public SpecularBasisSettings getSpecularBasisSettings()
    {
        return existingBasis.getSpecularBasisSettings();
    }
}
