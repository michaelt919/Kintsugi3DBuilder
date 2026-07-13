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

package kintsugi3d.builder.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public enum StandardTexture
{
    DIFFUSE_COLOR(new TextureDetails("diffuse", "Diffuse map",
        "The RGB color of the surface's Lambertian diffuse reflectance.")),
    SPECULAR_COLOR(new TextureDetails("specular", "Specular map",
            "The RGB color (F0) of the surface's Cook-Torrance / Schlick Fresnel specular reflection.")),
    NORMAL_MAP(new TextureDetails("normal", "Normal map",
        "A texture that encodes geometric detail absent from the mesh, used for lighting and shading.  Deviations from the mesh geometry are represented as colors that deviate from a light blue representing the mesh's surface orientation.")),
    ROUGHNESS(new TextureDetails("roughness", "Roughness map",
        "The GGX microfacet roughness of the surface: Black (0) means mirror-like reflection, white (1) means fully diffuse (albeit with some directionality).")),
    ALBEDO(new TextureDetails("albedo", "Albedo map",
        "The RGB albedo color that combines both diffuse and specular color.  Useful in PBR rendering pipelines that don't support separate diffuse and specular textures.")),
    OCCLUSION(new TextureDetails("occlusion", "Occlusion map",
        "An imported occlusion map that represents how much ambient light can reach the surface based on neighboring geometry: cracks and crevices receive less ambient light than smooth surfaces.")),
    ORM(new TextureDetails("orm", "ORM map",
        "A texture with occlusion, roughness, and metallicity packed into the red, green, and blue channels, respectively.  Occlusion and roughness should be identical to the corresponding standalone textures.  Metallicity is useful in combination with an albedo map for PBR rendering pipelines that don't support separate diffuse and specular textures.  A metallicity of 0 (black) indicates that the albedo color is a diffuse color, while a metallicity of 1 (white) indicates that the albedo is a specular color, with intermediate values distributing the albedo proportionately between diffuse and specular.")),
    ERROR(new TextureDetails("error", "Error map",
        "A texture encoding relative, per-pixel reconstruction error after processing textures.  White pixels have the most error while black pixels have the least error."));

    public final TextureDetails details;

    StandardTexture(TextureDetails details)
    {
        this.details = details;
    }

    public static <TextureType> Map<TextureDetails, TextureType> convertEnumMapToObjectMap(Map<StandardTexture, TextureType> original)
    {
        return original.entrySet().stream().collect(Collectors.toMap(
            entry -> entry.getKey().details, Entry::getValue));
    }

    public static <TextureType> Map<StandardTexture, TextureType> convertObjectMapToEnumMap(Map<TextureDetails, TextureType> original)
    {
        Map<StandardTexture, TextureType> standardTextures = new EnumMap<>(StandardTexture.class);
        for (StandardTexture textureID : values())
        {
            TextureType texture = original.get(textureID.details);
            if (texture != null)
            {
                standardTextures.put(textureID, texture);
            }
        }

        return Collections.unmodifiableMap(standardTextures);
    }
}
