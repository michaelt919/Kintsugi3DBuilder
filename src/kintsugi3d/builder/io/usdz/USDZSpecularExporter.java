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

package kintsugi3d.builder.io.usdz;

import de.javagl.jgltf.impl.v2.TextureInfo;
import kintsugi3d.builder.core.texture.StandardTexture;
import kintsugi3d.builder.io.gltf.StandardTextureExport;

import java.io.File;
import java.util.List;

public class USDZSpecularExporter extends USDZExporter
{
    @StandardTextureExport(StandardTexture.DIFFUSE_COLOR)
    public void diffuse(TextureInfo diffuse)
    {
    }

    @StandardTextureExport(StandardTexture.SPECULAR_COLOR)
    public void specular(TextureInfo specular)
    {
    }

    @Override
    protected void addCommands(List<String> commandList)
    {
        commandList.add("--diffuse");
        commandList.add(new File(getTempPath(), getTextureFilename(StandardTexture.DIFFUSE_COLOR.details.name, getTextureFileFormat())).getPath());
        commandList.add("--specular");
        commandList.add(new File(getTempPath(), getTextureFilename(StandardTexture.SPECULAR_COLOR.details.name, getTextureFileFormat())).getPath());
    }
}