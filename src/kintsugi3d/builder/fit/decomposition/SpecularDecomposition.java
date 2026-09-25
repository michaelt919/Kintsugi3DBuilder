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
import org.ejml.simple.SimpleMatrix;

import java.io.File;
import java.util.List;

public interface SpecularDecomposition
{
    TextureResolution getTextureResolution();

    MaterialBasis getMaterialBasis();
    SpecularBasisWeights getWeights();

    boolean areWeightsValid(int texelIndex);
    void setWeightsValidity(int texelIndex, boolean validity);
    void invalidateWeights();

    double getWeight(int b, int p);
    SimpleMatrix getWeights(int texelIndex);
    List<SimpleMatrix> getWeightsList();
    void setWeights(int texelIndex, SimpleMatrix weights);
    void fillHoles();

    void saveBasisFunctions(File outputDirectory);
    void saveWeightMaps(File outputDirectory);
    void saveDiffuseMap(File outputDirectory);
}
