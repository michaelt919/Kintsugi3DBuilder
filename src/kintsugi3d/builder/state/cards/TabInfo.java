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

package kintsugi3d.builder.state.cards;

public class TabInfo
{
    private final String label;
    private final ProjectDataCardFactory factory;
    private final String path;

    public TabInfo(String label, ProjectDataCardFactory factory, String path)
    {
        this.label = label;
        this.factory = factory;
        this.path = path;
    }

    public String getLabel()
    {
        return label;
    }

    public ProjectDataCardFactory getFactory()
    {
        return factory;
    }

    public String getPath()
    {
        return path;
    }
}
