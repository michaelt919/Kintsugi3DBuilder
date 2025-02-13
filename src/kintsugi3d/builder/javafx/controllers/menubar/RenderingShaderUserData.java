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

package kintsugi3d.builder.javafx.controllers.menubar;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RenderingShaderUserData
{
    private final String shaderName;

    private final Map<String, Optional<Object>> shaderDefines;

    public RenderingShaderUserData(String shaderName)
    {
        this.shaderName = shaderName;
        this.shaderDefines = new HashMap<>(0);
    }

    public RenderingShaderUserData(String shaderName, Map<String, Optional<Object>> shaderDefines)
    {
        this.shaderName = shaderName;
        this.shaderDefines = shaderDefines;
    }

    public String getShaderName()
    {
        return shaderName;
    }

    public Map<String, Optional<Object>> getShaderDefines()
    {
        return shaderDefines;
    }
}
