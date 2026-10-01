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

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SimpleShaderInfo implements ShaderInfo
{
    private static final String SHADER_DIR = "shaders";

    private final String friendlyName;
    private final String filename;

    public SimpleShaderInfo(String friendlyName, String shaderFilename)
    {
        this.friendlyName = friendlyName;
        this.filename = shaderFilename;
    }

    @Override
    public String getFriendlyName()
    {
        return friendlyName;
    }

    @Override
    public String getFullName()
    {
        return friendlyName;
    }

    @Override
    public String getFilename()
    {
        return filename;
    }

    @Override
    public File getFile()
    {
        return new File(SHADER_DIR, filename);
    }

    @Override
    public Map<String, Optional<Object>> getDefines()
    {
        return Map.of();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof SimpleShaderInfo && Objects.equals(this.filename, ((SimpleShaderInfo) obj).filename);
    }

    @Override
    public int hashCode()
    {
        return filename.hashCode();
    }
}
