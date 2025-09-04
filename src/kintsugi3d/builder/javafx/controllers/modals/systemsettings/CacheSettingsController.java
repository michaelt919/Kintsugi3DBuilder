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

package kintsugi3d.builder.javafx.controllers.modals.systemsettings;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.javafx.core.JavaFXState;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class CacheSettingsController implements SystemSettingsControllerBase
{
    @FXML private Label previewImageCacheLabel;
    @FXML private Label specularFitCacheLabel;

    @Override
    public void initializeSettingsPage(Window parentWindow, JavaFXState state)
    {
        previewImageCacheLabel.setText(ApplicationFolders.getPreviewImagesRootDirectory().toString());
        specularFitCacheLabel.setText(ApplicationFolders.getFitCacheRootDirectory().toString());
    }

    @FXML private void openDirectory(MouseEvent e){
        if (!(e.getSource() instanceof Label)) {
            return;
        }

        Label label = (Label) e.getSource();
        File file = new File(label.getText());
        if (!file.exists()){
            ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert alert = new Alert(Alert.AlertType.NONE, "Cache path not found: " + label.getText(), ok);

            alert.setTitle("Cache path not found");
            alert.show();
            return;
        }

        try{
            Desktop.getDesktop().open(file);
        }
        catch(IOException ioe){
            ExceptionHandling.error("Failed to open project directory.", ioe);
        }
    }

    @FXML private void clearCache()
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
                clearPreviewCache(previewCacheDir);
                clearFitCache(fitCacheDir);
            }
        });
    }

    private void clearPreviewCache(File directory)
    {
        assert directory.isDirectory();
        File[] projects = directory.listFiles();
        assert projects != null;

        for (File project : projects)
        {
            assert project.isDirectory();
            File[] resolutions = project.listFiles();
            assert resolutions != null;

            for (File resolution : resolutions)
            {
                assert resolution.isDirectory();
                File[] images = resolution.listFiles();
                assert images != null;

                for (File image : images)
                {
                    // Extra check due to danger of this operation
                    String imgName = image.toString();
                    assert imgName.startsWith(directory.toString());
                    assert imgName.toLowerCase(Locale.ROOT).endsWith(".png");
                    image.delete();
                }

                resolution.delete(); // Will only work if directory is empty.
            }

            project.delete(); // Will only work if directory is empty.
        }
    }

    private void clearFitCache(File directory)
    {
        assert directory.isDirectory();
        File[] projects = directory.listFiles();
        assert projects != null;

        for (File project : projects)
        {
            assert project.isDirectory();
            File[] resolutions = project.listFiles();
            assert resolutions != null;

            for (File resolution : resolutions)
            {
                assert resolution.isDirectory();

                // debug.png
                File debugImg = new File(resolution, "debug.png");
                assert debugImg.toString().startsWith(directory.toString());
                debugImg.delete();

                // sampleLocations.txt
                File sampleLocations = new File(resolution, "sampleLocations.txt");
                assert sampleLocations.toString().startsWith(directory.toString());
                sampleLocations.delete();

                // Everything left should be chunks folders (including the sampled folder)
                File[] chunks = resolution.listFiles();
                assert chunks != null;

                for (File chunk : chunks)
                {
                    assert chunk.isDirectory();
                    File[] images = chunk.listFiles();
                    assert images != null;

                    for (File image : images)
                    {
                        // Extra check due to danger of this operation
                        String imgName = image.toString();
                        assert imgName.startsWith(directory.toString());
                        assert imgName.toLowerCase(Locale.ROOT).endsWith(".png");
                        image.delete();
                    }

                    chunk.delete();
                }

                resolution.delete(); // Will only work if directory is empty.
            }

            project.delete(); // Will only work if directory is empty.
        }
    }

//    // Not using this since it scares me.
//    private void deleteRecursively(File file)
//    {
//        deleteRecursively(file, file);
//    }
//
//    private void deleteRecursively(File original, File current)
//    {
//        if (current.isDirectory())
//        {
//            File[] contents = current.listFiles();
//            if (contents != null)
//            {
//                for (File f : contents)
//                {
//                    deleteRecursively(original, f);
//                }
//            }
//
//            // Extra check due to danger of this operation
//            assert current.toString().startsWith(original.toString());
//            current.delete();
//        }
//        else
//        {
//            // Extra check due to danger of this operation
//            assert current.toString().startsWith(original.toString());
//            current.delete();
//        }
//    }
}
