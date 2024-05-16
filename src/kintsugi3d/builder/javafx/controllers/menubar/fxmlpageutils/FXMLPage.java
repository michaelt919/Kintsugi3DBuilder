package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

public class FXMLPage {
     String fxmlFilePath;

    FXMLPageController controller;

    public FXMLPage(String fxmlFile, FXMLPageController controller) {
        this.fxmlFilePath = fxmlFile;
        this.controller = controller;
    }

    public FXMLPageController getController() {
        return controller;
    }

    public String getFxmlFilePath(){
        return fxmlFilePath;
    }
}
