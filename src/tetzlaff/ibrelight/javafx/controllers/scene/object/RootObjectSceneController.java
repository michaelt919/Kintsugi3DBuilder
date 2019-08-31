/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx.controllers.scene.object;

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
import tetzlaff.ibrelight.javafx.controllers.scene.SceneModel;
import tetzlaff.ibrelight.javafx.internal.ObjectModelImpl;

public class RootObjectSceneController
{
    @FXML
    private VBox settings;
    @FXML
    private SettingsObjectSceneController settingsController;
    @FXML
    private ListView<ObjectPoseSetting> objectPoseListView;
    @FXML
    private VBox listControls;
    @FXML
    private Button renameButton;

    private SceneModel sceneModel;

    public void init(ObjectModelImpl objectModel, SceneModel injectedSceneModel)
    {
        this.sceneModel = injectedSceneModel;

        objectPoseListView.setItems(sceneModel.getObjectPoseList());
        objectPoseListView.getSelectionModel().selectedItemProperty().addListener(settingsController.changeListener);

        ObjectPoseSetting defaultPose = new ObjectPoseSetting(
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            false,
            "Default Pose"
        );

        ObservableList<ObjectPoseSetting> objectPoseList = sceneModel.getObjectPoseList();

        objectPoseList.add(defaultPose);
        objectPoseListView.getSelectionModel().select(defaultPose);

        objectPoseList.addListener((ListChangeListener<? super ObjectPoseSetting>) change ->
        {
            change.next();
            if (change.wasAdded() && change.getAddedSize() == objectPoseList.size())
            {
                objectPoseListView.getSelectionModel().select(0);
            }
        });

        objectModel.setSelectedObjectPoseProperty(objectPoseListView.getSelectionModel().selectedItemProperty());
    }

    private SelectionModel<ObjectPoseSetting> getObjectPoseSelectionModel()
    {
        return objectPoseListView.getSelectionModel();
    }

    @FXML
    private void newPoseButton()
    {
        sceneModel.getObjectPoseList()
            .add(getObjectPoseSelectionModel().getSelectedItem().duplicate());
        getObjectPoseSelectionModel().select(sceneModel.getObjectPoseList().size() - 1);
    }

    @FXML
    private void savePoseButton()
    {
        System.out.println("TODO: saved " + getObjectPoseSelectionModel().getSelectedItem() + " to the library.");
    }

    @FXML
    private void renamePoseButton()
    {
        if (getObjectPoseSelectionModel().getSelectedIndex() == 0)
        {
            return;
        }

        EventHandler<ActionEvent> oldOnAction = renameButton.getOnAction();//backup the old on action event for the rename button

        //set up two buttons and a text field for name entry
        listControls.getChildren().iterator().forEachRemaining(n -> n.setDisable(true));
        renameButton.setDisable(false);

        settings.setDisable(true);

        int renameIndex = listControls.getChildren().indexOf(renameButton);

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
            renameButton.setOnAction(oldOnAction);

            listControls.getChildren().iterator().forEachRemaining(n -> n.setDisable(false));

            objectPoseListView.refresh();

            settings.setDisable(false);
        };

        EventHandler<ActionEvent> doRename = event ->
        {
            String newName = renameTextField.getText();
            if (newName != null && !newName.isEmpty())
            {
                getObjectPoseSelectionModel().getSelectedItem().setName(newName);
            }

            finishRename.handle(event);
        };

        //set the on actions
        renameButton.setOnAction(doRename);
        cancelRenameButton.setOnAction(finishRename);
        renameTextField.setOnAction(doRename);

        renameTextField.setText(getObjectPoseSelectionModel().getSelectedItem().getName());
        renameTextField.requestFocus();
        renameTextField.selectAll();
    }

    @FXML
    private void moveUPButton()
    {
        int i = getObjectPoseSelectionModel().getSelectedIndex();
        if (i > 1)
        {
            Collections.swap(sceneModel.getObjectPoseList(), i, i - 1);
            getObjectPoseSelectionModel().select(i - 1);
        }
    }

    @FXML
    void moveDOWNButton()
    {
        int i = getObjectPoseSelectionModel().getSelectedIndex();
        List<ObjectPoseSetting> objectPoseList = sceneModel.getObjectPoseList();
        if (i != 0 && i < objectPoseList.size() - 1)
        {
            Collections.swap(objectPoseList, i, i + 1);
            getObjectPoseSelectionModel().select(i + 1);
        }
    }

    @FXML
    void lockPoseButton()
    {
        Boolean newValue = !getObjectPoseSelectionModel().getSelectedItem().isLocked();
        getObjectPoseSelectionModel().getSelectedItem().setLocked(newValue);
        settingsController.setDisabled(newValue);
        objectPoseListView.refresh();
    }

    @FXML
    void keyframePoseButton()
    {
        //TODO
        System.out.println("TODO: keyframe added for " + getObjectPoseSelectionModel().getSelectedItem());
    }

    @FXML
    void deletePoseButton()
    {
        int selectedIndex = getObjectPoseSelectionModel().getSelectedIndex();
        if (selectedIndex != 0)
        {
            Dialog<ButtonType> confirmation = new Alert(AlertType.CONFIRMATION, "This action cannot be reversed.");
            confirmation.setHeaderText("Are you sure you want to delete the following object pose: "
                + sceneModel.getObjectPoseList().get(selectedIndex).getName() + '?');
            confirmation.setTitle("Delete Confirmation");

            confirmation.showAndWait()
                .filter(Predicate.isEqual(ButtonType.OK))
                .ifPresent(response -> sceneModel.getObjectPoseList().remove(selectedIndex));
        }
    }
}