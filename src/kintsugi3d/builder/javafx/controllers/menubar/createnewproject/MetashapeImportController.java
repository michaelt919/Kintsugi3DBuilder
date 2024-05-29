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
import javafx.util.Pair;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObject;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ShareInfo;

import java.io.File;
import java.util.ArrayList;

public class MetashapeImportController extends FXMLPageController implements ShareInfo {
    @FXML private AnchorPane anchorPane;
    @FXML private Text loadMetashapeObject;
    @FXML private ChoiceBox chunkSelectionChoiceBox;
    @FXML private ChoiceBox modelSelectionChoiceBox;

    private File metashapePsxFile;
    private MetashapeObjectChunk metashapeObjectChunk;

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

    @Override
    public boolean isNextButtonValid() {
        return super.isNextButtonValid() && isMetashapeObjectLoaded();
    }

    @Override
    public void shareInfo() {
        //update metashapeObjectChunk with selected chunk from chunkSelectionChoiceBox
        if (metashapeObjectChunk != null){
            MetashapeObject metashapeObject = metashapeObjectChunk.getMetashapeObject();
            String chunkName = (String) chunkSelectionChoiceBox.getSelectionModel().getSelectedItem();
            int modelID = getSelectedModelID();

            metashapeObjectChunk = new MetashapeObjectChunk(metashapeObject, chunkName, modelID);
        }

        hostScrollerController.addInfo(Info.METASHAPE_OBJ_CHUNK, metashapeObjectChunk);
    }

    private int getSelectedModelID() {
        String modelIDAsString = (String) modelSelectionChoiceBox.getSelectionModel().getSelectedItem();
        int modelID = Integer.parseInt(modelIDAsString.substring(0, modelIDAsString.indexOf(' ')));
        return modelID;
    }

    @FXML
    private void psxFileSelect(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose .psx file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Metashape files", "*.psx"));

        Stage stage = (Stage) ((Node)actionEvent.getSource()).getScene().getWindow();
        metashapePsxFile = fileChooser.showOpenDialog(stage);

        if(metashapePsxFile != null){
            metashapeObjectChunk = null;
            updateChoiceBoxes();
            updateLoadedIndicators();
        }
    }

    private void updateChoiceBoxes() {
        updateChunkSelectionChoiceBox();
        updateModelSelectionChoiceBox();
    }

    private void updateModelSelectionChoiceBox() {
        modelSelectionChoiceBox.setDisable(true);

        if (metashapeObjectChunk == null){
            MetashapeObject metashapeObject = new MetashapeObject(metashapePsxFile.getAbsolutePath());
            String chunkName = (String) chunkSelectionChoiceBox.getSelectionModel().getSelectedItem();
            int modelID = 0;

            metashapeObjectChunk = new MetashapeObjectChunk(metashapeObject, chunkName, modelID);
        }
        ArrayList<Pair<Integer, String>> modelNames = metashapeObjectChunk.getAllModelNames();

        modelSelectionChoiceBox.getItems().clear();

        for (Pair<Integer, String> pair : modelNames){
            modelSelectionChoiceBox.getItems().add(pair.getKey() + "   " + pair.getValue());
        }

        modelSelectionChoiceBox.setDisable(false);

        //initialize choice box to first option instead of null option
        if (modelSelectionChoiceBox.getItems() != null &&
                modelSelectionChoiceBox.getItems().get(0) != null){
            modelSelectionChoiceBox.setValue(modelSelectionChoiceBox.getItems().get(0));
            modelSelectionChoiceBox.setDisable(false);
        }
    }

    private void updateChunkSelectionChoiceBox() {
        chunkSelectionChoiceBox.setDisable(true);

        //load chunks into chunk selection module
        MetashapeObject metashapeObject = new MetashapeObject(metashapePsxFile.getPath());

        ArrayList<String> chunkNames = (ArrayList<String>) metashapeObject.
                getChunkNamesDynamic(metashapeObject.getPsxFilePath());

        chunkSelectionChoiceBox.getItems().clear();
        //TODO: implement recursive / platform.run later from LoaderController?
        //first attempt led to data dependency issues where the model selection choice box
        //   needed info that the chunk selection choice box didn't have yet
        chunkSelectionChoiceBox.getItems().addAll(chunkNames);


        //initialize choice box to first option instead of null option
        if (chunkSelectionChoiceBox.getItems() != null &&
                chunkSelectionChoiceBox.getItems().get(0) != null){
            chunkSelectionChoiceBox.setValue(chunkSelectionChoiceBox.getItems().get(0));
            chunkSelectionChoiceBox.setDisable(false);
        }
    }

    private void updateLoadedIndicators() {
        if (isMetashapeObjectLoaded()) {//TODO: change condition to check for metashapeObjectChunk != null?
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
}
