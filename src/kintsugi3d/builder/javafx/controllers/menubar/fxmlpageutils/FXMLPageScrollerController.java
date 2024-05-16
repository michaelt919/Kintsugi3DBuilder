package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import kintsugi3d.builder.javafx.controllers.menubar.systemsettings.AutosaveSettingsController;
import kintsugi3d.builder.javafx.controllers.menubar.systemsettings.SystemSettingsControllerBase;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ListIterator;

public class FXMLPageScrollerController {
    private static final Logger log = LoggerFactory.getLogger(FXMLPageScrollerController.class);
    @FXML private Button prevButton;
    @FXML private Button nextButton;

    @FXML private AnchorPane hostAnchorPane;
    ArrayList<FXMLPage> pages;

    int currentPageIndex;

    public void prevPage(ActionEvent actionEvent) {
        if (hasPrevIndex(currentPageIndex)){

            //previous() moves the iterator backward
            currentPageIndex--;
            initControllerAndUpdatePanel(pages.get(currentPageIndex).fxmlFilePath);
        }
    }

    public void nextPage(ActionEvent actionEvent) {
        if (hasNextIndex(currentPageIndex)){

            currentPageIndex++;
            initControllerAndUpdatePanel(pages.get(currentPageIndex).fxmlFilePath);
        }
    }

    public void setPages(ArrayList<FXMLPage> pages){
        this.pages = pages;
        this.currentPageIndex = 0;
    }

    public void init() {
        String fileName = pages.get(currentPageIndex).fxmlFilePath;

        initControllerAndUpdatePanel(fileName);
    }

    private void initControllerAndUpdatePanel(String fileName) {
        Parent newContent = null;
        try {
            URL url = MenubarController.class.getClassLoader().getResource(fileName);
            if (url == null)
            {
                throw new FileNotFoundException(fileName);
            }
            FXMLLoader loader = new FXMLLoader(url);
            newContent = loader.load();

            //initialize controller
            FXMLPageController controller = loader.getController();
            controller.init();

        } catch (Exception e) {
            log.error("Failed to load " + fileName, e);
        }

        if (newContent != null) {
            hostAnchorPane.getChildren().setAll(newContent);
        }

        updatePrevAndNextButtons();
    }

    private void updatePrevAndNextButtons() {
        nextButton.setDisable(!hasNextIndex(currentPageIndex));
        prevButton.setDisable(!hasPrevIndex(currentPageIndex));
    }

    private boolean hasNextIndex(int i){return i+1 < pages.size() && i+1 >=0;}
    private boolean hasPrevIndex(int i){return i-1 < pages.size() && i-1 >= 0;}

}
