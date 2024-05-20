package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

import javafx.scene.layout.Region;
import javafx.util.Pair;

public abstract class FXMLPageController {

    protected FXMLPageScrollerController hostScrollerController;
    protected FXMLPage hostPage;

    public void setHostScrollerController(FXMLPageScrollerController scroller){this.hostScrollerController = scroller;}
    public FXMLPageScrollerController getHostScrollerController(){return hostScrollerController;}

    public void setHostPage(FXMLPage page){this.hostPage = page;}
    public FXMLPage getHostPage(){return this.hostPage;}

    public abstract Region getHostRegion(); //returns the outer anchorpane, vbox, gridpane, etc. for the controller's fxml

    public abstract void init();

    public abstract void refresh();

    public void openChildPage(String childFXMLPath) {
        hostPage.setNextPage(hostScrollerController.getPage(childFXMLPath));
        hostScrollerController.nextPage();
    }

    public Pair<Double, Double> getSizePreferences(){
        Region hostNode = getHostRegion();

        return new Pair<>(hostNode.getPrefWidth(), hostNode.getPrefHeight());
    }
}
