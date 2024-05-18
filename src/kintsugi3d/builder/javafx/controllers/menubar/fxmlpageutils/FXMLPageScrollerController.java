package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

public class FXMLPageScrollerController {
    private static final Logger log = LoggerFactory.getLogger(FXMLPageScrollerController.class);
    @FXML private Button prevButton;
    @FXML private Button nextButton;


    @FXML private AnchorPane hostAnchorPane;
    ArrayList<FXMLPage> pages;
    FXMLPage currentPage;

    HashMap<String, Object> sharedInfo;

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
            currentPage.getNextPage().setPrevPage(currentPage);
            currentPage = currentPage.getNextPage();

            initControllerAndUpdatePanel(nextPath);
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
        currentPage.getController().refresh();
    }

    private void updatePrevAndNextButtons() {
        nextButton.setDisable(!currentPage.hasNextPage());
        prevButton.setDisable(!currentPage.hasPrevPage());
    }

    public void setNextButtonDisable(boolean b) {
        nextButton.setDisable(b);
    }

    public <T> void addInfo(String key, T info){
        sharedInfo.put(key, info);
    }

    public <T> T getInfo(String key){
        return (T) sharedInfo.get(key);
    }
}

