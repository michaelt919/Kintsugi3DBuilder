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

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.builder.javafx.core.MainWindowController;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

/**
 * Creates a modal window.
 * Only one modal window managed by a single instance of this class can be open at a time.
 */
public class Modal
{
    private final Window parentWindow;
    private Stage stage;

    private final BooleanProperty openProperty = new SimpleBooleanProperty(false);

    public Modal(Window parentWindow)
    {
        this.parentWindow = parentWindow;
    }

    public BooleanExpression getOpenProperty()
    {
        return openProperty;
    }

    public boolean isOpen()
    {
        return openProperty.get();
    }

    public Stage getStage()
    {
        return stage;
    }

    /**
     * Opens a modal window and returns the associated controller.
     * Returns null if window is already open or if an error occurred.
     * @param title
     * @param urlString
     * @return
     * @param <ControllerType>
     * @throws IOException
     */
    public <ControllerType> ControllerType create(String title, String urlString) throws IOException
    {
        return create(title, getFXMLLoader(urlString));
    }

    public <ControllerType> ControllerType create(String title, FXMLLoader fxmlLoader) throws IOException
    {
        Parent root = fxmlLoader.load();

        this.stage = new Stage();
        stage.getIcons().add(MainApplication.getIcon());
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.initOwner(parentWindow);
        stage.setResizable(false);
        openProperty.bind(stage.showingProperty());
        MainApplication.initAccelerators(stage.getScene());

        return fxmlLoader.getController();
    }

    public void open()
    {
        stage.show();
    }

    public void requestClose()
    {
        requestClose(stage);
    }

    public static void requestClose(Node node)
    {
        requestClose(node.getScene().getWindow());
    }

    public static void requestClose(Window window)
    {
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private static FXMLLoader getFXMLLoader(String urlString) throws FileNotFoundException
    {
        URL url = MainWindowController.class.getResource(urlString);
        if (url == null)
        {
            throw new FileNotFoundException(urlString);
        }
        return new FXMLLoader(url);
    }
}
