package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;

import java.io.File;

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
        if (super.hostPage.getPrevPage().getController() instanceof MetashapeImportController) {
            MetashapeObjectChunk metaChunk = hostScrollerController.getInfo("metashapeObjChunk");

            if (metaChunk != null) {
                File psxFile = new File(metaChunk.getPsxFilePath());
                String fileName = psxFile.getName();

                //remove the .psx file extension
                projectNameTxtField.setText(fileName.substring(0, fileName.length() - 4));
            }
        }
        else{
            //TODO: imp text field initialization from this path (loader controller)
            projectNameTxtField.setText("");
        }
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
