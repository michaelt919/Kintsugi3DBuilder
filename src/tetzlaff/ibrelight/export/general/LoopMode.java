/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.general;

public enum LoopMode
{
    SINGLE_FRAME("Render once"),
    MULTIFRAME("Render a certain number of frames"),
    MULTIVIEW("Render for each view in the model's view set"),
    MULTIVIEW_RETARGET("Render for each view in another target view set");

    private final String description;

    LoopMode(String description)
    {
        this.description = description;
    }

    @Override
    public String toString()
    {
        return description;
    }
}
