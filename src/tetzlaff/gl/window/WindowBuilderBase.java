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

package tetzlaff.gl.window;

import tetzlaff.gl.core.Context;

public abstract class WindowBuilderBase<ContextType extends Context<ContextType>> implements WindowBuilder<ContextType>
{
    private final String title;
    private final int width;
    private final int height;
    private int x;
    private int y;
    private boolean resizable = false;
    private int multisamples = 0;

    protected WindowBuilderBase(String title, int width, int height, int x, int y)
    {
        this.title = title;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public int getWidth()
    {
        return width;
    }

    @Override
    public int getHeight()
    {
        return height;
    }

    @Override
    public int getX()
    {
        return x;
    }

    @Override
    public int getY()
    {
        return y;
    }

    @Override
    public boolean isResizable()
    {
        return resizable;
    }

    @Override
    public int getMultisamples()
    {
        return multisamples;
    }

    @Override
    public WindowBuilderBase<ContextType> setX(int x)
    {
        this.x = x;
        return this;
    }

    @Override
    public WindowBuilderBase<ContextType> setY(int y)
    {
        this.y = y;
        return this;
    }

    @Override
    public WindowBuilderBase<ContextType> setResizable(boolean resizable)
    {
        this.resizable = resizable;
        return this;
    }

    @Override
    public WindowBuilderBase<ContextType> setMultisamples(int multisamples)
    {
        this.multisamples = multisamples;
        return this;
    }
}
