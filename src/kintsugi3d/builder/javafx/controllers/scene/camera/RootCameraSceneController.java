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

package kintsugi3d.builder.javafx.controllers.scene.camera;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.builder.javafx.internal.ObservableProjectModel;
import kintsugi3d.builder.javafx.internal.CameraModelImpl;

public class RootCameraSceneController
{
    private static final Logger log = LoggerFactory.getLogger(RootCameraSceneController.class);
    @FXML
    private VBox settings;
    @FXML
    private SettingsCameraSceneController settingsController;
    @FXML
    private ListView<CameraSetting> cameraListView;
    @FXML
    private VBox listControls;
    @FXML
    private Button theRenameButton;

    private ObservableProjectModel projectModel;

    public void init(CameraModelImpl cameraModel, ObservableProjectModel injectedProjectModel)
    {
        this.projectModel = injectedProjectModel;

        cameraListView.setItems(projectModel.getCameraList());
        cameraListView.getSelectionModel().selectedItemProperty().addListener(settingsController.changeListener);

        CameraSetting freeCam = new CameraSetting();
        freeCam.setName("Free Camera");

        ObservableList<CameraSetting> cameraList = projectModel.getCameraList();

        cameraList.add(freeCam);
        cameraListView.getSelectionModel().select(freeCam);

        cameraList.addListener((ListChangeListener<? super CameraSetting>) change ->
        {
            change.next();
            if (change.wasAdded() && change.getAddedSize() == cameraList.size())
            {
                cameraListView.getSelectionModel().select(0);
            }
        });

        cameraModel.setSelectedCameraSetting(cameraListView.getSelectionModel().selectedItemProperty());
    }

    private SelectionModel<CameraSetting> getCameraSelectionModel()
    {
        return cameraListView.getSelectionModel();
    }

    @FXML
    private void newCameraButton()
    {
        projectModel.getCameraList().add(getCameraSelectionModel().getSelectedItem().duplicate());
        getCameraSelectionModel().select(projectModel.getCameraList().size() - 1);
    }

    @FXML
    private void saveCameraButton()
    {
        log.debug("TODO: saved " + getCameraSelectionModel().getSelectedItem() + " to the library.");
    }

    @FXML
    private void renameCameraButton()
    {
        if (getCameraSelectionModel().getSelectedIndex() == 0)
        {
            return;
        }

        EventHandler<ActionEvent> oldOnAction = theRenameButton.getOnAction();//backup the old on action event for the rename button

        //set up two buttons and a text field for name entry
        listControls.getChildren().iterator().forEachRemaining(n -> n.setDisable(true));
        theRenameButton.setDisable(false);

        settings.setDisable(true);

        int renameIndex = listControls.getChildren().indexOf(theRenameButton);

        TextField renameTextField = new TextField();

        listControls.getChildren().add(renameIndex + 1, renameTextField);

        Button cancelRenameButton = new Button("Cancel");

        listControls.getChildren().add(renameIndex + 2, cancelRenameButton);

        renameTextField.setMaxWidth(Double.MAX_VALUE);

        cancelRenameButton.setMaxWidth(Double.MAX_VALUE);

        //set up to event handlers, one to return the controls back to their original state,
        //and the other to actually perform the rename
        EventHandler<ActionEvent> finishRename = event ->
        {
            listControls.getChildren().removeAll(renameTextField, cancelRenameButton);
            theRenameButton.setOnAction(oldOnAction);

            listControls.getChildren().iterator().forEachRemaining(n -> n.setDisable(false));

            cameraListView.refresh();

            settings.setDisable(false);
        };

        EventHandler<ActionEvent> doRename = event ->
        {
            String newName = renameTextField.getText();
            if (newName != null && !newName.isEmpty())
            {
                getCameraSelectionModel().getSelectedItem().setName(newName);
            }

            finishRename.handle(event);
        };

        //set the on actions
        theRenameButton.setOnAction(doRename);
        cancelRenameButton.setOnAction(finishRename);
        renameTextField.setOnAction(doRename);

        renameTextField.setText(getCameraSelectionModel().getSelectedItem().getName());
        renameTextField.requestFocus();
        renameTextField.selectAll();
    }

    @FXML
    private void moveUPButton()
    {
        int i = getCameraSelectionModel().getSelectedIndex();
        if (i > 1)
        {
            Collections.swap(projectModel.getCameraList(), i, i - 1);
            getCameraSelectionModel().select(i - 1);
        }
    }

    @FXML
    void moveDOWNButton()
    {
        int i = getCameraSelectionModel().getSelectedIndex();
        List<CameraSetting> cameraList = projectModel.getCameraList();
        if (i != 0 && i < cameraList.size() - 1)
        {
            Collections.swap(cameraList, i, i + 1);
            getCameraSelectionModel().select(i + 1);
        }
    }

    @FXML
    void lockCameraButton()
    {
        Boolean newValue = !getCameraSelectionModel().getSelectedItem().isLocked();
        getCameraSelectionModel().getSelectedItem().setLocked(newValue);
        settingsController.setDisabled(newValue);
        cameraListView.refresh();
    }

    @FXML
    void keyframeCameraButton()
    {
        //TODO
        log.debug("TODO: keyframe added for " + getCameraSelectionModel().getSelectedItem());
    }

    @FXML
    void deleteCameraButton()
    {
        int selectedIndex = getCameraSelectionModel().getSelectedIndex();
        if (selectedIndex != 0)
        {
            Dialog<ButtonType> confirmation = new Alert(AlertType.CONFIRMATION, "This action cannot be reversed.");
            confirmation.setHeaderText("Are you sure you want to delete the following camera: "
                + projectModel.getCameraList().get(selectedIndex).getName() + '?');
            confirmation.setTitle("Delete Confirmation");

            confirmation.showAndWait()
                .filter(Predicate.isEqual(ButtonType.OK))
                .ifPresent(response -> projectModel.getCameraList().remove(selectedIndex));
        }
    }
}