package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ShareInfo;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.function.BiConsumer;

public class ConfirmNewProjectController extends FXMLPageController {
    private static final Logger log = LoggerFactory.getLogger(ConfirmNewProjectController.class);

    @FXML private AnchorPane anchorPane;
    @FXML private TextField projectNameTxtField;
    @FXML private TextField projectPathTxtField;
    private DirectoryChooser directoryChooser;
    private File cameraFile;
    private File objFile;
    private File photoDir;

    private Runnable loadStartCallback;
    private BiConsumer<ViewSet, File> viewSetCallback;
    private String primaryView;
    private MetashapeObjectChunk metashapeObjectChunk;

    @Override
    public Region getHostRegion() {
        return anchorPane;
    }

    @Override
    public void init() {
        directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose Project Save Path");
    }

    @Override
    public void refresh() {
        if (super.hostPage.getPrevPage().getController() instanceof MetashapeImportController) {
            MetashapeObjectChunk metaChunk = hostScrollerController.getInfo(ShareInfo.Info.METASHAPE_OBJ_CHUNK);

            if (metaChunk != null) {
                File psxFile = new File(metaChunk.getPsxFilePath());
                String fileName = psxFile.getName();

                //remove the .psx file extension
                projectNameTxtField.setText(fileName.substring(0, fileName.length() - 4));
            }
        }
        else{
            objFile = hostScrollerController.getInfo(ShareInfo.Info.OBJ_FILE);

            if(objFile != null){
                String fileName = objFile.getName();

                //remove the .obj file extension
                projectNameTxtField.setText(fileName.substring(0, fileName.length() - 4));
            }
        }
    }

    @Override
    public boolean isNextButtonValid() {
        return areAllFieldsValid();
    }

    @FXML private void chooseProjLocation(ActionEvent actionEvent) {
        File directory = directoryChooser.showDialog(anchorPane.getScene().getWindow());

        if (directory != null){
            projectPathTxtField.setText(directory.getAbsolutePath());
        }
        updateConfirmButton(null);
    }

    @FXML private void updateConfirmButton(KeyEvent actionEvent) {
        hostScrollerController.updatePrevAndNextButtons();
    }

    public void setLoadStartCallback(Runnable callback)
    {
        this.loadStartCallback = callback;
    }

    public void setViewSetCallback(BiConsumer<ViewSet, File> callback)
    {
        this.viewSetCallback = callback;
    }

    public void confirmButtonPress(){
        refreshMetashapeInfo();
        if (!areAllFieldsValid()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }


        if (loadStartCallback != null) {
            loadStartCallback.run();
        }

        String projName = projectNameTxtField.getText();
        String projSaveLoc = projectPathTxtField.getText();

        //append ".k3d" to file name if it does not already exist

        if (!projName.endsWith(".k3d")){
            //remove suffix if it exists
            int index = projName.lastIndexOf('.');

            if (index != -1) {
                projName = projName.substring(0, projName.lastIndexOf('.'));
            }

            projName += ".k3d";
        }

        File saveLoc = new File(projSaveLoc);

        if (!saveLoc.exists()){
            Toolkit.getDefaultToolkit().beep();
            //TODO: make this more user friendly
            //add popup?
            log.error("Save file location does not exist.");
            return;
        }

        File toBeSaved = new File(saveLoc, projName);


        //loading from custom import
        if(super.hostPage.getPrevPage().getController() instanceof CustomImportController) {
            if (viewSetCallback != null) {
                MultithreadModels.getInstance().getLoadingModel().addViewSetLoadCallback(
                        viewSet -> viewSetCallback.accept(viewSet, toBeSaved));
            }
            new Thread(() ->
                    MultithreadModels.getInstance().getLoadingModel().loadFromAgisoftFiles(
                            cameraFile.getPath(), cameraFile, objFile, photoDir,
                            primaryView))
                    .start();
        }
        else{//loading from metashape import
            if (viewSetCallback != null) {
                MultithreadModels.getInstance().getLoadingModel().addViewSetLoadCallback(
                        viewSet ->viewSetCallback.accept(viewSet, toBeSaved));
            }
            new Thread(() ->
                    MultithreadModels.getInstance().getLoadingModel()
                            .loadAgisoftFromZIP(
                                    metashapeObjectChunk.getFramePath(),
                                    metashapeObjectChunk,
                                    primaryView))
                    .start();
        }
        WelcomeWindowController.getInstance().hideWelcomeWindow();
        close();
    }

    private void close() {
        Window window = anchorPane.getScene().getWindow();
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void refreshMetashapeInfo() {
        cameraFile = hostScrollerController.getInfo(ShareInfo.Info.CAM_FILE);
        photoDir = hostScrollerController.getInfo(ShareInfo.Info.PHOTO_DIR);
        objFile = hostScrollerController.getInfo(ShareInfo.Info.OBJ_FILE);
        primaryView = hostScrollerController.getInfo(ShareInfo.Info.PRIMARY_VIEW);
        metashapeObjectChunk = hostScrollerController.getInfo(ShareInfo.Info.METASHAPE_OBJ_CHUNK);
    }

    private boolean areAllFieldsValid() {
        //TODO: need more robust checking for file paths
        // move confirmButtonPress() path verifications to here?
        return !projectNameTxtField.getText().isEmpty() && !projectPathTxtField.getText().isEmpty();
    }
}
