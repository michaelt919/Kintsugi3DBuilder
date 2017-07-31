package tetzlaff.ibr.gui2.controllers.scene.environment_map;

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

import com.sun.javafx.collections.ObservableListWrapper;
import tetzlaff.ibr.rendering2.EnvironmentMapModel3;

public class RootEVSceneController implements Initializable {
    private ObservableList<EVSetting> listOfEVs = new ObservableListWrapper<>(new ArrayList<>());
    @FXML
    private VBox settings;
    @FXML
    private SettingsEVSceneController settingsController;
    @FXML
    private VBox listControls;
    @FXML
    private ListView<EVSetting> eVListView;
    @FXML
    private Button theRenameButton;

    private final static boolean hasStatingMap = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //if (settingsController != null) System.out.println("EV controller linked");

        eVListView.setItems(listOfEVs);

        s().selectedItemProperty().addListener(settingsController.changeListener);

        if(hasStatingMap) {
            EVSetting startingMap = new EVSetting(
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
                    false
            );


            listOfEVs.add(startingMap);
            s().select(0);
        }


    }

    public void init2(EnvironmentMapModel3 environmentMapModel3){
        environmentMapModel3.setSelected(s().selectedItemProperty());
    }

    private SelectionModel<EVSetting> s() {
        return eVListView.getSelectionModel();
    }


    @FXML
    private void newEVButton() {
        if(s().getSelectedItem() == null){
            listOfEVs.add(
                    new EVSetting(
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
                            "New EV",
                            false
                    )
            );
        } else {
            listOfEVs.add(s().getSelectedItem().duplicate());
            s().select(listOfEVs.size() - 1);
        }
    }

    @FXML
    private void saveEVButton() {
        System.out.println("TODO: saved " + s().getSelectedItem() + " to the library.");
    }

    @FXML
    private void renameEVButton() {
        if (hasStatingMap && s().getSelectedIndex() == 0) return;

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

                eVListView.refresh();

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
        if(hasStatingMap && i == 1)return;
        if (i > 0) {
            Collections.swap(listOfEVs, i, i - 1);
            s().select(i - 1);
        }
    }

    @FXML
    void moveDOWNButton() {
        int i = s().getSelectedIndex();
        if(hasStatingMap && i == 0) return;
        if (i < listOfEVs.size() - 1) {
            Collections.swap(listOfEVs, i, i + 1);
            s().select(i + 1);
        }
    }

    @FXML
    void lockEVButton() {
        Boolean newValue = !s().getSelectedItem().isLocked();
        s().getSelectedItem().setLocked(newValue);
        settingsController.setDisabled(newValue);
        eVListView.refresh();
    }

    @FXML
    void deleteEVButton() {
        int i = s().getSelectedIndex();
        if(hasStatingMap && i ==0)return;
        listOfEVs.remove(i);
    }

}
