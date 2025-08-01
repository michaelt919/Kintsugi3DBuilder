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
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.gl.vecmath.DoubleVector3;

import java.util.Collections;
import java.util.List;

public class SpecularDecompositionFromExistingBasis extends SpecularDecompositionBase
{
    private final List<DoubleVector3> diffuseAlbedos;
    private final MaterialBasis materialBasis;
    private final SpecularBasisSettings settings;

    public SpecularDecompositionFromExistingBasis(TextureResolution textureResolution, SpecularDecomposition existingDecomposition)
    {
        super(textureResolution, existingDecomposition.getMaterialBasis().getMaterialCount());
        this.diffuseAlbedos = existingDecomposition.getDiffuseAlbedos();
        this.materialBasis = existingDecomposition.getMaterialBasis();
        this.settings = existingDecomposition.getSpecularBasisSettings();
    }

    @Override
    public List<DoubleVector3> getDiffuseAlbedos()
    {
        return Collections.unmodifiableList(diffuseAlbedos);
    }

    @Override
    public MaterialBasis getMaterialBasis()
    {
        return materialBasis;
    }

    @Override
    public SpecularBasisSettings getSpecularBasisSettings()
    {
        return settings;
    }
}
