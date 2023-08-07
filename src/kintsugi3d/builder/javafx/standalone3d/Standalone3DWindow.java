/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.javafx.standalone3d;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.javafx.FramebufferView;
import kintsugi3d.gl.window.FramebufferCanvas;
import kintsugi3d.gl.window.PollableCanvas3D;
import kintsugi3d.gl.window.PollableWindow;
import kintsugi3d.gl.window.WindowSpecification;

public class Standalone3DWindow<ContextType extends Context<ContextType>> implements PollableWindow
{
    private final Stage stage;
    private final FramebufferView framebufferView;
    private final FramebufferCanvas<ContextType> canvas;
    private boolean windowClosing = false;
    private boolean focused;

    Standalone3DWindow(Stage primaryStage, FramebufferCanvas<ContextType> canvas, WindowSpecification windowSpec)
    {
        this.canvas = canvas;

        framebufferView = new FramebufferView();
        framebufferView.setPrefWidth(windowSpec.getWidth());
        framebufferView.setPrefHeight(windowSpec.getHeight());

        stage = new Stage();
        stage.initOwner(primaryStage);
        stage.setTitle(windowSpec.getTitle());
        stage.setResizable(windowSpec.isResizable());

        if (windowSpec.getX() >= 0)
        {
            stage.setX(windowSpec.getX());
        }

        if (windowSpec.getY() >= 0)
        {
            stage.setY(windowSpec.getY());
        }

        Scene scene = new Scene(framebufferView);
        stage.setScene(scene);
        framebufferView.registerKeyAndWindowEventsFromStage(stage);

        framebufferView.setCanvas(canvas);

        focused = stage.isFocused();
        stage.focusedProperty().addListener((event, oldValue, newValue) -> focused = stage.isFocused());
    }

    @Override
    public void show()
    {
        Platform.runLater(stage::show);
    }

    @Override
    public void hide()
    {
        Platform.runLater(stage::hide);
    }

    @Override
    public void focus()
    {
        Platform.runLater(stage::requestFocus);
    }

    @Override
    public boolean isWindowClosing()
    {
        return windowClosing;
    }

    @Override
    public void requestWindowClose()
    {
        windowClosing = true;
        canvas.requestTerminate();
    }

    @Override
    public void cancelWindowClose()
    {
        windowClosing = false;
        canvas.cancelTerminate();
    }

    @Override
    public void close()
    {
        Platform.runLater(stage::close);
    }

    @Override
    public void setWindowTitle(String title)
    {
        Platform.runLater(() -> stage.setTitle(title));
    }

    @Override
    public void setSize(int width, int height)
    {
        Platform.runLater(() ->
        {
            framebufferView.setPrefWidth(width);
            framebufferView.setPrefHeight(height);
            stage.sizeToScene();
        });
    }

    @Override
    public void setPosition(int x, int y)
    {
        Platform.runLater(() ->
        {
            stage.setX(x);
            stage.setY(y);
        });
    }

    @Override
    public boolean isFocused()
    {
        return focused;
    }

    @Override
    public PollableCanvas3D<? extends Context<?>> getCanvas()
    {
        return framebufferView.getCanvas();
    }
}
