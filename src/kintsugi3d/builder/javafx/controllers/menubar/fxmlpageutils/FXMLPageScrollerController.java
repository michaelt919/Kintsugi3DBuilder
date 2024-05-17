package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class FXMLPageScrollerController {
    private static final Logger log = LoggerFactory.getLogger(FXMLPageScrollerController.class);
    @FXML private Button prevButton;
    @FXML private Button nextButton;


    @FXML private AnchorPane hostAnchorPane;
    ArrayList<FXMLPage> pages;
    FXMLPage currentPage;


    public void prevPage(ActionEvent actionEvent) {
        if (currentPage.hasPrevPage()){
            currentPage = currentPage.getPrevPage();
            initControllerAndUpdatePanel(currentPage.getFxmlFilePath());
        }
    }

    public void nextPage(ActionEvent actionEvent) {
        if (currentPage.hasNextPage()){
            currentPage = currentPage.getNextPage();
            initControllerAndUpdatePanel(currentPage.getFxmlFilePath());
        }
    }

    public void setPages(ArrayList<FXMLPage> pages, String firstPageFXMLPath){
        this.pages = pages;
        currentPage = getPage(firstPageFXMLPath);
    }

    public FXMLPage getPage(String firstPageFXMLPath) {
        for (FXMLPage page: pages){
            if (page.getFxmlFilePath().equals(firstPageFXMLPath)){
                return page;
            }
        }
        return null;
    }

    public void init() {
        String fileName = currentPage.getFxmlFilePath();
        initControllerAndUpdatePanel(fileName);

        for (FXMLPage page : pages){
            page.getController().setHostScrollerController(this);
        }
    }

    private void initControllerAndUpdatePanel(String fileName) {
        FXMLPage newPage = getPage(fileName);
        Parent newContent = newPage.getLoader().getRoot();

        if (newContent != null) {
            hostAnchorPane.getChildren().setAll(newContent);
        }

        updatePrevAndNextButtons();
    }

    public void openNextPage(){
        String nextPath = null;
        try{
            nextPath = currentPage.getNextPage().getFxmlFilePath();
        }
        catch(NullPointerException e){
            log.error("Failed to load next page", e);
            return;
        }
        initControllerAndUpdatePanel(nextPath);
        currentPage.getNextPage().setPrevPage(currentPage);
        currentPage = currentPage.getNextPage();

        updatePrevAndNextButtons();
    }

    public AnchorPane getHostAnchorPane() {
        return hostAnchorPane;
    }

    private void updatePrevAndNextButtons() {
        nextButton.setDisable(!currentPage.hasNextPage());
        prevButton.setDisable(!currentPage.hasPrevPage());
    }

    public FXMLPage getCurrentPage() {
        return currentPage;
    }
}
