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

import java.io.File;
import java.util.Collection;
import java.util.List;

public interface MaterialBasis
{
    Collection<? extends BasisMaterialInfo> getMaterials();

    List<? extends BasisMaterialInfo> getIndexableMaterialList();

    BasisMaterialInfo getMaterial(String materialName);

    int getMaterialCount();

    int getEnabledMaterialCount();

    int getSpecularResolution();

    void save(File outputDirectory, String filenameOverride);

    default void save(File outputDirectory)
    {
        save(outputDirectory, null);
    }

    MutableMaterialBasis copy();
}
