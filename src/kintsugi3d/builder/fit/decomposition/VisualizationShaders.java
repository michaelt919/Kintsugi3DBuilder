/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit.decomposition;

import kintsugi3d.builder.state.scene.UserShader;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class VisualizationShaders
{
    public static final String WEIGHT_MAP_GRAYSCALE = "rendermodes/weightmaps/weightmapSingle.frag";
    public static final String WEIGHT_MAP_SUPERIMPOSED = "rendermodes/weightmaps/weightmapOverlay.frag";
    public static final String BASIS_MATERIAL = "rendermodes/basisMaterialSingle.frag";
    public static final String BASIS_MATERIAL_WEIGHTED = "rendermodes/basisMaterialWeightedSingle.frag";

    public static UserShader getForBasisMaterial(String filename, int materialIndex)
    {
        Map<String, Optional<Object>> defines = new HashMap<>(1);
        defines.put("WEIGHTMAP_INDEX", Optional.of(materialIndex));
        return new UserShader(String.format("Palette material %d", materialIndex), filename, defines);
    }
}
