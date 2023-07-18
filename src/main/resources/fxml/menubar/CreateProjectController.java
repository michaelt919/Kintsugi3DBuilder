package main.resources.fxml.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import tetzlaff.ibrelight.javafx.MultithreadModels;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class CreateProjectController {
    private Runnable callback;

    @FXML public TextField projectNameTxtField;
    @FXML public ChoiceBox<String> directoryChoices;

    @FXML public CheckBox imageCompressCheckbox;
    @FXML public CheckBox import3DOriginCheckbox;
    @FXML public CheckBox visibilityAndShadowTestingCheckbox;

    @FXML public TextField widthTextField;
    @FXML public TextField heightTextField;
    private DirectoryChooser directoryChooser = new DirectoryChooser();
    String defaultPath = "No default path yet";//TODO: WHAT SHOULD THIS BE?

    String defaultSelection = "Default Path: --> " + defaultPath;
    final static String CHOOSE_DIRECTORY = "Choose Directory...";

    public void init() {
        //initialize directory selection dropdown menu
        directoryChoices.getItems().addAll(defaultSelection, CHOOSE_DIRECTORY);

        //initialize option to default path
        directoryChoices.setValue(defaultSelection);

        //attach event handler (this cannot be done in scenebuilder)
        directoryChoices.setOnAction(this::handleDirectoryDropdownSelection);
    }

    private void handleDirectoryDropdownSelection(ActionEvent actionEvent) {
        //if user clicks "choose directory" option, open the directory chooser
        //then add an item to the dropdown which contains the path they selected

        if (directoryChoices.getValue().equals(CHOOSE_DIRECTORY)){
                this.directoryChooser.setTitle("Choose an output directory");

                Stage stage = (Stage) directoryChoices.getScene().getWindow();
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

    @FXML
    public void selectOutputDirectory() {

    }

    @FXML
    private void okButtonPress()
    {
//        if ((cameraFile != null) && (objFile != null) && (photoDir != null))
//        {
//            callback.run();
//
//            new Thread(() ->
//                    MultithreadModels.getInstance().getLoadingModel().loadFromAgisoftFiles(
//                            cameraFile.getPath(), cameraFile, objFile, photoDir,
//                            primaryViewChoiceBox.getSelectionModel().getSelectedItem()))
//                    .start();
//
//            close();
//        }
    }

    public void createProject() {
        //temporary testing measure: print all fields/info
        System.out.println("Project Name: " + projectNameTxtField.getText());
        System.out.println("Project Location: " + directoryChoices.getValue());
        System.out.println("Image compression? --> " + imageCompressCheckbox.isSelected());
        System.out.println("Use imported 3D origin? --> " + import3DOriginCheckbox.isSelected());

    }

    public void toggleVisibilitySectionDisabling() {
        //disable all options in Preload Visibility & Shadow Testing section if the corresponding checkbox is disabled
        widthTextField.setDisable(!visibilityAndShadowTestingCheckbox.isSelected());
        heightTextField.setDisable(!visibilityAndShadowTestingCheckbox.isSelected());
    }
}
