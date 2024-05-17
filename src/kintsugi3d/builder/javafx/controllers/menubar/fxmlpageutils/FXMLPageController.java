package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

public abstract class FXMLPageController {

    protected FXMLPageScrollerController hostScrollerController;
    protected FXMLPage hostPage;

    public void setHostScrollerController(FXMLPageScrollerController scroller){this.hostScrollerController = scroller;}
    public FXMLPageScrollerController getHostScrollerController(){return hostScrollerController;}

    public void setHostPage(FXMLPage page){this.hostPage = page;}
    public FXMLPage getHostPage(){return this.hostPage;}

    public abstract void init();

    public abstract void openChildPage(String childFXMLPath);
    //void sendInfoToParent();?
}
