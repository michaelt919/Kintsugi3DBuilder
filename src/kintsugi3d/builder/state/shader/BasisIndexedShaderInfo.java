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

import java.util.*;

public class BasisIndexedShaderInfo extends SimpleShaderInfo
{
    private final BasisMaterialInfo material;

    public BasisIndexedShaderInfo(String friendlyName, String shaderFilename, BasisMaterialInfo material)
    {
        super(friendlyName, shaderFilename);
        this.material = material;
    }

    @Override
    public Map<String, Optional<Object>> getDefines()
    {
        Map<String, Optional<Object>> defines = new HashMap<>(super.getDefines());
        defines.put("WEIGHTMAP_INDEX", Optional.of(material.getGPUIndex()));
        return Collections.unmodifiableMap(defines);
    }

    @Override
    public boolean equals(Object obj)
    {
        return super.equals(obj) && obj instanceof BasisIndexedShaderInfo
            && ((BasisIndexedShaderInfo) obj).material.getName().equals(material.getName());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), material.getName());
    }
}
