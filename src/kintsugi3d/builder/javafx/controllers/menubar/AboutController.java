/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class AboutController {

    private static final Logger log = LoggerFactory.getLogger(AboutController.class);
    @FXML private ScrollPane scrollPane;
    @FXML private Rectangle backgroundRectangle;
    @FXML private Text aboutText;

    public void init(){
        try
        {
            //TODO: text appears a little blurry, also needs some beautification
            List<String> lines = Files.readAllLines(new File("kintsugi3d-builder-about.txt").toPath());
            String contentText = String.join(System.lineSeparator(), lines);

            aboutText.setText(contentText);
            aboutText.setWrappingWidth(scrollPane.getWidth() - 20);
            backgroundRectangle.setWidth(scrollPane.getWidth());
            backgroundRectangle.setHeight(aboutText.getLayoutBounds().getHeight() + 20); // +20 to account for margins

        }
        catch (IOException e)
        {
            log.error("An error occurred showing help and about:", e);
        }
    }

}
