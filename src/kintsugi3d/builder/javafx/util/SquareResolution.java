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

package kintsugi3d.builder.javafx.util;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Used by UI elements to display resolution in the form DxD where D is both the width and height.
 */
public class SquareResolution
{
    private final IntegerProperty size = new SimpleIntegerProperty(2048);

    public SquareResolution()
    {
    }

    public SquareResolution(int size)
    {
        this.size.set(size);
    }

    public SquareResolution(Number size)
    {
        this.size.set(size.intValue());
    }

    public int getSize()
    {
        return size.get();
    }

    public void setSize(int size)
    {
        this.size.set(size);
    }

    public IntegerProperty sizeProperty()
    {
        return size;
    }

    @Override
    public int hashCode()
    {
        return Integer.hashCode(getSize());
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof SquareResolution && this.getSize() == ((SquareResolution)other).getSize();
    }

    @Override
    public String toString()
    {
        return String.format("%dx%d", getSize(), getSize());
    }
}
