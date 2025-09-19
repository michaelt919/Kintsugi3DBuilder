/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.javafx.core.MainWindowController;
import kintsugi3d.util.Flag;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.function.Function;

public class WindowUtilities
{
    private WindowUtilities()
    {
    }

    public static <ControllerType> ControllerType makeWindow(Window parentWindow, String title, Flag flag,
        Function<Parent, Scene> sceneFactory, String urlString) throws IOException
    {
        URL url = MainWindowController.class.getClassLoader().getResource(urlString);
        if (url == null)
        {
            throw new FileNotFoundException(urlString);
        }
        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent root = fxmlLoader.load();
        Stage stage = new Stage();
        stage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        stage.setTitle(title);
        stage.setScene(sceneFactory.apply(root));
        stage.initOwner(parentWindow);

        stage.setResizable(false);

        flag.set(true);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, param -> flag.set(false));

        stage.show();

        return fxmlLoader.getController();
    }

    public static <ControllerType> ControllerType makeWindow(Window parentWindow, String title, Flag flag, String urlString) throws IOException
    {
        return makeWindow(parentWindow, title, flag, Scene::new, urlString);
    }
}
