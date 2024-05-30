package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

import javafx.fxml.FXMLLoader;

public class FXMLPage {
    private String fxmlFilePath;

    private FXMLPageController controller;

    private FXMLLoader loader;

    private FXMLPage prev;
    private FXMLPage next;

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

    public FXMLPage getPrevPage(){return prev;}
    public FXMLPage getNextPage(){return next;}

    public boolean hasNextPage(){return next != null;}
    public boolean hasPrevPage(){return prev != null;}

    public void setPrevPage(FXMLPage page) {prev = page;}

    public void setNextPage(FXMLPage page) {next = page;}
}
