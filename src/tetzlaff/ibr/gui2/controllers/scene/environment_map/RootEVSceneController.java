package tetzlaff.ibr.gui2.controllers.scene.environment_map;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class RootEVSceneController implements Initializable{
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if(settingsController != null) System.out.println("EV controller linked");

        eVListView.setItems(listOfEVs);

        s().selectedItemProperty().addListener(settingsController.changeListener());

        EVSetting startingMap = new EVSetting(
                false,
                false,
                false,
                false,
                false,
                "",
                "",
                1.0,
                0.0,
                new Color(0.0,0.0,0.0,1.0),
                new Color(0,0,0,1),
                "first EV",
                false
        );

        EVSetting startingMap2 = new EVSetting(
                false,
                false,
                false,
                false,
                false,
                "",
                "",
                1.0,
                0.0,
                new Color(0.0,0.0,0.0,1.0),
                new Color(0,0,0,1),
                "second EV",
                false
        );

        listOfEVs.add(startingMap);
        listOfEVs.add(startingMap2);
        s().select(0);


    }

    private SelectionModel<EVSetting> s(){
        return eVListView.getSelectionModel();
    }



    @FXML
    void deleteCameraButton() {
       listOfEVs.remove(s().getSelectedIndex());
    }


}
