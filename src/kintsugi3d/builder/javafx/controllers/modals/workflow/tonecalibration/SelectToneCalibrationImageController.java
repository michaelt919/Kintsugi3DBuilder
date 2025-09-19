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

package kintsugi3d.builder.javafx.controllers.modals.workflow.tonecalibration;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.controllers.paged.NonDataPageControllerBase;
import kintsugi3d.builder.javafx.internal.ObservableProjectModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Optional;

public class SelectToneCalibrationImageController extends NonDataPageControllerBase
{
    private static final Logger LOG = LoggerFactory.getLogger(SelectToneCalibrationImageController.class);

    @FXML private Pane rootPane;

    @FXML private ToggleButton primaryViewImageButton;
    @FXML private ToggleButton previousImageButton;
    @FXML private ToggleButton selectImageFileButton;
    @FXML private Label selectImageFileLabel;

    private final FileChooser imageFileChooser;
    private final ObjectProperty<File> selectedImageFile = new SimpleObjectProperty<>(null);

    private final ToggleGroup buttonGroup = new ToggleGroup();

    public SelectToneCalibrationImageController()
    {
        imageFileChooser = new FileChooser();
        imageFileChooser.setTitle("Select tone calibration image");
    }

    @Override
    public Region getRootNode()
    {
        return rootPane;
    }

    @Override
    public void initPage()
    {
        buttonGroup.getToggles().add(primaryViewImageButton);
        buttonGroup.getToggles().add(previousImageButton);
        buttonGroup.getToggles().add(selectImageFileButton);

        buttonGroup.selectToggle(primaryViewImageButton);

        selectImageFileButton.setOnAction(this::selectImageFileAction);
        previousImageButton.setOnAction(e ->
        {
            if (hasPreviousColorCheckerImage())
            {
                selectedImageFile.set(getState().getProjectModel().getColorCheckerFile());
                refreshImageLabel();
            }
            else
            {
                selectedImageFile.set(null);
            }
        });

        selectImageFileLabel.visibleProperty().bind(selectImageFileButton.selectedProperty()
            .or(previousImageButton.selectedProperty().and(selectedImageFile.isNotNull())));
        buttonGroup.selectedToggleProperty().addListener((a, b, c) ->
            selectImageFileLabel.setVisible(selectImageFileButton.isSelected()));

        this.getCanAdvanceObservable().bind(buttonGroup.selectedToggleProperty().isNotNull());
    }

    @Override
    public void refresh()
    {
        if (hasPreviousColorCheckerImage())
        {
            previousImageButton.setText("Use previously chosen image");
            selectedImageFile.set(getState().getProjectModel().getColorCheckerFile());
            refreshImageLabel();
        }
        else
        {
            previousImageButton.setText("Skip (no tone curve)");
        }

        if (hasPreviousColorCheckerImage() || Global.state().getIOModel().getLoadedViewSet().hasCustomLuminanceEncoding())
        {
            // Default unless the user has never selected a calibration image nor saved a luminance encoding.
            buttonGroup.selectToggle(previousImageButton);
        }
    }

    private boolean hasPreviousColorCheckerImage()
    {
        ObservableProjectModel project = getState().getProjectModel();
        return project.getColorCheckerFile() != null && project.getColorCheckerFile().exists();
    }

    @Override
    public boolean advance()
    {
        ViewSet viewSet = Global.state().getIOModel().validateHandler().getLoadedViewSet();

        File imageFile = null;
        if (buttonGroup.getSelectedToggle() == primaryViewImageButton)
        {
            int primaryViewIndex = viewSet.getPrimaryViewIndex();
            try
            {
                imageFile = viewSet.findFullResImageFile(primaryViewIndex);
            }
            catch (FileNotFoundException e)
            {
                error("File not found", String.format("Could not find file: %s", viewSet.getFullResImageFile(primaryViewIndex)));
                return false;
            }
        }
        else if (buttonGroup.getSelectedToggle() == selectImageFileButton)
        {
            imageFile = selectedImageFile.get();
        }

        if (imageFile != null)
        {
            if (viewSet.hasCustomLuminanceEncoding())
            {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Change tone calibration image? This will clear any previous tone calibration values!");
                Optional<ButtonType> confirmResult = alert.showAndWait();
                if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.OK)
                {
                    return false;
                }
            }

            viewSet.clearLuminanceEncoding();

            LOG.debug("Setting new color calibration image: {}", imageFile);
            getState().getProjectModel().setColorCheckerFile(imageFile);
        }

        // Skip is selected if and only if there is no previous image (i.e. button is actually labelled "Skip")
        // and the button itself is selected.
        // Thus is there is a previous image or the button is not selected, some image has been selected.
        if ((buttonGroup.getSelectedToggle() != previousImageButton || hasPreviousColorCheckerImage()) && !viewSet.hasCustomLuminanceEncoding())
        {
            // Skip was not selected and view set doesn't currently have a luminance encoding:
            // Give it a "dummy" encoding so that the checkbox is selected by default on the next page.
            viewSet.setLuminanceEncoding(new double[] { 1.0 }, new byte[] { (byte)0xFF });
        }

        return true;
    }

    private void selectImageFileAction(ActionEvent event)
    {
        // Don't show file chooser when deselecting
        if (!selectImageFileButton.isSelected())
        {
            return;
        }

        ObservableProjectModel project = getState().getProjectModel();
        File colorCheckerFile = project.getColorCheckerFile();
        if (colorCheckerFile != null && colorCheckerFile.exists())
        {
            imageFileChooser.setInitialDirectory(colorCheckerFile.getParentFile());
        }
        else
        {
            ViewSet viewSet = Global.state().getIOModel().validateHandler().getLoadedViewSet();
            imageFileChooser.setInitialDirectory(viewSet.getFullResImageFilePath());
        }
        File temp = imageFileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (temp != null)
        {
            selectedImageFile.set(temp);
            refreshImageLabel();
        }
    }

    private void refreshImageLabel()
    {
        selectImageFileLabel.setText("Selected: " + selectedImageFile.get().getName());
    }
}
