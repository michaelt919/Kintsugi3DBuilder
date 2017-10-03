package tetzlaff.ibr.javafx.controllers.scene.camera;

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
import tetzlaff.ibr.javafx.controllers.scene.SceneModel;
import tetzlaff.ibr.javafx.internal.CameraModelImpl;

public class RootCameraSceneController
{
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

    private SceneModel sceneModel;

    public void init(CameraModelImpl cameraModel, SceneModel injectedSceneModel)
    {
        this.sceneModel = injectedSceneModel;

        cameraListView.setItems(sceneModel.getCameraList());
        cameraListView.getSelectionModel().selectedItemProperty().addListener(settingsController.changeListener);

        CameraSetting freeCam = new CameraSetting(
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            90.0,
            18.0,
            false,
            false,
            "Free Camera"

        );

        ObservableList<CameraSetting> cameraList = sceneModel.getCameraList();

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
        sceneModel.getCameraList()
            .add(getCameraSelectionModel().getSelectedItem().duplicate());
        getCameraSelectionModel().select(sceneModel.getCameraList().size() - 1);
    }

    @FXML
    private void saveCameraButton()
    {
        System.out.println("TODO: saved " + getCameraSelectionModel().getSelectedItem() + " to the library.");
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
            Collections.swap(sceneModel.getCameraList(), i, i - 1);
            getCameraSelectionModel().select(i - 1);
        }
    }

    @FXML
    void moveDOWNButton()
    {
        int i = getCameraSelectionModel().getSelectedIndex();
        List<CameraSetting> cameraList = sceneModel.getCameraList();
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
        System.out.println("TODO: keyframe added for " + getCameraSelectionModel().getSelectedItem());
    }

    @FXML
    void deleteCameraButton()
    {
        int selectedIndex = getCameraSelectionModel().getSelectedIndex();
        if (selectedIndex != 0)
        {
            Dialog<ButtonType> confirmation = new Alert(AlertType.CONFIRMATION, "This action cannot be reversed.");
            confirmation.setHeaderText("Are you sure you want to delete the following camera: "
                + sceneModel.getCameraList().get(selectedIndex).getName() + '?');
            confirmation.setTitle("Delete Confirmation");

            confirmation.showAndWait()
                .filter(Predicate.isEqual(ButtonType.OK))
                .ifPresent(response -> sceneModel.getCameraList().remove(selectedIndex));
        }
    }
}