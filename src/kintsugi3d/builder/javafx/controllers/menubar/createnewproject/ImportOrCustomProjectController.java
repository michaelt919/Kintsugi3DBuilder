package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;

public class ImportOrCustomProjectController extends FXMLPageController {

    @FXML private Button metashapeImportButton;

    @FXML private AnchorPane anchorPane;

    @Override
    public Region getHostRegion() {
        return anchorPane;
    }

    @Override
    public void init() {
    }

    @Override
    public void refresh() {
        System.out.println(metashapeImportButton == null);
    }

    public void metashapeImportSelect(ActionEvent actionEvent) {
        String importMetashapeFXMLPath = "fxml/menubar/createnewproject/MetashapeImport.fxml";
        openChildPage(importMetashapeFXMLPath);
    }

    public void customImportSelect(ActionEvent actionEvent) {
        String customImportFXMLPath = "fxml/menubar/createnewproject/CustomImport.fxml";
        openChildPage(customImportFXMLPath);
    }
}
