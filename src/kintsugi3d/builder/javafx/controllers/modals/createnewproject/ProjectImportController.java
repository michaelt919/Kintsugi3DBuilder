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

package kintsugi3d.builder.javafx.controllers.modals.createnewproject;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.NonValidatedInputSource;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.ValidatedInputSource;
import kintsugi3d.builder.javafx.controllers.paged.DataSourcePageControllerBase;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.javafx.experience.Modal;
import kintsugi3d.builder.resources.project.MissingImagesException;

import java.io.File;
import java.util.Collection;

public abstract class ProjectImportController extends DataSourcePageControllerBase<ValidatedInputSource>
{
    protected abstract NonValidatedInputSource getData();

    private void showMissingImagesAlert(MissingImagesException exception, NonValidatedInputSource data)
    {
        Collection<File> missingImgs = exception.getMissingImgs();

        ButtonType cancel = new ButtonType("Cancel", ButtonData.OTHER);
        ButtonType newDirectory = new ButtonType("Choose Different Image Directory", ButtonData.YES);
        ButtonType skipMissingCams = new ButtonType("Disable Missing Cameras", ButtonData.NO);

        Alert alert = new Alert(AlertType.NONE,
            String.format("Imported object is missing %d images.", missingImgs.size()),
            cancel, newDirectory, skipMissingCams/*, openDirectory*/);

        // Force the window back to the correct size in case of race conditions with the OS (esp. on Linux)
        ChangeListener<? super Number> forceSize =
            (obs, oldValue, newValue) ->
                Platform.runLater(() ->
                {
                    alert.getDialogPane().autosize();
                    alert.getDialogPane().getScene().getWindow().sizeToScene();
                });
        alert.getDialogPane().widthProperty().addListener(forceSize);
        alert.getDialogPane().heightProperty().addListener(forceSize);

        Window modalWindow = getRootNode().getScene().getWindow();

        ((ButtonBase) alert.getDialogPane().lookupButton(cancel)).setOnAction(
            event -> Modal.requestClose(modalWindow));

        ((ButtonBase) alert.getDialogPane().lookupButton(newDirectory)).setOnAction(event ->
        {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(data.getInitialPhotosDirectory());

            directoryChooser.setTitle("Choose New Image Directory");
            data.overrideFullResImageDirectory(directoryChooser.showDialog(modalWindow));
        });

        ((ButtonBase) alert.getDialogPane().lookupButton(skipMissingCams)).setOnAction(event ->
            data.disableImages(exception.getMissingImgs()));

        alert.setTitle("Project is Missing Images");
        alert.show();
    }

    private static void handleGenericImportException(Exception e)
    {
        ExceptionHandling.error("Error importing images", e);
    }

    @Override
    public boolean advance()
    {
        NonValidatedInputSource data = getData();

        try
        {
            getPage().setOutData(getData().validate());
            return true;
        }
        catch (MissingImagesException e)
        {
            showMissingImagesAlert(e, data);
            return false;
        }
        catch (Exception e)
        {
            handleGenericImportException(e);
            return false;
        }
    }

    @Override
    public boolean confirm()
    {
        getPage().getOutData().confirm();
        return true;
    }
}
