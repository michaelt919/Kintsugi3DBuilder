/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar.systemsettings;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.javafx.InternalModels;

public class CacheSettingsController implements SystemSettingsControllerBase
{
    @FXML private Label previewImageCacheLabel;
    @FXML private Label specularFitCacheLabel;

    @Override
    public void init()
    {
        previewImageCacheLabel.setText(ApplicationFolders.getPreviewImagesRootDirectory().toString());
        specularFitCacheLabel.setText(ApplicationFolders.getFitCacheRootDirectory().toString());
    }

    @Override
    public void bindInfo(InternalModels internalModels)
    {
        //TODO: imp.
    }

    public void clearCache()
    {
        File previewCacheDir = ApplicationFolders.getPreviewImagesRootDirectory().toFile();
        File fitCacheDir = ApplicationFolders.getFitCacheRootDirectory().toFile();

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Clear Cache");
        confirm.setHeaderText("Confirm cache clear?");
        confirm.setContentText("This will permanently remove all files in " + previewCacheDir + " and " + fitCacheDir
            + " and cannot be undone.  Are you sure?");

        confirm.showAndWait().ifPresent(response ->
        {
            if (response == ButtonType.OK)
            {
                deleteRecursively(previewCacheDir);
                deleteRecursively(fitCacheDir);
            }
        });
    }

    private void deleteRecursively(File file)
    {
        deleteRecursively(file, file);
    }

    private void deleteRecursively(File original, File current)
    {
        if (current.isDirectory())
        {
            File[] contents = current.listFiles();
            if (contents != null)
            {
                for (File f : contents)
                {
                    deleteRecursively(original, f);
                }
            }

            // Extra check due to danger of this operation
            assert current.toString().startsWith(original.toString());
            current.delete();
        }
        else
        {
            // Extra check due to danger of this operation
            assert current.toString().startsWith(original.toString());
            current.delete();
        }
    }
}
