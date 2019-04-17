/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.glfw;

import java.util.function.Function;

import umn.gl.core.DoubleFramebuffer;
import umn.gl.window.PollableWindow;
import umn.gl.window.WindowBuilderBase;

public class WindowBuilderImpl<ContextType extends WindowContextBase<ContextType>>
    extends WindowBuilderBase<ContextType>
{
    private final ContextFactory<ContextType> contextFactory;

    private Function<ContextType, DoubleFramebuffer<ContextType>> createDefaultFramebuffer;

    WindowBuilderImpl(ContextFactory<ContextType> contextFactory, String title, int width, int height)
    {
        // (-1, -1) is the GLFW convention for default window position
        super(title, width, height, -1, -1);

        this.contextFactory = contextFactory;
    }

    WindowBuilderImpl<ContextType> setDefaultFramebufferCreator(Function<ContextType, DoubleFramebuffer<ContextType>> createDefaultFramebuffer)
    {
        this.createDefaultFramebuffer = createDefaultFramebuffer;
        return this;
    }

    @Override
    public PollableWindow<ContextType> create()
    {
        return new WindowImpl<>(contextFactory, createDefaultFramebuffer, this);
    }
}
