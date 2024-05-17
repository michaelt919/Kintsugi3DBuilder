package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.event.ActionEvent;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPage;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageScrollerController;

public class ImportOrCustomProjectController extends FXMLPageController {

    @Override
    public void init() {
        hostScroller = getHostScrollerController();
    }

    @Override
    public void initNext() {

    }

    public void metashapeImportSelect(ActionEvent actionEvent) {
        String importMetashapeFXMLPath = "fxml/menubar/createnewproject/MetashapeImport.fxml";
        setNext(hostScroller.getPage(importMetashapeFXMLPath));
        hostScroller.openNextPage();
    }

    public void customImportSelect(ActionEvent actionEvent) {
    }
}
