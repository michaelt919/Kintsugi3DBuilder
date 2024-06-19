/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
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
import java.util.List;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.gl.vecmath.DoubleVector3;

public class SpecularDecompositionFromExistingBasis extends SpecularDecompositionBase
{
    private final List<DoubleVector3> diffuseAlbedos;
    private final SpecularBasis specularBasis;
    private final SpecularBasisSettings settings;

    public SpecularDecompositionFromExistingBasis(TextureResolution textureResolution, SpecularDecomposition existingDecomposition)
    {
        super(textureResolution, existingDecomposition.getSpecularBasis().getCount());
        this.diffuseAlbedos = existingDecomposition.getDiffuseAlbedos();
        this.specularBasis = existingDecomposition.getSpecularBasis();
        this.settings = existingDecomposition.getSpecularBasisSettings();
    }

    @Override
    public List<DoubleVector3> getDiffuseAlbedos()
    {
        return diffuseAlbedos;
    }

    @Override
    public SpecularBasis getSpecularBasis()
    {
        return specularBasis;
    }

    @Override
    public SpecularBasisSettings getSpecularBasisSettings()
    {
        return settings;
    }
}
