package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.event.ActionEvent;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;

public class ImportOrCustomProjectController extends FXMLPageController {

    @Override
    public void init() {
        hostScroller = getHostScrollerController();
    }

    @Override
    public void openChildPage(String childFXMLPath) {
        setNext(hostScroller.getPage(childFXMLPath));
        hostScroller.openNextPage();
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
