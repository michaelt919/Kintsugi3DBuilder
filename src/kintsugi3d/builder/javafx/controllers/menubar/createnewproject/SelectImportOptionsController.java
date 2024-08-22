package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;

public class SelectImportOptionsController extends FXMLPageController {

    @FXML private ToggleButton metashapeImportButton;
    @FXML private ToggleButton looseFilesImportButton;
    @FXML private ToggleButton realityCaptureImportButton;
    ToggleGroup buttons = new ToggleGroup();

    @FXML private AnchorPane anchorPane;

    @Override
    public Region getHostRegion() {
        return anchorPane;
    }

    @Override
    public void init() {
        buttons.getToggles().add(metashapeImportButton);
        buttons.getToggles().add(looseFilesImportButton);
        buttons.getToggles().add(realityCaptureImportButton);
    }

    @Override
    public void refresh() {
    }

    public void metashapeImportSelect() {
        if(metashapeImportButton.isSelected()){
            String importMetashapeFXMLPath = "/fxml/menubar/createnewproject/MetashapeImport.fxml";
            hostPage.setNextPage(hostScrollerController.getPage(importMetashapeFXMLPath));
            hostScrollerController.updatePrevAndNextButtons();
        }
        else{
            hostPage.setNextPage(null);
            hostScrollerController.updatePrevAndNextButtons();
        }
    }

    public void looseFilesSelect() {
        if(looseFilesImportButton.isSelected()){
            String customImportFXMLPath = "/fxml/menubar/createnewproject/CustomImport.fxml";
            hostPage.setNextPage(hostScrollerController.getPage(customImportFXMLPath));
            hostScrollerController.updatePrevAndNextButtons();
        }
        else{
            hostPage.setNextPage(null);
            hostScrollerController.updatePrevAndNextButtons();
        }
    }

    public void realityCaptureImportSelect() {
        if(realityCaptureImportButton.isSelected()){
            String customImportFXMLPath = "/fxml/menubar/createnewproject/CustomImport.fxml";
            hostPage.setNextPage(hostScrollerController.getPage(customImportFXMLPath));
            hostScrollerController.updatePrevAndNextButtons();
        }
        else{
            hostPage.setNextPage(null);
            hostScrollerController.updatePrevAndNextButtons();
        }
    }
}
