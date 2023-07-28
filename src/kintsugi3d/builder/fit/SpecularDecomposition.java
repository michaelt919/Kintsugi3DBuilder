/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.fit;

import org.ejml.simple.SimpleMatrix;
import kintsugi3d.gl.vecmath.DoubleVector3;
import kintsugi3d.builder.core.TextureFitSettings;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;

import java.io.File;
import java.util.List;

public interface SpecularDecomposition extends SpecularBasis, SpecularBasisWeights
{
    TextureFitSettings getTextureFitSettings();

    SpecularBasisSettings getSpecularBasisSettings();

    DoubleVector3 getDiffuseAlbedo(int basisIndex);

    SimpleMatrix getWeights(int texelIndex);

    void setWeights(int texelIndex, SimpleMatrix weights);

    List<SimpleMatrix> getWeightsList();

    void invalidateWeights();

    void fillHoles();

    void setWeightsValidity(int texelIndex, boolean validity);

    void saveBasisFunctions(File outputDirectory);

    void saveWeightMaps(File outputDirectory);

    void saveDiffuseMap(double gamma, File outputDirectory);
}
