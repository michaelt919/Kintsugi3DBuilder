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

package kintsugi3d.builder.state.scene;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UserShader
{
    private final String friendlyName;

    private final String filename;

    private final Map<String, Optional<Object>> defines;

    public UserShader(String friendlyName, String filename)
    {
        this.friendlyName = friendlyName;
        this.filename = filename;
        this.defines = new HashMap<>(0);
    }

    public UserShader(String friendlyName, String filename, Map<String, Optional<Object>> defines)
    {
        this.friendlyName = friendlyName;
        this.filename = filename;
        this.defines = defines;
    }

    public String getFriendlyName()
    {
        return friendlyName;
    }

    public String getFilename()
    {
        return filename;
    }

    public Map<String, Optional<Object>> getDefines()
    {
        return Collections.unmodifiableMap(defines);
    }
}
