/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.standalone3d;

import javafx.application.Platform;
import javafx.stage.Stage;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.window.FramebufferCanvas;
import kintsugi3d.gl.window.PollableWindow;
import kintsugi3d.gl.window.WindowBuilderBase;

public class Standalone3DWindowBuilder extends WindowBuilderBase<PollableWindow>
{
    private final Stage primaryStage;
    private FramebufferCanvas<? extends Context<?>> canvas;
    private volatile PollableWindow result;

    public Standalone3DWindowBuilder(Stage primaryStage,
        String title, int width, int height)
    {
        // (-1, -1) is the GLFW convention for default window position
        super(title, width, height, -1, -1);

        this.primaryStage = primaryStage;
    }

    public Standalone3DWindowBuilder setCanvas(FramebufferCanvas<? extends Context<?>> canvas)
    {
        this.canvas = canvas;
        return this;
    }

    @Override
    public PollableWindow create()
    {
        Platform.runLater(() -> result = new Standalone3DWindow<>(primaryStage, canvas, this));

        while (result == null)
        {
            Thread.onSpinWait();
        }

        return result;
    }
}
