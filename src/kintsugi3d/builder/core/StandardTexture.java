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

package kintsugi3d.builder.core;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public enum StandardTexture
{
    DIFFUSE_COLOR("diffuse"),
    SPECULAR_COLOR("specular"),
    NORMAL_MAP("normal"),
    ROUGHNESS("roughness"),
    ALBEDO("albedo"),
    OCCLUSION("occlusion"),
    ORM("orm"),
    ERROR("error");

    public final String texName;

    StandardTexture(String texName)
    {
        this.texName = texName;
    }

    public static <TextureType> Map<String, TextureType> convertEnumMapToStringMap(Map<StandardTexture, TextureType> original)
    {
        return original.entrySet().stream().collect(Collectors.toMap(
            entry -> entry.getKey().texName, Entry::getValue));
    }

    public static <TextureType> Map<StandardTexture, TextureType> convertStringMapToEnumMap(Map<String, TextureType> original)
    {
        Map<StandardTexture, TextureType> standardTextures = new EnumMap<>(StandardTexture.class);
        List<String> standardTextureNames = Arrays.stream(StandardTexture.values())
            .map(t -> t.texName)
            .collect(Collectors.toList());

        for (var entry : original.entrySet())
        {
            if (standardTextureNames.contains(entry.getKey()))
            {
                standardTextures.put(StandardTexture.valueOf(entry.getKey()), entry.getValue());
            }
        }

        return Collections.unmodifiableMap(standardTextures);
    }
}
