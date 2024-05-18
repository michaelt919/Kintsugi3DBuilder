package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class FXMLPageScrollerController {
    private static final Logger log = LoggerFactory.getLogger(FXMLPageScrollerController.class);
    @FXML private Button prevButton;
    @FXML private Button nextButton;


    @FXML private AnchorPane hostAnchorPane;
    ArrayList<FXMLPage> pages;
    FXMLPage currentPage;

    public void init() {
        String fileName = currentPage.getFxmlFilePath();
        initControllerAndUpdatePanel(fileName);

        for (FXMLPage page : pages){
            page.getController().setHostScrollerController(this);
            page.getController().setHostPage(page);
        }
    }
    public void prevPage() {
        if (currentPage.hasPrevPage()){
            currentPage = currentPage.getPrevPage();
            initControllerAndUpdatePanel(currentPage.getFxmlFilePath());
        }
    }

    public void nextPage() {
        if (currentPage.hasNextPage()){
            String nextPath;
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

    private void initControllerAndUpdatePanel(String fileName) {
        FXMLPage newPage = getPage(fileName);
        Parent newContent = newPage.getLoader().getRoot();

        if (newContent != null) {
            hostAnchorPane.getChildren().setAll(newContent);
        }

        updatePrevAndNextButtons();
    }

    private void updatePrevAndNextButtons() {
        nextButton.setDisable(!currentPage.hasNextPage());
        prevButton.setDisable(!currentPage.hasPrevPage());
    }
}
