package kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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

    @FXML private AnchorPane hostAnchorPane;
    ArrayList<FXMLPage> pages;
    ListIterator<FXMLPage> currentPage;

    public void prevPage(ActionEvent actionEvent) {
        if (currentPage.hasPrevious()){

            //previous() moves the iterator backward
            currentPage.previous().controller.initParent();
        }
    }

    public void nextPage(ActionEvent actionEvent) {
        if (currentPage.hasNext()){

            //next() moves the iterator forward
            currentPage.next().controller.initChild();
        }
    }

    public void setPages(ArrayList<FXMLPage> pages){
        this.pages = pages;
        this.currentPage = pages.listIterator();
    }

    public void init() {
        Parent newContent = null;
        String fileName = currentPage.next().fxmlFilePath;

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
    }
}
