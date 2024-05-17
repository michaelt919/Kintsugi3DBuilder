package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

public abstract class FXMLPageController {

    FXMLPageScrollerController hostScroller;

    FXMLPage prev;
    FXMLPage next;

    public void setHostScrollerController(FXMLPageScrollerController scroller){this.hostScroller = scroller;}
    public FXMLPageScrollerController getHostScrollerController(){return hostScroller;}

    public FXMLPage getPrev() {return prev;}
    public boolean hasPrev(){return prev!= null;}

    public void setPrev(FXMLPage prev) {this.prev = prev;}

    public FXMLPage getNext(){return next;}
    public boolean hasNext(){return next!= null;}
    public void setNext(FXMLPage nextPage){this.next = nextPage;}

    public abstract void init();

    public abstract void initNext();
    //void sendInfoToParent();?
}
