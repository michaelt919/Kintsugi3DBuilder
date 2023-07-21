package main.resources.fxml.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.Toolkit;
import java.io.File;

public class CreateProjectController {
    private Runnable callback;

    @FXML public TextField projectNameTxtField;
    @FXML public ChoiceBox<String> directoryChoices;

    @FXML public CheckBox import3DOriginCheckbox;
    @FXML public CheckBox spatialOrientation3DOriginCheckbox;
    @FXML public CheckBox importedTransparencyCheckbox;
    @FXML public CheckBox autosaveCheckbox;

    private Stage stage;

    private DirectoryChooser directoryChooser = new DirectoryChooser();
    String defaultPath = "C:\\";//TODO: WILL CHANGE WHEN FILE STRUCTURE IS CEMENTED

    String defaultSelection = "Default Path: --> " + defaultPath;
    static final String CHOOSE_LOCATION = "Choose Location...";

    public void init() {
        //initialize directory selection dropdown menu
        directoryChoices.getItems().addAll(defaultSelection, CHOOSE_LOCATION);

        //initialize option to default path
        directoryChoices.setValue(defaultSelection);

        //attach event handler (this cannot be done in scenebuilder)
        directoryChoices.setOnAction(this::handleDirectoryDropdownSelection);

        stage = (Stage) directoryChoices.getScene().getWindow();
    }

    private void handleDirectoryDropdownSelection(ActionEvent actionEvent) {
        //if user clicks "choose directory" option, open the directory chooser
        //then add an item to the dropdown which contains the path they selected

        if (directoryChoices.getValue().equals(CHOOSE_LOCATION)){
                this.directoryChooser.setTitle("Choose an output directory");

                File file = this.directoryChooser.showDialog(stage.getOwner());

                if (file != null && file.exists()){
                    directoryChooser.setInitialDirectory(file);
                    directoryChoices.getItems().add(file.getAbsolutePath());
                    directoryChoices.setValue(file.getAbsolutePath());
                }
                else{
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        }

    public void setCallback(Runnable callback)
    {
        this.callback = callback;
    }

    public void createProject() {
        //temporary testing measure: print all fields/info
        System.out.println("Project Name: " + projectNameTxtField.getText());
        System.out.println("Project Location: " + directoryChoices.getValue());

        System.out.println("Use imported 3D origin? --> " + import3DOriginCheckbox.isSelected());
        System.out.println("Spatial orientation origin point as 3D origin? --> " + spatialOrientation3DOriginCheckbox.isSelected());

        System.out.println("Import transparency? --> " + importedTransparencyCheckbox.isSelected());

        System.out.println("Autosave? --> " + autosaveCheckbox.isSelected());

        //TODO: PUT RESOURCE IMPACT ALL IN ONE SECTION?
        //TODO: ADD UPDATE RECENT FILES

    }

//    private boolean areIntFieldsValid(){
//        for(TextField txtField : intTxtFields) {
//            if (txtField == null ||
//                    txtField.getText() == null ||
//                    !txtField.getText().matches("-?\\d+")) {//regex to check if input is integer
//                return false;
//            }
//        }
//        return true;
//    }

    @FXML
    public void cancelButtonAction()
    {
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }
}
