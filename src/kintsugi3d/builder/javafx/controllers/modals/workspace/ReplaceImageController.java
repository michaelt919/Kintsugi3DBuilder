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

package kintsugi3d.builder.javafx.controllers.modals.workspace;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ImageReplaceData;
import kintsugi3d.builder.javafx.controllers.paged.DataReceiverPageControllerBase;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class ReplaceImageController extends DataReceiverPageControllerBase<ImageReplaceData>
{
    @FXML private Pane root;
    @FXML private ImageView currentImageView;
    @FXML private ImageView newImageView;
    @FXML private Label currentPath;
    @FXML private Label newPath;
    @FXML private Button newFileButton;

    private final FileChooser replacementFileChooser = new FileChooser();
    private Image currentImage;

    private static final Logger LOG = LoggerFactory.getLogger(ReplaceImageController.class);
    private ImageReplaceData data;

    @Override
    public Region getRootNode() { return root; }

    @Override
    public void initPage()
    {
        newFileButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("dark-button"), true);

        replacementFileChooser.setTitle("Replace with...");
        replacementFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texture image", "*.png"));
        setCurrentDirectoryFile(Global.state().getIOModel().getLoadedViewSet().getSupportingFilesDirectory());

        setCanConfirm(true);
        setCanAdvance(true);
    }

    @Override
    public void refresh()
    {
    }

    @Override
    public boolean confirm()
    {
        // Texture replacement must happen on graphics thread.
        Rendering.runLater(() ->
        {
            try
            {
                // Replacing texture
                if (data.getKey() != null)
                {
                    // Try to load the texture
                    data.getResources().replaceTextureWithSpecificFile(data.getKey(), data.getNewTexture());

                    // If load was successful, then copy the file into the project files directory.
                    Files.copy(data.getNewTexture().toPath(), data.getCurrentTexture().toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // Finally, attempt to refresh the card (including thumbnail from the version saved to disk).
                    Global.state().getTabModels().getTab("Textures").refreshCards(card ->
                        Objects.equals(card.getTitle(), data.getKey().friendlyName));
                }
                // Replacing weightmap
                else
                {
                    // Try to load the texture
                    data.getResources().getBasisWeightResources()
                        .replaceWeightMapWithSpecificFile(data.getWeightmapIndex(), data.getNewTexture());

                    // If load was successful, then copy the file into the project files directory.
                    Files.copy(data.getNewTexture().toPath(), data.getCurrentTexture().toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // Finally, attempt to refresh the card  (including thumbnail from the version saved to disk).
                    Global.state().getTabModels().getTab("Textures").refreshCards(card ->
                        Objects.equals(card.getInternalName(),
                            TextureResources.getUnpackedWeightMapFilename(data.getWeightmapIndex(), "PNG")));
                }
            }
            catch (IOException | RuntimeException e)
            {
                ExceptionHandling.error("Error replacing texture", e);
            }
        });

        return true;
    }

    private void updateNewTexture()
    {
        if (data != null)
        {
            if (data.getNewTexture() == null)
            {
                newImageView.setImage(currentImage);
                newPath.setText(data.getCurrentTexture().getPath());
            }
            else
            {
                Image newImage = new Image(data.getNewTexture().toURI().toString(), 72, 72, false, false);
                newImageView.setImage(newImage);
                newPath.setText(data.getNewTexture().getPath());
            }
        }
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
    public void receiveData(ImageReplaceData newData)
    {
        this.data = newData;

        currentImage = new Image(data.getCurrentTexture().toURI().toString(), 72, 72, false, false);
        currentImageView.setImage(currentImage);
        currentPath.setText(data.getCurrentTexture().getPath());
        updateNewTexture();
    }

    @FXML
    public void openFileBrowser()
    {
        data.setNewTexture(replacementFileChooser.showOpenDialog(root.getScene().getWindow()));
        updateNewTexture();
    }
}
