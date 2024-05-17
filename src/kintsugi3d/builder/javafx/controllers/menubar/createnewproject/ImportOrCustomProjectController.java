package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.event.ActionEvent;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPage;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;

public class ImportOrCustomProjectController extends FXMLPageController {

    FXMLPage parentPage;
    @Override
    public void init() {

    }

    @Override
    public void initNext() {

    }

    public void metashapeImportSelect(ActionEvent actionEvent) {
        String importMetashapeFXMLPath = "fxml/menubar/createnewproject/MetashapeImport.fxml";
        setNext(getHostScrollerController().getPage(importMetashapeFXMLPath));
        getHostScrollerController().openNextPage();
    }

    public void customImportSelect(ActionEvent actionEvent) {
    }
}
