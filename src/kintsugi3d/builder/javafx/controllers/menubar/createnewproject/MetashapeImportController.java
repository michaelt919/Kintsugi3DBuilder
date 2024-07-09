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
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObject;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.CanConfirm;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ShareInfo;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import kintsugi3d.util.RecentProjects;
import kintsugi3d.util.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class MetashapeImportController extends FXMLPageController implements ShareInfo, CanConfirm {
    private static final Logger log = LoggerFactory.getLogger(MetashapeImportController.class);

    @FXML private Text fileNameTxtField;
    @FXML private AnchorPane anchorPane;
    @FXML private Text loadMetashapeObject;

    @FXML private ChoiceBox chunkSelectionChoiceBox;
    @FXML private ChoiceBox modelSelectionChoiceBox;
    @FXML private ChoiceBox primaryViewChoiceBox;


    private File metashapePsxFile;
    private MetashapeObjectChunk metashapeObjectChunk;

    private static final String NO_MODEL_ID_MSG = "No Model ID";
    private static final String NO_MODEL_NAME_MSG = "Unnamed Model";
    private volatile boolean alertShown = false;

    FileChooser fileChooser;
    private volatile boolean updatingPrimaryView;

    @Override
    public Region getHostRegion() {
        return anchorPane;
    }

    @Override
    public void init() {
        fileChooser = new FileChooser();
        fileChooser.setTitle("Choose .psx file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Metashape files", "*.psx"));
        fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());
    }

    @Override
    public void refresh() {
        updateLoadedIndicators();
        //need to do Platform.runLater so updateModelSelectionChoiceBox can pull info from chunkSelectionChoiceBox
        chunkSelectionChoiceBox.setOnAction(event -> Platform.runLater(()->{
                updateModelSelectionChoiceBox();

                if (!updatingPrimaryView){
                    updatePrimaryViewChoiceBox();
                }
                updateLoadedIndicators();

        }));

        fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());
    }

    @Override
    public boolean isNextButtonValid() {
        return isMetashapeObjectLoaded() && hasModels();
    }

    @Override
    public void shareInfo() {
        //update metashapeObjectChunk with selected chunk from chunkSelectionChoiceBox
        updateMetashapeChunk();

        hostScrollerController.addInfo(Info.METASHAPE_OBJ_CHUNK, metashapeObjectChunk);
    }

    private void updateMetashapeChunk() {
        if (metashapeObjectChunk != null){
            MetashapeObject metashapeObject = metashapeObjectChunk.getMetashapeObject();
            String chunkName = (String) chunkSelectionChoiceBox.getSelectionModel().getSelectedItem();
            Integer modelID = getSelectedModelID();

            metashapeObjectChunk = new MetashapeObjectChunk(metashapeObject, chunkName, modelID);
        }
    }

    private Integer getSelectedModelID() {
        String selectionAsString = (String) modelSelectionChoiceBox.getSelectionModel().getSelectedItem();
        return getModelIDFromSelection(selectionAsString);
    }

    private static Integer getModelIDFromSelection(String selectionAsString) {
        //TODO: need to revisit this when formatting of model selection choice box changes
        if (selectionAsString.startsWith(NO_MODEL_ID_MSG)){
            return null;
        }

        return Integer.parseInt(selectionAsString.substring(0, selectionAsString.indexOf(' ')));
    }

    @FXML
    private void psxFileSelect(ActionEvent actionEvent) {
        Stage stage = (Stage) ((Node)actionEvent.getSource()).getScene().getWindow();
        metashapePsxFile = fileChooser.showOpenDialog(stage);

        if(metashapePsxFile != null){
            metashapeObjectChunk = null;
            fileNameTxtField.setText(metashapePsxFile.getName());
            updateChoiceBoxes();
            updateLoadedIndicators();

            RecentProjects.setMostRecentDirectory(metashapePsxFile.getParentFile());
            fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());
        }
    }

    private synchronized void updatePrimaryViewChoiceBox()
    {
        updatingPrimaryView = true;

        // Disable while updating the choices as it won't be responsive until it's done adding all the options
        primaryViewChoiceBox.setValue("Loading Views...");
        primaryViewChoiceBox.setDisable(true);
        primaryViewChoiceBox.getItems().clear();


        List<Element> cameras = metashapeObjectChunk.findEnabledCameras();
        if (!cameras.isEmpty())
        {
            Iterator<String> imageIterator = cameras.stream().map(camera -> camera.getAttribute("label"))
                    .sorted(Comparator.naturalOrder())
                    .iterator();


            // Use individual Platform.runLater calls, chained together recursively
            // to prevent locking up the JavaFX Application thread
            addToViewListRecursive(imageIterator);
        }
        else{
            //TODO: create warning alert because no enabled cameras were found
            primaryViewChoiceBox.setValue("No Enabled Cameras Found");
        }

    }

    private void addToViewListRecursive(Iterator<String> iterator)
    {
        primaryViewChoiceBox.getItems().add(iterator.next());

        if (iterator.hasNext())
        {
            Platform.runLater(() -> addToViewListRecursive(iterator));
        }
        else
        {
            primaryViewChoiceBox.getSelectionModel().select(0);
            primaryViewChoiceBox.setDisable(false);
            updatingPrimaryView = false;
        }
    }

    private void updateChoiceBoxes() {
        updateChunkSelectionChoiceBox();
        updateModelSelectionChoiceBox();
        updateLoadedIndicators();
    }

    private void updateModelSelectionChoiceBox() {
        modelSelectionChoiceBox.setDisable(true);

        if (metashapeObjectChunk != null){
            String chunkName = (String) chunkSelectionChoiceBox.getSelectionModel().getSelectedItem();
            metashapeObjectChunk.updateChunk(chunkName);
        }
        else{
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


        if (!hasModels()){
            showNoModelsAlert();
            return;
        }

        //initialize choice box to first option instead of null option
        //then set to default model if the chunk has one
        modelSelectionChoiceBox.setValue(modelSelectionChoiceBox.getItems().get(0));
        modelSelectionChoiceBox.setDisable(false);

        if (metashapeObjectChunk.getActiveModelID() == null){return;}

        for (int i = 0; i < modelSelectionChoiceBox.getItems().size(); ++i){
            Object obj = modelSelectionChoiceBox.getItems().get(i);
            Integer modelID = getModelIDFromSelection((String) obj);
            if (modelID == null){continue;}

            if (modelID.equals(metashapeObjectChunk.getActiveModelID())){
                modelSelectionChoiceBox.setValue(obj);
                break;
            }
        }

    }

    private void showNoModelsAlert() {
        if (alertShown){
            return;
        } //prevent multiple alerts from showing at once

        alertShown = true;
        Platform.runLater(() ->
        {
            ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            ButtonType openCustomProj = new ButtonType("Create Custom Project", ButtonBar.ButtonData.YES);

            Alert alert = new Alert(Alert.AlertType.NONE,"Please select another chunk or create a custom project.", ok, openCustomProj);

            ((ButtonBase) alert.getDialogPane().lookupButton(openCustomProj)).setOnAction(event -> {
                //manually navigate though pages to get to custom loader
                hostScrollerController.prevPage();//go to ImportOrCustomProject.fxml
                ImportOrCustomProjectController controller = (ImportOrCustomProjectController)
                        hostScrollerController.getCurrentPage().getController();
                controller.customImportSelect();
                alertShown = false;
            });

            ((ButtonBase) alert.getDialogPane().lookupButton(ok)).setOnAction(event -> alertShown = false);

            alert.setTitle("Metashape chunk has no models.");
            alert.show();
        });
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
        chunkSelectionChoiceBox.getItems().addAll(chunkNames);


        //initialize choice box to first option instead of null option
        if (chunkSelectionChoiceBox.getItems() != null &&
                chunkSelectionChoiceBox.getItems().get(0) != null){
            chunkSelectionChoiceBox.setValue(chunkSelectionChoiceBox.getItems().get(0));
            chunkSelectionChoiceBox.setDisable(false);

            //set chunk to default chunk if it has one
            Integer activeChunkID = metashapeObject.getActiveChunkID();
            if (activeChunkID != null){
                String chunkName = metashapeObject.getChunkNameFromID(activeChunkID);

                for (Object obj : chunkSelectionChoiceBox.getItems()){
                    String str = (String) obj;
                    if (str.equals(chunkName)){
                        chunkSelectionChoiceBox.setValue(obj);
                        break;
                    }
                }
            }
        }
    }

    private void updateLoadedIndicators() {
        if (isMetashapeObjectLoaded() && hasModels()) {//TODO: change condition to check for metashapeObjectChunk != null?
            loadMetashapeObject.setText("Loaded");
            loadMetashapeObject.setFill(Paint.valueOf("Green"));

            hostScrollerController.setNextButtonDisable(false);
        }
        else{
            loadMetashapeObject.setText("Unloaded");
            loadMetashapeObject.setFill(Paint.valueOf("Red"));

            hostScrollerController.setNextButtonDisable(true);
        }
    }

    private boolean isMetashapeObjectLoaded() {
        return metashapePsxFile != null;
    }

    @Override
    public void confirmButtonPress() {
        updateMetashapeChunk();
        if (loadStartCallback != null) {
            loadStartCallback.run();
        }

        if (viewSetCallback != null) {
            //"force" the user to save their project (user can still cancel saving)
            MultithreadModels.getInstance().getIOModel().addViewSetLoadCallback(
                    viewSet ->viewSetCallback.accept(viewSet));
        }

        new Thread(() ->
                MultithreadModels.getInstance().getIOModel()
                        .loadAgisoftFromZIP(
                                metashapeObjectChunk.getFramePath(),
                                metashapeObjectChunk,
                                primaryViewChoiceBox.getSelectionModel().getSelectedItem().toString()))
                .start();
        WelcomeWindowController.getInstance().hide();

        Window window = anchorPane.getScene().getWindow();
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }
}
