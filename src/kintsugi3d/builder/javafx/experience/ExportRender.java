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

package kintsugi3d.builder.javafx.experience;

import java.io.IOException;

public class ExportRender extends ExperienceBase
{
    private final String fxmlURLString;
    private final String shortName;

    public ExportRender(String fxmlURLString, String shortName)
    {
        this.fxmlURLString = fxmlURLString;
        this.shortName = shortName;
    }

    public String getShortName()
    {
        return this.shortName;
    }

    @Override
    public String getName()
    {
        return String.format("Export: %s", shortName);
    }

    @Override
    protected void open() throws IOException
    {
        openModal(fxmlURLString);
    }
}
