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

    private static final String NO_MODEL_ID_MSG = "No Model ID";
    private static final String NO_MODEL_NAME_MSG = "Unnamed Model";
    private volatile boolean alertShown = false;

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
        //need to do Platform.runLater so updateModelSelectionChoiceBox can pull info from chunkSelectionChoiceBox
        chunkSelectionChoiceBox.setOnAction(event -> Platform.runLater(this::updateModelSelectionChoiceBox));
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
        if (modelIDAsString.startsWith(NO_MODEL_ID_MSG)){return null;}

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

        modelSelectionChoiceBox.getItems().clear();
        for (Triplet<Integer, String, String> triplet : modelInfo){
            String modelID = triplet.first != null ? String.valueOf(triplet.first) : NO_MODEL_ID_MSG;
            String modelName = !triplet.second.isBlank() ? triplet.second : NO_MODEL_NAME_MSG;
            modelSelectionChoiceBox.getItems().add(modelID + "   " + modelName);
        }

        //initialize choice box to first option instead of null option
        if (hasModels()){
            modelSelectionChoiceBox.setValue(modelSelectionChoiceBox.getItems().get(0));
            modelSelectionChoiceBox.setDisable(false);
        }
        else{
            if (alertShown){return;} //prevent multiple alerts from showing at once

            alertShown = true;
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
                    alertShown = false;
                });

                ((ButtonBase) alert.getDialogPane().lookupButton(ok)).setOnAction(event -> alertShown = false);

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
