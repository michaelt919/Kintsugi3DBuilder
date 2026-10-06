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

import kintsugi3d.builder.core.texture.TextureInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class TextureVisualizationShaderInfo extends SimpleShaderInfo
{
    private final TextureInfo texture;

    public TextureVisualizationShaderInfo(TextureInfo texture, String shaderFilename)
    {
        super(texture.friendlyName, shaderFilename);
        this.texture = texture;
    }

    @Override
    public Map<String, Optional<Object>> getDefines()
    {
        Map<String, Optional<Object>> defines = new HashMap<>(super.getDefines());
        defines.put("VIEW_TEX", Optional.of(String.format("tex_%s", texture.name)));
        return defines;
    }

    @Override
    public boolean equals(Object obj)
    {
        return super.equals(obj) && obj instanceof TextureVisualizationShaderInfo &&
            ((TextureVisualizationShaderInfo)obj).texture.name.equals(texture.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), texture.name);
    }
}
