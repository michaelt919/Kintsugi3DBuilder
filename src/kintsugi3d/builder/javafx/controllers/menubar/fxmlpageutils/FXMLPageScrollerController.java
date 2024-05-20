package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FXMLPageScrollerController {
    private static final Logger log = LoggerFactory.getLogger(FXMLPageScrollerController.class);
    @FXML private GridPane outerGridPane;
    @FXML private Button prevButton;
    @FXML private Button nextButton;


    @FXML private AnchorPane hostAnchorPane;
    ArrayList<FXMLPage> pages;
    FXMLPage currentPage;

    HashMap<String, Object> sharedInfo;

    public void init() {
        for (FXMLPage page : pages){
            page.getController().setHostScrollerController(this);
            page.getController().setHostPage(page);
        }

        String fileName = currentPage.getFxmlFilePath();
        initControllerAndUpdatePanel(fileName);

        sharedInfo = new HashMap<>();
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

    public FXMLPage getPage(String fxmlPath) {
        for (FXMLPage page: pages){
            if (page.getFxmlFilePath().equals(fxmlPath)){
                return page;
            }
        }
        return null;
    }

    private void initControllerAndUpdatePanel(String fileName) {
        FXMLPage newPage = getPage(fileName);
        Parent newContent = newPage.getLoader().getRoot();

        updateSizePreferences();

        if (newContent != null) {
            hostAnchorPane.getChildren().setAll(newContent);
        }
        currentPage.getController().refresh();
        updatePrevAndNextButtons();
    }

    private void updateSizePreferences() {
        Pair<Double, Double> prefs = currentPage.getController().getSizePreferences();

        hostAnchorPane.setMinSize(prefs.getKey(), prefs.getValue());
        hostAnchorPane.setPrefSize(prefs.getKey(), prefs.getValue());

        final double EXTRA_HEIGHT = 1.2; // allow some extra breathing room for the prev and next buttons
        outerGridPane.setMinSize(prefs.getKey(), prefs.getValue() * EXTRA_HEIGHT);
        outerGridPane.setPrefSize(prefs.getKey(), prefs.getValue() * EXTRA_HEIGHT);

        Stage stage = (Stage) outerGridPane.getScene().getWindow();

        stage.setWidth(prefs.getKey() + stage.getScene().getWidth() - outerGridPane.getWidth());
        stage.setHeight((prefs.getValue() * EXTRA_HEIGHT) + stage.getScene().getHeight() - outerGridPane.getHeight());
    }


    public void updatePrevAndNextButtons() {
        prevButton.setDisable(!currentPage.hasPrevPage());
        nextButton.setDisable(!currentPage.getController().isNextButtonValid());
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

    public void dumpInfo() {
        for (Map.Entry<String, ?> entry : sharedInfo.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }
    }
}

