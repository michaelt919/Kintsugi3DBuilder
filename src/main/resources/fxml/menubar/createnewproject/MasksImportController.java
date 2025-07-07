package main.resources.fxml.menubar.createnewproject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ShareInfo;

import java.io.File;

public class MasksImportController extends FXMLPageController implements ShareInfo{
    @FXML private AnchorPane anchorPane;

    @FXML private ToggleButton useProjectMasksButton;
    @FXML private ToggleButton newMasksDirButton;
    @FXML private ToggleButton noMasksButton;
    private ToggleGroup toggleGroup;

    private DirectoryChooser masksDirectoryChooser;

    private InputSource source;
    private File masksDir; //represents the file chosen through file chooser, which may or may not be the final masks selection

    @Override
    public Region getHostRegion() {
        return anchorPane;
    }

    @Override
    public void init() {
        masksDirectoryChooser = new DirectoryChooser();
        toggleGroup = new ToggleGroup();
        toggleGroup.getToggles().add(useProjectMasksButton);
        toggleGroup.getToggles().add(newMasksDirButton);
        toggleGroup.getToggles().add(noMasksButton);

        useProjectMasksButton.setOnAction(e -> hostScrollerController.updatePrevAndNextButtons());
        newMasksDirButton.setOnAction(e -> hostScrollerController.updatePrevAndNextButtons());
        noMasksButton.setOnAction(e -> hostScrollerController.updatePrevAndNextButtons());

        hostPage.setNextPage(hostScrollerController.getPage("/fxml/menubar/createnewproject/PrimaryViewSelect.fxml"));
    }

    @Override
    public void refresh() {
        source = getHostScrollerController().getInfo(ShareInfo.Info.INPUT_SOURCE);
        masksDirectoryChooser.setInitialDirectory(source.getInitialMasksDirectory());
        useProjectMasksButton.setDisable(!source.doEnableProjectMasksButton());
    }

    @Override
    public boolean isNextButtonValid() {
        return toggleGroup.getToggles().stream().anyMatch(Toggle::isSelected);
    }

    @Override
    public void shareInfo() {
        //TODO
    }

    @FXML
    private void chooseMasksDir(ActionEvent e) {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        File file = masksDirectoryChooser.showDialog(stage);
        if (file!= null){
            masksDirectoryChooser.setInitialDirectory(file);
            masksDir = file;
        }
    }
}
