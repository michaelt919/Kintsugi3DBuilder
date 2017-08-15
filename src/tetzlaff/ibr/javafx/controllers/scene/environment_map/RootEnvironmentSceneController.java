package tetzlaff.ibr.javafx.controllers.scene.environment_map;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import tetzlaff.ibr.javafx.models.JavaFXEnvironmentMapModel;

import com.sun.javafx.collections.ObservableListWrapper;

public class RootEnvironmentSceneController implements Initializable {
    private ObservableList<EnvironmentSettings> listOfEnvironments = new ObservableListWrapper<>(new ArrayList<>());
    @FXML
    private VBox settings;
    @FXML
    private SettingsEnvironmentSceneController settingsController;
    @FXML
    private VBox listControls;
    @FXML
    private ListView<EnvironmentSettings> environmentListView;
    @FXML
    private Button theRenameButton;

    private boolean useStartingMap = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
    	environmentListView.setItems(listOfEnvironments);

        s().selectedItemProperty().addListener(settingsController.changeListener);

        if(useStartingMap) {
            EnvironmentSettings startingMap = new EnvironmentSettings(
                    false,
                    false,
                    false,
                    false,
                    false,
                    null,
                    null,
                    1.0,
                    0.0,
                    new Color(0.0, 0.0, 0.0, 1.0),
                    new Color(0, 0, 0, 1),
                    "Free Environment Map",
                    false,
                    false
            );


            listOfEnvironments.add(startingMap);
            s().select(0);
        }


    }

    public void init2(JavaFXEnvironmentMapModel environmentMapModel){
        environmentMapModel.setSelected(s().selectedItemProperty());
    }

    private SelectionModel<EnvironmentSettings> s() {
        return environmentListView.getSelectionModel();
    }


    @FXML
    private void newEnvButton() {
        if(s().getSelectedItem() == null){
            listOfEnvironments.add(
                    new EnvironmentSettings(
                            false,
                            false,
                            false,
                            false,
                            false,
                            null,
                            null,
                            1.0,
                            0.0,
                            Color.BLACK,
                            Color.BLACK,
                            "New Environment",
                            false,
                            false
                    )
            );
        } else {
            listOfEnvironments.add(s().getSelectedItem().duplicate());
            s().select(listOfEnvironments.size() - 1);
        }
    }

    @FXML
    private void saveEnvButton() {
        System.out.println("TODO: saved " + s().getSelectedItem() + " to the library.");
    }

    @FXML
    private void renameEnvButton() {
        if (useStartingMap && s().getSelectedIndex() == 0) return;

        EventHandler<ActionEvent> oldOnAction = theRenameButton.getOnAction();//backup the old on action event for the rename button

        //set up two buttons and a text field for name entry
        listControls.getChildren().iterator().forEachRemaining(n -> {
            n.setDisable(true);
        });
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
        EventHandler<ActionEvent> finishRename = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                listControls.getChildren().removeAll(renameTextField, cancelRenameButton);
                theRenameButton.setOnAction(oldOnAction);

                listControls.getChildren().iterator().forEachRemaining(n -> {
                    n.setDisable(false);
                });

                environmentListView.refresh();

                settings.setDisable(false);
            }
        };

        EventHandler<ActionEvent> doRename = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String newName = renameTextField.getText();
                if (!newName.equals("")) {
                    s().getSelectedItem().setName(newName);
                }

                finishRename.handle(event);
            }
        };

        //set the on actions
        theRenameButton.setOnAction(doRename);
        cancelRenameButton.setOnAction(finishRename);
        renameTextField.setOnAction(doRename);

        renameTextField.setText(s().getSelectedItem().getName());
        renameTextField.requestFocus();
        renameTextField.selectAll();

    }

    @FXML
    private void moveUPButton() {
        int i = s().getSelectedIndex();
        if(useStartingMap && i == 1)return;
        if (i > 0) {
            Collections.swap(listOfEnvironments, i, i - 1);
            s().select(i - 1);
        }
    }

    @FXML
    void moveDOWNButton() {
        int i = s().getSelectedIndex();
        if(useStartingMap && i == 0) return;
        if (i < listOfEnvironments.size() - 1) {
            Collections.swap(listOfEnvironments, i, i + 1);
            s().select(i + 1);
        }
    }

    @FXML
    void lockEnvButton() {
        Boolean newValue = !s().getSelectedItem().isLocked();
        s().getSelectedItem().setLocked(newValue);
        settingsController.setDisabled(newValue);
        environmentListView.refresh();
    }

    @FXML
    void deleteEnvButton() {
        int i = s().getSelectedIndex();
        if(useStartingMap && i ==0)return;
        listOfEnvironments.remove(i);
    }

}
