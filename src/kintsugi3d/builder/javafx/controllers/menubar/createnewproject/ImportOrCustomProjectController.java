package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;

public class ImportOrCustomProjectController extends FXMLPageController {

    @FXML private AnchorPane anchorPane;

    @Override
    public Region getHostRegion() {
        return anchorPane;
    }

    @Override
    public void init() {
    }

    @Override
    public void refresh() {
    }

    public void metashapeImportSelect() {
        //stop other pages from using accelerators to access these methods
        //ideally, accelerators would be removed from the scene entirely instead of using this workaround
        //however, attempting to clear accelerators leads to a ConcurrentModificationException
        //hence, a "temporary" workaround
        if(!(hostScrollerController.getCurrentPage().getController() instanceof ImportOrCustomProjectController)){
            return;
        }
        String importMetashapeFXMLPath = "/fxml/menubar/createnewproject/MetashapeImport.fxml";
        openChildPage(importMetashapeFXMLPath);
    }

    public void customImportSelect() {
        if(!(hostScrollerController.getCurrentPage().getController() instanceof ImportOrCustomProjectController)){
            return;
        }
        String customImportFXMLPath = "/fxml/menubar/createnewproject/CustomImport.fxml";
        openChildPage(customImportFXMLPath);
    }

    @Override
    public void setButtonShortcuts(){
        super.setButtonShortcuts();

        KeyCombination chooseMetashapeCode = new KeyCodeCombination(KeyCode.W);

        KeyCombination chooseCustomCode = new KeyCodeCombination(KeyCode.S);

        //TODO: fix W and S accelerators being applied in pages where they shouldn't
        Scene scene = getHostRegion().getScene();
        scene.getAccelerators().put(chooseMetashapeCode, this::metashapeImportSelect);
        scene.getAccelerators().put(chooseCustomCode, this::customImportSelect);
    }
}
