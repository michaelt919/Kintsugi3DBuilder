/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.scene.environment;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
import kintsugi3d.builder.javafx.internal.EnvironmentModelImpl;

public class RootEnvironmentSceneController
{
    private static final Logger log = LoggerFactory.getLogger(RootEnvironmentSceneController.class);
    @FXML
    private VBox settings;
    @FXML
    private SettingsEnvironmentSceneController settingsController;
    @FXML
    private VBox listControls;
    @FXML
    private ListView<EnvironmentSetting> environmentListView;
    @FXML
    private Button theRenameButton;

    private static final boolean USE_STARTING_MAP = false;

    private ObservableProjectModel projectModel;

    public void init(EnvironmentModelImpl environmentMapModel, ObservableProjectModel injectedProjectModel)
    {
        this.projectModel = injectedProjectModel;

        environmentListView.setItems(projectModel.getEnvironmentList());
        environmentListView.getSelectionModel().selectedItemProperty().addListener(settingsController.changeListener);

        settingsController.setEnvironmentMapModel(environmentMapModel);

        ObservableList<EnvironmentSetting> environmentList = projectModel.getEnvironmentList();

        projectModel.getEnvironmentList().add(EnvironmentSetting.NO_ENVIRONMENT);

        if (USE_STARTING_MAP)
        {
            EnvironmentSetting startingMap = new EnvironmentSetting();
            startingMap.setName("Environment 1");
            projectModel.getEnvironmentList().add(startingMap);
            environmentListView.getSelectionModel().select(1);
        }
        else
        {
            environmentListView.getSelectionModel().select(0);
        }

        environmentList.addListener((ListChangeListener<? super EnvironmentSetting>) change ->
        {
            change.next();
            if (change.wasAdded() && change.getAddedSize() == environmentList.size())
            {
                environmentListView.getSelectionModel().select(0);
            }
        });

        environmentMapModel.loadedEnvironmentMapImageProperty().addListener(
            (observable, oldValue, newValue) -> settingsController.updateEnvironmentMapImage(newValue));

        environmentMapModel.setSelected(environmentListView.getSelectionModel().selectedItemProperty());
    }

    @FXML
    private void newEnvButton()
    {
        MultipleSelectionModel<EnvironmentSetting> selectionModel = environmentListView.getSelectionModel();
        EnvironmentSetting selectedEnvironment = selectionModel.getSelectedItem();
        List<EnvironmentSetting> environmentList = projectModel.getEnvironmentList();

        if (selectedEnvironment == null || Objects.equals(selectedEnvironment, EnvironmentSetting.NO_ENVIRONMENT))
        {
            environmentList.add(new EnvironmentSetting());
        }
        else
        {
            environmentList.add(selectedEnvironment.duplicate());
        }

        selectionModel.select(environmentList.size() - 1);
    }

    @FXML
    private void saveEnvButton()
    {
        //TODO
        log.debug("TODO: saved " + environmentListView.getSelectionModel().getSelectedItem() + " to the library.");
    }

    @FXML
    private void renameEnvButton()
    {
        if (!Objects.equals(environmentListView.getSelectionModel().getSelectedItem(), EnvironmentSetting.NO_ENVIRONMENT))
        {
            EventHandler<ActionEvent> oldOnAction = theRenameButton.getOnAction();//backup the old on action event for the rename button

            //set up two buttons and a text field for name entry
            listControls.getChildren().iterator().forEachRemaining(n -> n.setDisable(true));
            settings.setDisable(true);
            theRenameButton.setDisable(false);

            int renameIndex = listControls.getChildren().indexOf(theRenameButton);
            TextField renameTextField = new TextField();
            listControls.getChildren().add(renameIndex + 1, renameTextField);
            Button cancelRenameButton = new Button("Cancel");
            listControls.getChildren().add(renameIndex + 2, cancelRenameButton);
            renameTextField.setMaxWidth(Double.MAX_VALUE);
            cancelRenameButton.setMaxWidth(Double.MAX_VALUE);

            //set up to event handlers, one to return the controls back to their original state,
            // and the other to actually perform the rename
            EventHandler<ActionEvent> finishRename = event ->
            {
                listControls.getChildren().removeAll(renameTextField, cancelRenameButton);
                theRenameButton.setOnAction(oldOnAction);

                listControls.getChildren().iterator().forEachRemaining(n -> n.setDisable(false));

                environmentListView.refresh();
                settings.setDisable(false);
            };

            EventHandler<ActionEvent> doRename = event ->
            {
                String newName = renameTextField.getText();
                if (newName != null && !newName.isEmpty())
                {
                    environmentListView.getSelectionModel().getSelectedItem().setName(newName);
                }

                finishRename.handle(event);
            };

            //set the on actions
            theRenameButton.setOnAction(doRename);
            cancelRenameButton.setOnAction(finishRename);
            renameTextField.setOnAction(doRename);

            renameTextField.setText(environmentListView.getSelectionModel().getSelectedItem().getName());
            renameTextField.requestFocus();
            renameTextField.selectAll();
        }
    }

    @FXML
    private void moveUPButton()
    {
        int selectedIndex = environmentListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex > 1)
        {
            Collections.swap(projectModel.getEnvironmentList(), selectedIndex, selectedIndex - 1);
            environmentListView.getSelectionModel().select(selectedIndex - 1);
        }
    }

    @FXML
    void moveDOWNButton()
    {
        int selectedIndex = environmentListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex != 0)
        {
            List<EnvironmentSetting> environmentList = projectModel.getEnvironmentList();
            if (selectedIndex < environmentList.size() - 1)
            {
                Collections.swap(environmentList, selectedIndex, selectedIndex + 1);
                environmentListView.getSelectionModel().select(selectedIndex + 1);
            }
        }
    }

    @FXML
    void lockEnvButton()
    {
        EnvironmentSetting selectedEnvironment = environmentListView.getSelectionModel().getSelectedItem();
        if (!Objects.equals(selectedEnvironment, EnvironmentSetting.NO_ENVIRONMENT))
        {
            Boolean newValue = !selectedEnvironment.isLocked();
            selectedEnvironment.setLocked(newValue);
            settingsController.setDisabled(newValue);
            environmentListView.refresh();
        }
    }

    @FXML
    void deleteEnvButton()
    {
        MultipleSelectionModel<EnvironmentSetting> selectionModel = environmentListView.getSelectionModel();
        EnvironmentSetting selectedEnvironment = selectionModel.getSelectedItem();

        if (!Objects.equals(selectedEnvironment, EnvironmentSetting.NO_ENVIRONMENT))
        {
            Dialog<ButtonType> confirmation = new Alert(AlertType.CONFIRMATION, "This action cannot be reversed.");
            confirmation.setHeaderText("Are you sure you want to delete the following environment: " + selectedEnvironment.getName() + '?');
            confirmation.setTitle("Delete Confirmation");

            confirmation.showAndWait()
                .filter(Predicate.isEqual(ButtonType.OK))
                .ifPresent(response -> projectModel.getEnvironmentList().remove(selectionModel.getSelectedIndex()));
        }
    }
}
