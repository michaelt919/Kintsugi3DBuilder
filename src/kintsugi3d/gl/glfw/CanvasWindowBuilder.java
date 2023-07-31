/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.gl.glfw;

import java.util.function.Function;

import kintsugi3d.gl.core.DoubleFramebuffer;
import kintsugi3d.gl.window.WindowBuilderBase;

public class CanvasWindowBuilder<ContextType extends WindowContextBase<ContextType>>
    extends WindowBuilderBase<CanvasWindow<ContextType>>
{
    private final ContextFactory<ContextType> contextFactory;

    private Function<ContextType, DoubleFramebuffer<ContextType>> createDefaultFramebuffer;

    CanvasWindowBuilder(ContextFactory<ContextType> contextFactory, String title, int width, int height)
    {
        // (-1, -1) is the GLFW convention for default window position
        super(title, width, height, -1, -1);

        this.contextFactory = contextFactory;
    }

    public CanvasWindowBuilder<ContextType> setDefaultFramebufferCreator(Function<ContextType, DoubleFramebuffer<ContextType>> createDefaultFramebuffer)
    {
        this.createDefaultFramebuffer = createDefaultFramebuffer;
        return this;
    }

    @Override
    public CanvasWindow<ContextType> create()
    {
        return new CanvasWindow<>(contextFactory, createDefaultFramebuffer, this);
    }
}
