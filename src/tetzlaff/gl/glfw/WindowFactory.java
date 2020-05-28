/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.glfw;

import javafx.stage.Stage;
import tetzlaff.gl.javafx.CopyWindowBuilder;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLContextFactory;

public final class WindowFactory
{
    private WindowFactory()
    {
    }

    public static WindowBuilderImpl<OpenGLContext> buildOpenGLWindow(String title, int width, int height)
    {
        return new WindowBuilderImpl<>(OpenGLContextFactory.getInstance(), title, width, height);
    }

    public static CopyWindowBuilder<OpenGLContext> buildJavaFXWindow(Stage primaryStage, String title, int width, int height)
    {
        return new CopyWindowBuilder<>(primaryStage,
            f -> new WindowBuilderImpl<>(OpenGLContextFactory.getInstance(), "<ignore>", 1, 1)
                .setDefaultFramebufferCreator(f)
                .create()
                .getContext(),
            title, width, height);
    }
}
