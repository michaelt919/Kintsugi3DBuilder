package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.LooseFilesInputSource;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.MetashapeProjectInputSource;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.RealityCaptureInputSource;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ShareInfo;

public class SelectImportOptionsController extends FXMLPageController {

    @FXML private ToggleButton metashapeImportButton;
    @FXML private ToggleButton looseFilesImportButton;
    @FXML private ToggleButton realityCaptureImportButton;
    ToggleGroup buttons = new ToggleGroup();

    @FXML private AnchorPane anchorPane;

    @Override
    public Region getHostRegion() {
        return anchorPane;
    }

    @Override
    public void init() {
        buttons.getToggles().add(metashapeImportButton);
        buttons.getToggles().add(looseFilesImportButton);
        buttons.getToggles().add(realityCaptureImportButton);

        metashapeImportButton.setOnAction(e -> handleButtonSelect(metashapeImportButton,
                "/fxml/menubar/createnewproject/MetashapeImport.fxml", new MetashapeProjectInputSource()));
        looseFilesImportButton.setOnAction(e-> handleButtonSelect(looseFilesImportButton,
                "/fxml/menubar/createnewproject/CustomImport.fxml", new LooseFilesInputSource()));

        realityCaptureImportButton.setOnAction(e -> handleButtonSelect(realityCaptureImportButton,
                "/fxml/menubar/createnewproject/CustomImport.fxml", new RealityCaptureInputSource()));

        buttons.selectedToggleProperty().addListener((a, b, c)->{
            resetFont(metashapeImportButton);
            resetFont(looseFilesImportButton);
            resetFont(realityCaptureImportButton);
        });
    }

    @Override
    public void refresh() {
    }

    public void handleButtonSelect(ToggleButton button, String path, InputSource source) {
        if (button.isSelected()) {
            hostPage.setNextPage(hostScrollerController.getPage(path));
            hostScrollerController.updatePrevAndNextButtons();
            hostScrollerController.addInfo(ShareInfo.Info.INPUT_SOURCE, source);
        } else {
            hostPage.setNextPage(null);
            hostScrollerController.updatePrevAndNextButtons();
        }
    }

    public void looseFilesSelect() {
        hostPage.setNextPage(hostScrollerController.getPage("/fxml/menubar/createnewproject/CustomImport.fxml"));
        hostScrollerController.nextPage();
    }

    private void resetFont(ToggleButton button){
        button.getStyleClass().clear();
        button.getStyleClass().add("toggle-button");
        button.getStyleClass().add(button.isSelected() ? "wireframeTitle" : "wireframeSubtitle");
    }
}
