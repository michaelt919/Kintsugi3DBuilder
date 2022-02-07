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

package tetzlaff.ibrelight.javafx.controllers.scene.lights;//Created by alexk on 7/16/2017.

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import javafx.scene.control.skin.TableHeaderRow;
import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.layout.VBox;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.javafx.controllers.scene.SceneModel;
import tetzlaff.ibrelight.javafx.internal.LightingModelImpl;
import tetzlaff.models.SceneViewport;
import tetzlaff.models.SceneViewportModel;

public class RootLightSceneController implements Initializable
{
    @FXML private VBox settings;
    @FXML private SettingsLightSceneController settingsController;
    @FXML private TableView<LightGroupSetting> tableView;
    @FXML private VBox groupControls;
    @FXML private VBox lightControls;
    @FXML private Button renameButton;

    private final Property<LightInstanceSetting> selectedLight = new SimpleObjectProperty<>();
    private int lastSelectedIndex = -1;

    private SceneModel sceneModel;
    private SceneViewportModel sceneViewportModel;

    @SuppressWarnings("rawtypes")
    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        //columns
        TableColumn<LightGroupSetting, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(param ->
        {
            if (param.getValue().isLocked())
            {
                return new SimpleStringProperty("(L) " + param.getValue().getName());
            }
            else
            {
                return new SimpleStringProperty( param.getValue().getName());
            }
        });
        nameCol.setPrefWidth(90);
        tableView.getColumns().add(nameCol);

        for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++)
        {
            int currentIndex = i;
            TableColumn<LightGroupSetting, LightInstanceSetting> newCol = new TableColumn<>(String.valueOf(currentIndex + 1));
            newCol.setCellValueFactory(param -> param.getValue().lightListProperty().valueAt(currentIndex));
            newCol.setPrefWidth(20);
            tableView.getColumns().add(newCol);
        }

        tableView.getSelectionModel().setCellSelectionEnabled(true);

        //light selection listener
        //noinspection rawtypes
        tableView.getSelectionModel().getSelectedCells().addListener((ListChangeListener<TablePosition>) change ->
        {
            ObservableList<TablePosition> selectedCells = tableView.getSelectionModel().getSelectedCells();

            if (!selectedCells.isEmpty())
            {
                //new cell selected
                assert change.next() && change.getAddedSize() == 1;
                TablePosition<?, ?> tablePosition = tableView.getSelectionModel().getSelectedCells().get(0);
                ObservableValue<?> selectedCell = tablePosition.getTableColumn().getCellObservableValue(tablePosition.getRow());
                LightGroupSetting selectedLightGroup = getSelectedLightGroup();

                if (selectedCell != null && selectedCell.getValue() instanceof LightInstanceSetting)
                {
                    selectedLight.setValue((LightInstanceSetting) selectedCell.getValue());
                    lastSelectedIndex = tablePosition.getColumn() - 1;
                    if (selectedLightGroup != null)
                    {
                        selectedLightGroup.setSelectedLightIndex(lastSelectedIndex);
                    }

                }
                else
                {
                    selectedLight.setValue(null);
                    lastSelectedIndex = -1;
                    if (selectedLightGroup != null)
                    {
                        selectedLightGroup.setSelectedLightIndex(-1);
                    }
                }
            }
        });

        //preventing reordering or rearranging
        tableView.skinProperty().addListener((obs, oldS, newS) ->
        {
//            TableHeaderRow tableHeaderRow = (TableHeaderRow) tableView.lookup("TableHeaderRow");
//            tableHeaderRow.reorderingProperty().addListener((p, o, n) ->
//                tableHeaderRow.setReordering(false));
        });

        tableView.setSortPolicy(param -> false);
        tableView.setColumnResizePolicy(param -> false);

        selectedLight.addListener(settingsController.changeListener);
    }

    private InvalidationListener generateLightChangeListener(LightGroupSetting lightGroup)
    {
        return observable ->
        {
            TableViewSelectionModel<LightGroupSetting> selectionModel = tableView.getSelectionModel();
            int groupIndex = tableView.getItems().indexOf(lightGroup);
            TableColumn<LightGroupSetting, ?> selectedColumn = tableView.getColumns().get(lightGroup.getSelectedLightIndex() + 1);
            if (selectionModel.getSelectedIndex() == groupIndex && !selectionModel.isSelected(groupIndex, selectedColumn))
            {
                selectionModel.clearAndSelect(groupIndex, selectedColumn);
            }
        };
    }

    public void init(LightingModelImpl lightingModel, SceneModel injectedSceneModel, SceneViewportModel injectedSceneViewportModel)
    {
        this.sceneModel = injectedSceneModel;
        this.sceneViewportModel = injectedSceneViewportModel;

        ObservableList<LightGroupSetting> lightGroupList = sceneModel.getLightGroupList();
        tableView.setItems(lightGroupList);

        //lightGroupList.add(new LightGroupSetting("Free Lights"));

        lightGroupList.addListener((ListChangeListener<? super LightGroupSetting>) change ->
        {
            change.next();
            if (change.wasAdded() && change.getAddedSize() == lightGroupList.size())
            {
                tableView.getSelectionModel().select(0);
            }
        });

        for (LightGroupSetting lightGroup : lightGroupList)
        {
            lightGroup.selectedLightIndexProperty().addListener(generateLightChangeListener(lightGroup));
        }

        lightGroupList.addListener((ListChangeListener<? super LightGroupSetting>) change ->
        {
            while(change.next())
            {
                for (LightGroupSetting newLightGroup : change.getAddedSubList())
                {
                    newLightGroup.selectedLightIndexProperty().addListener(generateLightChangeListener(newLightGroup));
                }
            }
        });

        ObservableValue<LightGroupSetting> observableValue = tableView.getSelectionModel().selectedItemProperty();
        lightingModel.setSelectedLightGroupSetting(observableValue );

        // Setup an initial light group with a single light source.
        // TODO don't do this if a default environment map is available.
        newGroup();
        newLight();
    }

    @FXML
    private void newGroup()
    {
        //for now we will create a blank group
        //in the future we may want to duplicate the previous group instead
        LightGroupSetting newGroup = new LightGroupSetting("New Group");
        List<LightGroupSetting> lightGroupList = sceneModel.getLightGroupList();
        lightGroupList.add(newGroup);

        int newRowIndex = lightGroupList.size() - 1;
        tableView.getSelectionModel().clearAndSelect(newRowIndex, tableView.getColumns().get(0));
    }

    @FXML
    private void saveGroup()
    {
        System.out.println("TODO saveGroup");//TODO
    }

    @FXML
    private void renameGroup()
    {
        EventHandler<ActionEvent> oldOnAction = renameButton.getOnAction();//backup the old on action event for the rename button

        //disable all
        groupControls.getChildren().iterator().forEachRemaining(node -> node.setDisable(true));
        lightControls.getChildren().iterator().forEachRemaining(node -> node.setDisable(true));

        settings.setDisable(true);
        renameButton.setDisable(false);

        int renameIndex = groupControls.getChildren().indexOf(renameButton);

        TextField renameTextField = new TextField();
        groupControls.getChildren().add(renameIndex + 1, renameTextField);

        Button cancelRenameButton = new Button("Cancel");
        groupControls.getChildren().add(renameIndex + 2, cancelRenameButton);

        renameTextField.setMaxWidth(Double.MAX_VALUE);
        cancelRenameButton.setMaxWidth(Double.MAX_VALUE);

        //set up to event handlers, one to return the controls back to their original state,
        //and the other to actually perform the rename
        EventHandler<ActionEvent> finishRename = event ->
        {
            groupControls.getChildren().removeAll(renameTextField, cancelRenameButton);
            renameButton.setOnAction(oldOnAction);

            groupControls.getChildren().iterator().forEachRemaining(n -> n.setDisable(false));
            lightControls.getChildren().iterator().forEachRemaining(n -> n.setDisable(false));

            settings.setDisable(false);
            tableView.refresh();
        };

        EventHandler<ActionEvent> doRename = event ->
        {
            String newName = renameTextField.getText();
            if (!newName.isEmpty())
            {
                tableView.getSelectionModel().getSelectedItem().setName(newName);
            }

            finishRename.handle(event);
        };

        //set the on actions
        renameButton.setOnAction(doRename);
        cancelRenameButton.setOnAction(finishRename);
        renameTextField.setOnAction(doRename);

        renameTextField.setText(tableView.getSelectionModel().getSelectedItem().getName());
        renameTextField.requestFocus();
        renameTextField.selectAll();
    }

    @FXML
    private void moveUPGroup()
    {
        if (getSelectedLightGroupIndex() > 0)
        {
            Collections.swap(sceneModel.getLightGroupList(), getSelectedLightGroupIndex(), getSelectedLightGroupIndex() - 1);
        }
    }

    @FXML
    private void moveDOWNGroup()
    {
        List<LightGroupSetting> lightGroupList = sceneModel.getLightGroupList();
        if (getSelectedLightGroupIndex() < lightGroupList.size() - 1)
        {
            Collections.swap(lightGroupList, getSelectedLightGroupIndex(), getSelectedLightGroupIndex() + 1);
        }
    }

    @FXML
    private void lockGroup()
    {
        if (getSelectedLightGroup() != null)
        {
            boolean n = !getSelectedLightGroup().isLocked();
            getSelectedLightGroup().setLocked(n);
            if (selectedLight.getValue() != null)
            {
                settingsController.setDisabled(n || selectedLight.getValue().locked().get());
            }
        }

        tableView.refresh();
    }

    @FXML
    private void keyframeGroup()
    {
        System.out.println("TODO keyframeGroup");//TODO
    }

    @FXML
    private void deleteGroup()
    {
        List<LightGroupSetting> lightGroupList = sceneModel.getLightGroupList();
        int selectedRow = getSelectedLightGroupIndex();

        if (lightGroupList.size() > selectedRow && selectedRow >= 0)
        {
            Dialog<ButtonType> confirmation = new Alert(AlertType.CONFIRMATION, "This action cannot be reversed.");
            confirmation.setHeaderText( "Are you sure you want to delete the following light group: "
                + lightGroupList.get(selectedRow).getName() + '?');
            confirmation.setTitle("Delete Confirmation");

            confirmation.showAndWait()
                .filter(Predicate.isEqual(ButtonType.OK))
                .ifPresent(response -> lightGroupList.remove(selectedRow));
        }
    }

    @FXML
    private void newLight()
    {
        LightGroupSetting selectedLightGroup = getSelectedLightGroup();
        if (selectedLightGroup != null)
        {
            SceneViewport sceneViewport = sceneViewportModel.getSceneViewport();

            if (lastSelectedIndex >= 0 && lastSelectedIndex < selectedLightGroup.getLightCount())
            {
                LightInstanceSetting lastSelectedLight = selectedLightGroup.lightListProperty().get(lastSelectedIndex);
                Vector3 currentLightTarget =
                    new Vector3((float) lastSelectedLight.targetX().get(), (float) lastSelectedLight.targetY().get(), (float) lastSelectedLight.targetZ().get());
                Vector2 windowPosition = sceneViewport.projectPoint(currentLightTarget);

                Vector2 newWindowPosition;

                if (windowPosition.x > 0.5f)
                {
                    newWindowPosition = windowPosition.minus(new Vector2(0.15625f, 0));
                }
                else
                {
                    newWindowPosition = windowPosition.plus(new Vector2(0.125f, 0));
                }

                Vector3 objectLightTarget = sceneViewport.get3DPositionAtCoordinates(newWindowPosition.x, newWindowPosition.y);

                Vector3 cameraCenter = sceneViewport.getViewportCenter();
                float currentDistance = cameraCenter.distance(currentLightTarget);
                Vector3 equidistantLightTarget = cameraCenter
                    .plus(sceneViewport.getViewingDirection(newWindowPosition.x, newWindowPosition.y).times(currentDistance));

                if (cameraCenter.distance(objectLightTarget) < currentDistance)
                {
                    selectedLightGroup.addLight(lastSelectedIndex, objectLightTarget.x, objectLightTarget.y, objectLightTarget.z);
                }
                else
                {
                    selectedLightGroup.addLight(lastSelectedIndex, equidistantLightTarget.x, equidistantLightTarget.y, equidistantLightTarget.z);
                }
            }
            else
            {
                selectedLightGroup.addLight();
            }

            tableView.refresh();
            tableView.getSelectionModel().clearAndSelect(tableView.getSelectionModel().getSelectedIndex(),
                tableView.getColumns().get(selectedLightGroup.getLightCount()));
        }
    }

    @FXML
    private void saveLight()
    {
        System.out.println("TODO saveLight");//TODO
    }

    @FXML
    private void lockLight()
    {
        if (selectedLight.getValue() != null)
        {
            boolean newValue = !selectedLight.getValue().locked().get();
            selectedLight.getValue().locked().set(newValue);
            settingsController.setDisabled(newValue || selectedLight.getValue().isGroupLocked());
            if (newValue)
            {
                selectedLight.getValue().name().set("L");
            }
            else
            {
                selectedLight.getValue().name().set("X");
            }
            tableView.refresh();
        }
    }

    @FXML
    private void deleteLight()
    {
        LightGroupSetting selectedLightGroup = getSelectedLightGroup();
        if (selectedLightGroup != null && lastSelectedIndex >= 0 && lastSelectedIndex < selectedLightGroup.getLightCount())
        {
            Dialog<ButtonType> confirmation = new Alert(AlertType.CONFIRMATION, "This action cannot be reversed.");
            confirmation.setHeaderText("Are you sure you want to delete light " + (lastSelectedIndex + 1) + " from the following group: "
                + selectedLightGroup.getName() + '?');
            confirmation.setTitle("Delete Confirmation");

            confirmation.showAndWait()
                .filter(Predicate.isEqual(ButtonType.OK))
                .ifPresent(response ->
                {
                    selectedLightGroup.removeLight(lastSelectedIndex);
                    tableView.refresh();
                    tableView.getSelectionModel().selectPrevious();
                });
        }
    }

    private int getSelectedLightGroupIndex()
    {
        return tableView.getSelectionModel().getSelectedIndex();
    }

    private LightGroupSetting getSelectedLightGroup()
    {
        return tableView.getSelectionModel().getSelectedItem();
    }
}
