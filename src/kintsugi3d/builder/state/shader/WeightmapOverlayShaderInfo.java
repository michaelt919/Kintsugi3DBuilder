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

package kintsugi3d.builder.state.shader;

import kintsugi3d.builder.fit.decomposition.BasisMaterialInfo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class WeightmapOverlayShaderInfo implements ShaderInfo
{
    private final ShaderInfo baseShader;
    private final BasisMaterialInfo weightmapMaterial;

    public WeightmapOverlayShaderInfo(ShaderInfo baseShader, BasisMaterialInfo weightmapMaterial)
    {
        this.baseShader = baseShader;
        this.weightmapMaterial = weightmapMaterial;
    }

    @Override
    public String getFriendlyName()
    {
        return baseShader.getFriendlyName();
    }

    @Override
    public String getFullName()
    {
        return String.format("%s [%s]", baseShader.getFullName(), weightmapMaterial.getFriendlyName());
    }

    @Override
    public String getFilename()
    {
        return baseShader.getFilename();
    }

    @Override
    public File getFile()
    {
        return baseShader.getFile();
    }

    public ShaderInfo getBaseShader()
    {
        return baseShader;
    }

    public BasisMaterialInfo getWeightmapMaterial()
    {
        return weightmapMaterial;
    }

    @Override
    public Map<String, Optional<Object>> getDefines()
    {
        Map<String, Optional<Object>> defines = new HashMap<>(baseShader.getDefines());
        defines.put("OVERLAY_MODE", Optional.of("OVERLAY_MODE_WEIGHTMAP"));
        defines.put("OVERLAY_WEIGHTMAP_INDEX", Optional.of(weightmapMaterial.getGPUIndex()));
        return defines;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(baseShader.hashCode(), weightmapMaterial.getName());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof WeightmapOverlayShaderInfo)
        {
            WeightmapOverlayShaderInfo other = (WeightmapOverlayShaderInfo) obj;
            return Objects.equals(baseShader, other.baseShader)
                && Objects.equals(weightmapMaterial.getName(), other.weightmapMaterial.getName());
        }
        else
        {
            return false;
        }
    }
}
