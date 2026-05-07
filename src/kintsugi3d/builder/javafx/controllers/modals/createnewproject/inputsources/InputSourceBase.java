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

package kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import kintsugi3d.builder.io.primaryview.ViewSelectionModel;
import kintsugi3d.builder.javafx.controllers.modals.viewselect.ViewSelectableBase;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.javafx.experience.Modal;
import kintsugi3d.builder.resources.project.MissingImagesException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

public abstract class InputSourceBase extends ViewSelectableBase implements InputSource
{
    private final Collection<File> disabledImages = new ArrayList<>(8);

    protected abstract void loadForViewSelectionOrThrow(Consumer<ViewSelectionModel> onLoadComplete) throws Exception;

    @Override
    public void loadForViewSelection(Consumer<ViewSelectionModel> onLoadComplete)
    {
        try
        {
            loadForViewSelectionOrThrow(onLoadComplete);
        }
        catch (MissingImagesException e)
        {
            showMissingImagesAlert(e, () -> loadForViewSelection(onLoadComplete), getModalWindow());
        }
        catch (Exception e)
        {
            ExceptionHandling.error("Error initializing view selection", e);
        }
    }

    private void showMissingImagesAlert(MissingImagesException exception, Runnable reattampt, Window modalWindow)
    {
        Collection<File> missingImgs = exception.getMissingImgs();
        File prevTriedDirectory = exception.getImgDirectory();

        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.OTHER);
        ButtonType newDirectory = new ButtonType("Choose Different Image Directory", ButtonBar.ButtonData.YES);
        ButtonType skipMissingCams = new ButtonType("Disable Missing Cameras", ButtonBar.ButtonData.NO);

        Alert alert = new Alert(Alert.AlertType.NONE,
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

        ((ButtonBase) alert.getDialogPane().lookupButton(cancel)).setOnAction(
            event -> Modal.requestClose(modalWindow));

        ((ButtonBase) alert.getDialogPane().lookupButton(newDirectory)).setOnAction(event ->
        {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(getInitialPhotosDirectory());

            directoryChooser.setTitle("Choose New Image Directory");

            overrideFullResImageDirectory(directoryChooser.showDialog(modalWindow));
            reattampt.run();
        });

        ((ButtonBase) alert.getDialogPane().lookupButton(skipMissingCams)).setOnAction(event ->
        {
            overrideFullResImageDirectory(prevTriedDirectory);
            disabledImages.addAll(exception.getMissingImgs());
            reattampt.run();
        });

        alert.setTitle("Project is Missing Images");
        alert.show();
    }

    @Override
    public Collection<File> getDisabledImages()
    {
        return Collections.unmodifiableCollection(disabledImages);
    }
}
