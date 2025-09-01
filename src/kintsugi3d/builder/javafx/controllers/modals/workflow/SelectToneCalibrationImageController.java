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

package kintsugi3d.builder.javafx.controllers.modals.workflow;

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
    private File selectedImageFile = null;

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

        selectImageFileLabel.setVisible(selectImageFileButton.isSelected());
        buttonGroup.selectedToggleProperty().addListener((a, b, c) ->
            selectImageFileLabel.setVisible(selectImageFileButton.isSelected()));

        this.getCanAdvanceObservable().bind(buttonGroup.selectedToggleProperty().isNotNull());
    }

    @Override
    public void refresh()
    {
        ObservableProjectModel project = getState().getProjectModel();

        boolean hasPreviousColorCheckerImage = project.getColorCheckerFile() != null && project.getColorCheckerFile().exists();
        previousImageButton.setDisable(!hasPreviousColorCheckerImage);

        if (hasPreviousColorCheckerImage)
        {
            buttonGroup.selectToggle(previousImageButton);
        }
    }

    @Override
    public boolean advance()
    {
        File imageFile = null;
        if (buttonGroup.getSelectedToggle() == primaryViewImageButton)
        {
            ViewSet viewSet = Global.state().getIOModel().getLoadedViewSet();
            int primaryViewIndex = viewSet.getPrimaryViewIndex();
            imageFile = viewSet.getFullResImageFile(primaryViewIndex);
        }
        else if (buttonGroup.getSelectedToggle() == selectImageFileButton)
        {
            imageFile = selectedImageFile;
        }

        if (imageFile != null)
        {
            ViewSet viewSet = Global.state().getIOModel().getLoadedViewSet();
            if (viewSet.hasCustomLuminanceEncoding())
            {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Change tone calibration image? This will clear any previous tone calibration values!");
                Optional<ButtonType> confirmResult = alert.showAndWait();
                if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.OK)
                {
                    return false;
                }
            }

            viewSet.clearTonemapping();

            LOG.debug("Setting new color calibration image: {}", imageFile);
            getState().getProjectModel().setColorCheckerFile(imageFile);
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
            ViewSet viewSet = Global.state().getIOModel().getLoadedViewSet();

            imageFileChooser.setInitialDirectory(viewSet.getFullResImageFilePath());
        }
        File temp = imageFileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (temp != null)
        {
            selectedImageFile = temp;
            selectImageFileLabel.setText("Selected: " + temp.getName());
        }
    }
}
