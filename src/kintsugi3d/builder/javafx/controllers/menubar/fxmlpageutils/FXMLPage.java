package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

import javafx.fxml.FXMLLoader;

public class FXMLPage {
    private String fxmlFilePath;

    private FXMLPageController controller;

    private FXMLLoader loader;

    public FXMLPage(String fxmlFile, FXMLLoader loader) {
        this.fxmlFilePath = fxmlFile;
        this.loader = loader;
        this.controller = loader.getController();
    }

    public FXMLPageController getController() {return controller;}
    public FXMLLoader getLoader() {return loader;}

    public String getFxmlFilePath(){
        return fxmlFilePath;
    }

    public FXMLPage getPrevPage(){return controller.getPrev();}
    public FXMLPage getNextPage(){return controller.getNext();}

    public boolean hasNextPage(){return controller.hasNext();}
    public boolean hasPrevPage(){return controller.hasPrev();}
}
