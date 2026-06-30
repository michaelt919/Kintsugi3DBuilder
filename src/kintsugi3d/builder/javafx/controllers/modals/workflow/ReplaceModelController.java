/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.modals.workflow;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import kintsugi3d.builder.javafx.controllers.paged.DataReceiverPageControllerBase;

import java.io.File;

public class ReplaceModelController extends DataReceiverPageControllerBase<ReplaceData>
{
    @FXML private Pane root;
    @FXML private ImageView currentImageView;
    @FXML private ImageView newImageView;
    @FXML private Label currentPath;
    @FXML private Label newPath;
    @FXML private Button currentFileButton;
    @FXML private Button newFileButton;

    private final FileChooser replacementFileChooser = new FileChooser();
    private Image currentImage;

    private ReplaceData data;

    @Override
    public Region getRootNode() { return root; }

    @Override
    public void initPage()
    {
        replacementFileChooser.setTitle("Replace with...");
        replacementFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texture image", "*.png"));
//        setCurrentDirectoryFile();

    }

    @Override
    public void refresh()
    {

    }

    public void setExistingImage(File currentTexture)
    {
        File diffuse = currentTexture;
        currentImage = new Image(currentTexture.getPath());

    }

    private void setCurrentDirectoryFile(File currentDirectoryFile)
    {
        // Sets FileChooser defaults
        if (currentDirectoryFile != null)
        {
            replacementFileChooser.setInitialDirectory(currentDirectoryFile);
            replacementFileChooser.setInitialFileName(currentDirectoryFile.getName());
        }
    }

    @Override
    public void receiveData(ReplaceData newData)
    {
        this.data = newData;
        if (data != null)
        {
            currentImage = new Image(data.getCurrentTexture().toURI().toString(), 72, 72, false, false);
            currentImageView.setImage(currentImage);
            currentPath.setText(data.getCurrentTexture().getPath());
            setCurrentDirectoryFile(data.getCurrentTexture());
            if (data.getNewTexture() == null)
            {
                newImageView.setImage(currentImage);
                newPath.setText(data.getCurrentTexture().getPath());
            }
        }
    }
}
