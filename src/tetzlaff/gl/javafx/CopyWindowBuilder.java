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

package tetzlaff.gl.javafx;

import java.util.function.Function;

import javafx.application.Platform;
import javafx.stage.Stage;
import tetzlaff.gl.builders.framebuffer.DefaultFramebufferFactory;
import tetzlaff.gl.core.DoubleFramebuffer;
import tetzlaff.gl.core.DoubleFramebufferObject;
import tetzlaff.gl.glfw.WindowContextBase;
import tetzlaff.gl.window.PollableWindow;
import tetzlaff.gl.window.WindowBuilderBase;

public class CopyWindowBuilder<ContextType extends WindowContextBase<ContextType>>
    extends WindowBuilderBase<ContextType>
{
    private final ContextType context;
    private final Stage primaryStage;
    private DoubleFramebufferObject<ContextType> framebuffer;
    private volatile PollableWindow<ContextType> result;

    public CopyWindowBuilder(Stage primaryStage,
        Function<Function<ContextType, DoubleFramebuffer<ContextType>>, ContextType> createContext,
        String title, int width, int height)
    {
        // (-1, -1) is the GLFW convention for default window position
        super(title, width, height, -1, -1);

        this.primaryStage = primaryStage;
        this.context = createContext.apply(c ->
        {
            framebuffer = DefaultFramebufferFactory.create(c, width, height);
            return framebuffer;
        });
    }

    @Override
    public PollableWindow<ContextType> create()
    {
        Platform.runLater(() -> result = new WindowImpl<>(primaryStage, context, framebuffer, this));

        while(result == null)
        {
        }

        return result;
    }
}
