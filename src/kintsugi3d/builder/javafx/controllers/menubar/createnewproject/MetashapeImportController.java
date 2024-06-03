package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
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
import kintsugi3d.util.Triplet;

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
        return super.isNextButtonValid() && hasModels();
    }

    @Override
    public void shareInfo() {
        //update metashapeObjectChunk with selected chunk from chunkSelectionChoiceBox
        if (metashapeObjectChunk != null){
            MetashapeObject metashapeObject = metashapeObjectChunk.getMetashapeObject();
            String chunkName = (String) chunkSelectionChoiceBox.getSelectionModel().getSelectedItem();
            Integer modelID = getSelectedModelID();

            metashapeObjectChunk = new MetashapeObjectChunk(metashapeObject, chunkName, modelID);
        }

        hostScrollerController.addInfo(Info.METASHAPE_OBJ_CHUNK, metashapeObjectChunk);
    }

    private Integer getSelectedModelID() {
        String modelIDAsString = (String) modelSelectionChoiceBox.getSelectionModel().getSelectedItem();
        //TODO: need to revisit this when formatting of model selection choice box changes
        if (modelIDAsString.startsWith("null")){return null;}

        return Integer.parseInt(modelIDAsString.substring(0, modelIDAsString.indexOf(' ')));
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

        ArrayList<Triplet<Integer, String, String>> modelInfo = metashapeObjectChunk.getModelInfo();

        for (Triplet<Integer, String, String> triplet : modelInfo){
            modelSelectionChoiceBox.getItems().add(triplet.first + "   " + triplet.second);
        }

        //initialize choice box to first option instead of null option
        if (hasModels()){
            modelSelectionChoiceBox.setValue(modelSelectionChoiceBox.getItems().get(0));
            modelSelectionChoiceBox.setDisable(false);
        }
        else{
            Platform.runLater(() ->
            {
                ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                ButtonType openCustomProj = new ButtonType("Create Custom Project", ButtonBar.ButtonData.YES);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Metashape chunk has no models." +
                        "\nPlease select another chunk or create a custom project.", ok, openCustomProj);
                ((ButtonBase) alert.getDialogPane().lookupButton(openCustomProj)).setOnAction(event -> {
                    //manually navigate though pages to get to custom loader
                    hostScrollerController.prevPage();//go to ImportOrCustomProject.fxml
                    ImportOrCustomProjectController controller = (ImportOrCustomProjectController)
                            hostScrollerController.getCurrentPage().getController();
                    controller.customImportSelect();
                });
                alert.show();
            });
        }
    }

    private boolean hasModels() {
        return modelSelectionChoiceBox.getItems() != null &&
                !modelSelectionChoiceBox.getItems().isEmpty() &&
                modelSelectionChoiceBox.getItems().get(0) != null;
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
        if (isMetashapeObjectLoaded() && hasModels()) {//TODO: change condition to check for metashapeObjectChunk != null?
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
