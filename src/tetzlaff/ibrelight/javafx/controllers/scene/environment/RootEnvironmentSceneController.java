package tetzlaff.ibrelight.javafx.controllers.scene.environment;

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
import javafx.scene.paint.Color;
import tetzlaff.ibrelight.javafx.controllers.scene.SceneModel;
import tetzlaff.ibrelight.javafx.internal.EnvironmentModelImpl;
import tetzlaff.ibrelight.javafx.internal.SettingsModelImpl;

public class RootEnvironmentSceneController
{
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

    private SceneModel sceneModel;

    public void init(EnvironmentModelImpl environmentMapModel, SceneModel injectedSceneModel, SettingsModelImpl injectedSettingsModel)
    {
        this.sceneModel = injectedSceneModel;

        environmentListView.setItems(sceneModel.getEnvironmentList());
        environmentListView.getSelectionModel().selectedItemProperty().addListener(settingsController.changeListener);

        settingsController.setSettingsModel(injectedSettingsModel);

        ObservableList<EnvironmentSetting> environmentList = sceneModel.getEnvironmentList();

        if (USE_STARTING_MAP)
        {
            EnvironmentSetting startingMap = new EnvironmentSetting(
                false,
                false,
                false,
                false,
                false,
                null,
                null,
                1.0,
                0.0,
                Color.WHITE,
                Color.BLACK,
                "Free Environment Map",
                false,
                false
            );

            sceneModel.getEnvironmentList().add(startingMap);
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

        environmentMapModel.setSelected(environmentListView.getSelectionModel().selectedItemProperty());
    }

    @FXML
    private void newEnvButton()
    {
        if (environmentListView.getSelectionModel().getSelectedItem() == null)
        {

            sceneModel.getEnvironmentList().add(
                new EnvironmentSetting(
                    false,
                    false,
                    false,
                    false,
                    false,
                    null,
                    null,
                    1.0,
                    0.0,
                    Color.WHITE,
                    Color.BLACK,
                    "New Environment",
                    false,
                    false
                )
            );
        }
        else
        {

            List<EnvironmentSetting> environmentList = sceneModel.getEnvironmentList();
            environmentList.add(environmentListView.getSelectionModel().getSelectedItem().duplicate());
            environmentListView.getSelectionModel().select(environmentList.size() - 1);
        }
    }

    @FXML
    private void saveEnvButton()
    {
        System.out.println("TODO: saved " + environmentListView.getSelectionModel().getSelectedItem() + " to the library.");
    }

    @FXML
    private void renameEnvButton()
    {
        if (USE_STARTING_MAP && environmentListView.getSelectionModel().getSelectedIndex() == 0)
        {
            return;
        }

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

    @FXML
    private void moveUPButton()
    {
        int i = environmentListView.getSelectionModel().getSelectedIndex();
        if (USE_STARTING_MAP && i == 1)
        {
            return;
        }
        if (i > 0)
        {
            Collections.swap(sceneModel.getEnvironmentList(), i, i - 1);
            environmentListView.getSelectionModel().select(i - 1);
        }
    }

    @FXML
    void moveDOWNButton()
    {
        int i = environmentListView.getSelectionModel().getSelectedIndex();
        if (USE_STARTING_MAP && i == 0)
        {
            return;
        }

        List<EnvironmentSetting> environmentList = sceneModel.getEnvironmentList();
        if (i < environmentList.size() - 1)
        {
            Collections.swap(environmentList, i, i + 1);
            environmentListView.getSelectionModel().select(i + 1);
        }
    }

    @FXML
    void lockEnvButton()
    {
        Boolean newValue = !environmentListView.getSelectionModel().getSelectedItem().isLocked();
        environmentListView.getSelectionModel().getSelectedItem().setLocked(newValue);
        settingsController.setDisabled(newValue);
        environmentListView.refresh();
    }

    @FXML
    void deleteEnvButton()
    {
        int selectedIndex = environmentListView.getSelectionModel().getSelectedIndex();
        if (!USE_STARTING_MAP || selectedIndex != 0)
        {
            Dialog<ButtonType> confirmation = new Alert(AlertType.CONFIRMATION, "This action cannot be reversed.");
            confirmation.setHeaderText("Are you sure you want to delete the following environment: "
                + sceneModel.getEnvironmentList().get(selectedIndex).getName() + '?');
            confirmation.setTitle("Delete Confirmation");

            confirmation.showAndWait()
                .filter(Predicate.isEqual(ButtonType.OK))
                .ifPresent(response -> sceneModel.getEnvironmentList().remove(selectedIndex));
        }
    }
}
