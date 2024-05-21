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
import kintsugi3d.builder.javafx.controllers.menubar.LoaderController;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;

import java.awt.*;
import java.io.File;
import java.util.function.BiConsumer;

public class ConfirmNewProjectController extends FXMLPageController {
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
            MetashapeObjectChunk metaChunk = hostScrollerController.getInfo("metashapeObjChunk");

            if (metaChunk != null) {
                File psxFile = new File(metaChunk.getPsxFilePath());
                String fileName = psxFile.getName();

                //remove the .psx file extension
                projectNameTxtField.setText(fileName.substring(0, fileName.length() - 4));
            }
        }
        else{
            File objFile = hostScrollerController.getInfo("objFile");

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


        if(super.hostPage.getPrevPage().getController() instanceof LoaderController){
            if (loadStartCallback != null) {
                loadStartCallback.run();
            }

            if (viewSetCallback != null) {
                MultithreadModels.getInstance().getLoadingModel().addViewSetLoadCallback(
                        viewSet -> viewSetCallback.accept(viewSet, cameraFile.getParentFile()));
            }

            new Thread(() ->
                    MultithreadModels.getInstance().getLoadingModel().loadFromAgisoftFiles(
                            cameraFile.getPath(), cameraFile, objFile, photoDir,
                            primaryView))
                    .start();
            WelcomeWindowController.getInstance().hideWelcomeWindow();
            close();
        }

    }

    private void close() {
        Window window = anchorPane.getScene().getWindow();
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void refreshMetashapeInfo() {
        cameraFile = hostScrollerController.getInfo("camFile");
        photoDir = hostScrollerController.getInfo("photoDir");
        objFile = hostScrollerController.getInfo("objFile");
        primaryView = hostScrollerController.getInfo("primaryView");
    }

    private boolean areAllFieldsValid() {
        //TODO: need more robust checking for file paths
        return !projectNameTxtField.getText().isEmpty() && !projectPathTxtField.getText().isEmpty();
    }
}
