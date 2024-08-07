/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import java.awt.*;
import java.io.File;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.IntStream;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.*;
import javafx.stage.Window;
import javafx.stage.FileChooser.ExtensionFilter;
import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.io.ViewSetReaderFromAgisoftXML;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.CanConfirm;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ShareInfo;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import kintsugi3d.util.RecentProjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomImportController extends FXMLPageController implements ShareInfo, CanConfirm
{
    private static final Logger log = LoggerFactory.getLogger(CustomImportController.class);
    @FXML private ChoiceBox<String> primaryViewChoiceBox;
    @FXML private Text loadCheckCameras;
    @FXML private Text loadCheckObj;
    @FXML private Text loadCheckImages;
    @FXML private VBox root;

    private Stage thisStage;

    private final FileChooser camFileChooser = new FileChooser();
    private final FileChooser objFileChooser = new FileChooser();
    private final DirectoryChooser photoDirectoryChooser = new DirectoryChooser();

    private File cameraFile;
    private File objFile;
    private File photoDir;

    @Override
    public Region getHostRegion() {
        return root;
    }

    public void init()
    {
        File recentFile = RecentProjects.getMostRecentDirectory();
        setInitDirectories(recentFile);

        camFileChooser.getExtensionFilters().add(new ExtensionFilter("Agisoft Metashape XML file", "*.xml"));
        objFileChooser.getExtensionFilters().add(new ExtensionFilter("Wavefront OBJ or PLY file", "*.obj", "*.ply"));

        camFileChooser.setTitle("Select camera positions file");
        objFileChooser.setTitle("Select object file");

        photoDirectoryChooser.setTitle("Select photo directory");
    }

    @Override
    public void refresh() {
        File recentFile = RecentProjects.getMostRecentDirectory();
        setInitDirectories(recentFile);
    }

    @Override
    public boolean isNextButtonValid() {
        return areAllFilesLoaded();
    }

    /**
     * Recursively chains together add calls to the dropdown, using Platform.runLater between each one
     * to avoid locking up the JavaFX Application thread
     * @param iterator
     */
    private void addToViewListRecursive(Iterator<String> iterator)
    {
        primaryViewChoiceBox.getItems().add(iterator.next());

        if (iterator.hasNext())
        {
            Platform.runLater(() -> addToViewListRecursive(iterator));
        }
        else
        {
            // Finished adding all the choices; select the first one by default and re-enable
            primaryViewChoiceBox.getSelectionModel().select(0);
            primaryViewChoiceBox.setDisable(false);
        }
    }

    @FXML
    private void camFileSelect()
    {

        File temp = camFileChooser.showOpenDialog(getStage());

        if (temp != null)
        {
            cameraFile = temp;
            setHomeDir(temp);

            try
            {
                ReadonlyViewSet newViewSet = ViewSetReaderFromAgisoftXML.getInstance().readFromFile(cameraFile);

                loadCheckCameras.setText("Loaded");
                loadCheckCameras.setFill(Paint.valueOf("Green"));

                primaryViewChoiceBox.getItems().clear();

                if (newViewSet.getCameraPoseCount() > 0)
                {
                    // Disable while updating the choices as it won't be responsive until it's done adding all the options
                    primaryViewChoiceBox.setDisable(true);
                    Iterator<String> imageIterator = IntStream.range(0, newViewSet.getCameraPoseCount())
                        .mapToObj(newViewSet::getImageFileName)
                        .sorted(Comparator.naturalOrder())
                        .iterator();

                    // Use individual Platform.runLater calls, chained together recursively
                    // to prevent locking up the JavaFX Application thread
                    Platform.runLater(() -> addToViewListRecursive(imageIterator));
                }
            }
            catch (Exception e)
            {
                log.error("An error occurred reading camera file:", e);
                new Alert(AlertType.ERROR, e.toString()).show();
            }
        }

        hostScrollerController.updatePrevAndNextButtons();
    }


    @FXML
    private void objFileSelect()
    {

        File temp = objFileChooser.showOpenDialog(getStage());

        if (temp != null)
        {
            objFile = temp;
            setHomeDir(temp);
            loadCheckObj.setText("Loaded");
            loadCheckObj.setFill(Paint.valueOf("Green"));
        }

        hostScrollerController.updatePrevAndNextButtons();
    }

    @FXML
    private void photoDirectorySelect()
    {

        File temp = photoDirectoryChooser.showDialog(getStage());

        if (temp != null)
        {
            photoDir = temp;
            setHomeDir(temp);
            loadCheckImages.setText("Loaded");
            loadCheckImages.setFill(Paint.valueOf("Green"));
        }

        hostScrollerController.updatePrevAndNextButtons();
    }

    private boolean areAllFilesLoaded() {
        return (cameraFile != null) && (objFile != null) && (photoDir != null);
    }


    private void close()
    {
        Window window = root.getScene().getWindow();
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void setHomeDir(File home)
    {
        File parentDir = home.getParentFile();

        setInitDirectories(parentDir);
    }

    private void setInitDirectories(File file){
        camFileChooser.setInitialDirectory(file);
        objFileChooser.setInitialDirectory(file);
        photoDirectoryChooser.setInitialDirectory(file);

        RecentProjects.setMostRecentDirectory(file);
    }

    private Stage getStage()
    {
        if (thisStage == null)
        {
            thisStage = (Stage) root.getScene().getWindow();
        }
        return thisStage;
    }

    @Override
    public void shareInfo() {
        hostScrollerController.addInfo(Info.CAM_FILE, cameraFile);
        hostScrollerController.addInfo(Info.PHOTO_DIR, photoDir);
        hostScrollerController.addInfo(Info.OBJ_FILE, objFile);
        hostScrollerController.addInfo(Info.PRIMARY_VIEW, primaryViewChoiceBox.getSelectionModel().getSelectedItem());
    }

    @Override
    public void confirmButtonPress() {
        if (!areAllFilesLoaded()){
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (loadStartCallback != null)
        {
            loadStartCallback.run();
        }

        if (viewSetCallback != null)
        {
            MultithreadModels.getInstance().getIOModel().addViewSetLoadCallback(
                    viewSet -> viewSetCallback.accept(viewSet));
        }

        new Thread(() ->
                MultithreadModels.getInstance().getIOModel().loadFromAgisoftFiles(
                        cameraFile.getPath(), cameraFile, objFile, photoDir,
                        primaryViewChoiceBox.getSelectionModel().getSelectedItem()))
                .start();

        WelcomeWindowController.getInstance().hide();
        close();
    }
}
