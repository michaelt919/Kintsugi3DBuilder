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

import java.util.Objects;

public final class TextureDetails implements Comparable<TextureDetails>
{
    public final String name;
    public final String friendlyName;
    public final String purpose;

    public TextureDetails(String name, String friendlyName, String purpose)
    {
        this.name = name;
        this.friendlyName = friendlyName;
        this.purpose = purpose;
    }

    public TextureDetails(String name)
    {
        this.name = name;
        this.friendlyName = name;
        this.purpose = "";
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof TextureDetails && Objects.equals(this.name, ((TextureDetails) obj).name);
    }

    @Override
    public int hashCode()
    {
        return this.name.hashCode();
    }

    @Override
    public int compareTo(TextureDetails o)
    {
        return this.name.compareTo(o.name);
    }
}
