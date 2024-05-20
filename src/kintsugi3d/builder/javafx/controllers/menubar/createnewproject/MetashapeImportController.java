package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObject;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;

import java.io.File;
import java.util.ArrayList;

public class MetashapeImportController extends FXMLPageController {
    @FXML private AnchorPane anchorPane;
    @FXML private Text loadMetashapeObject;
    @FXML private ChoiceBox chunkSelectionChoiceBox;
    private Stage thisStage;
    private File metashapePsxFile;

    @Override
    public Region getHostRegion() {
        return anchorPane;
    }

    @Override
    public void init() {
    }

    @Override
    public void refresh() {
        updateLoadedIndicators();
    }

    @FXML
    private void psxFileSelect(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose .psx file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Metashape files", "*.psx"));

        Stage stage = (Stage) ((Node)actionEvent.getSource()).getScene().getWindow();
        metashapePsxFile = fileChooser.showOpenDialog(stage);

        if(isMetashapeObjectLoaded()){
            MetashapeObject metashapeObject = new MetashapeObject(metashapePsxFile.getAbsolutePath());
            initMetashapeObject(metashapeObject);

            //give metashape object info to fxml scroller controller
            hostScrollerController.addInfo("metashapeFile", metashapeObject);
        }
    }

    public void initMetashapeObjectChunk(MetashapeObjectChunk metashapeObjectChunk){
        initMetashapeObject(metashapeObjectChunk.getMetashapeObject());
        chunkSelectionChoiceBox.setValue(metashapeObjectChunk.getChunkName());
    }

    public void initMetashapeObject(MetashapeObject metashapeObject){

        this.metashapePsxFile = new File(metashapeObject.getPsxFilePath());

        //load chunks into chunk selection module
        ArrayList<String> chunkNames = (ArrayList<String>) metashapeObject.
                getChunkNamesDynamic(metashapeObject.getPsxFilePath());

        chunkSelectionChoiceBox.getItems().clear();
        chunkSelectionChoiceBox.getItems().addAll(chunkNames);

        chunkSelectionChoiceBox.setDisable(false);

        //initialize choice box to first option instead of null option
        if (chunkSelectionChoiceBox.getItems() != null &&
                chunkSelectionChoiceBox.getItems().get(0) != null){
            chunkSelectionChoiceBox.setValue(chunkSelectionChoiceBox.getItems().get(0));
        }

        updateLoadedIndicators();
    }

    private void updateLoadedIndicators() {
        if (isMetashapeObjectLoaded()) {
            loadMetashapeObject.setText("Loaded");
            loadMetashapeObject.setFill(Paint.valueOf("Green"));
            hostScrollerController.setNextButtonDisable(false);
            hostPage.setNextPage(hostScrollerController.getPage("fxml/menubar/createnewproject/ConfirmNewProject.fxml"));
        }
        else{
            loadMetashapeObject.setText("Unloaded");
            loadMetashapeObject.setFill(Paint.valueOf("Red"));
            hostScrollerController.setNextButtonDisable(true);
            hostPage.setNextPage(null);
        }
    }

    private boolean isMetashapeObjectLoaded() {
        return metashapePsxFile != null;
    }
    private Stage getStage()
    {
        if (thisStage == null)
        {
            thisStage = (Stage) anchorPane.getScene().getWindow();
        }
        return thisStage;
    }
}
