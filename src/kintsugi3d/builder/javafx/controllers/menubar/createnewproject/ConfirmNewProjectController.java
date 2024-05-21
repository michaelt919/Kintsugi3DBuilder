package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;

public class ConfirmNewProjectController extends FXMLPageController {
    @FXML private AnchorPane anchorPane;
    @FXML private TextField projectNameTxtField;
    @FXML private TextField projectPathTxtField;

    @Override
    public Region getHostRegion() {
        return anchorPane;
    }

    @Override
    public void init() {

    }

    @Override
    public void refresh() {
    }

    @Override
    public boolean isNextButtonValid() {
        return areAllFieldsValid();
    }

    private boolean areAllFieldsValid() {
        //TODO: need more robust checking for file paths
        return !projectNameTxtField.getText().isEmpty() && !projectPathTxtField.getText().isEmpty();
    }

    public void chooseProjLocation(ActionEvent actionEvent) {
    }

    @FXML private void updateConfirmButton(KeyEvent actionEvent) {
        hostScrollerController.updatePrevAndNextButtons();
    }
}
