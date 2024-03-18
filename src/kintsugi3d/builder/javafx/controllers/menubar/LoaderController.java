/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.*;
import javafx.stage.Window;
import javafx.stage.FileChooser.ExtensionFilter;
import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.io.ViewSetReaderFromAgisoftXML;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LoaderController implements Initializable
{
    private static final Logger log = LoggerFactory.getLogger(LoaderController.class);

    @FXML private ChoiceBox<String> primaryViewChoiceBox;
    @FXML private Text loadCheckCameras;
    @FXML private Text loadCheckObj;
    @FXML private Text loadCheckImages;
    @FXML private Text loadCheckPLY;
    @FXML private Text loadCheckMeta;
    @FXML private GridPane root;

    private Stage thisStage;

    private final FileChooser camFileChooser = new FileChooser();
    private final FileChooser objFileChooser = new FileChooser();
    private final DirectoryChooser photoDirectoryChooser = new DirectoryChooser();
    private final FileChooser plyFileChooser = new FileChooser();
    private final FileChooser metaFileChooser = new FileChooser();


    private File cameraFile;
    private File objFile;
    private File photoDir;
    private File plyFile;
    private File metaFile;

    private Runnable loadStartCallback;
    private Consumer<ViewSet> viewSetCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {

        setHomeDir(new File(System.getProperty("user.home")));
        camFileChooser.getExtensionFilters().add(new ExtensionFilter("Agisoft Metashape XML file", "*.xml"));
        objFileChooser.getExtensionFilters().add(new ExtensionFilter("Wavefront OBJ file", "*.obj"));
        plyFileChooser.getExtensionFilters().add(new ExtensionFilter("PLY file", "*.ply"));
        metaFileChooser.getExtensionFilters().add(new ExtensionFilter("MetaShape project file", "*.psx"));


        camFileChooser.setTitle("Select camera positions file");
        objFileChooser.setTitle("Select object file");
        plyFileChooser.setTitle("Select ply file");
        metaFileChooser.setTitle("Select MetaShape project file");

        photoDirectoryChooser.setTitle("Select photo directory");


    }

    public void init()
    {
    }

    public void setLoadStartCallback(Runnable callback)
    {
        this.loadStartCallback = callback;
    }

    public void setViewSetCallback(Consumer<ViewSet> callback)
    {
        this.viewSetCallback = callback;
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
    }

    @FXML
    private void okButtonPress()
    {
        if(metaFile != null){
            if(!extractFromMetaShapeProject()) {
                log.warn("Failed to load from MetaShape Project");
                loadCheckMeta.setText("Unloaded");
                loadCheckMeta.setFill(Paint.valueOf("Red"));
                loadCheckImages.setText("Unloaded");
                loadCheckImages.setFill(Paint.valueOf("Red"));
                loadCheckCameras.setText("Unloaded");
                loadCheckCameras.setFill(Paint.valueOf("Red"));
                loadCheckPLY.setText("Unloaded");
                loadCheckPLY.setFill(Paint.valueOf("Red"));
            }
        }

        if ((cameraFile != null) && ((objFile != null) || (plyFile != null)) && (photoDir != null))
        {
            if (loadStartCallback != null)
            {
                loadStartCallback.run();
            }

            if (viewSetCallback != null)
            {
                MultithreadModels.getInstance().getLoadingModel().addViewSetLoadCallback(viewSetCallback);
            }
            File choosenFile;
            if(objFile != null){
                choosenFile = objFile;
            }else{
                choosenFile = plyFile;
            }
            new Thread(() ->
                MultithreadModels.getInstance().getLoadingModel().loadFromAgisoftFiles(
                        cameraFile.getPath(), cameraFile, choosenFile, photoDir,
                        primaryViewChoiceBox.getSelectionModel().getSelectedItem()))
                .start();

            close();
        }
        else{
            Toolkit.getDefaultToolkit().beep();
        }
    }

    @FXML
    private void cancelButtonPress()
    {
        close();
    }

    private void close()
    {
        Window window = root.getScene().getWindow();
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void setHomeDir(File home)
    {
        File parentDir;
        parentDir = home.getParentFile();
        camFileChooser.setInitialDirectory(parentDir);
        objFileChooser.setInitialDirectory(parentDir);
        photoDirectoryChooser.setInitialDirectory(parentDir);
    }

    private Stage getStage()
    {
        if (thisStage == null)
        {
            thisStage = (Stage) root.getScene().getWindow();
        }
        return thisStage;
    }

    private static final String QUICK_FILENAME = "quickSaveLoadConfig.txt";



    @FXML
    public void plySelect(ActionEvent actionEvent) {
        File temp = plyFileChooser.showOpenDialog(getStage());

        if (temp != null)
        {
            plyFile = temp;
            setHomeDir(temp);
            loadCheckPLY.setText("Loaded");
            loadCheckPLY.setFill(Paint.valueOf("Green"));
        }
    }

    @FXML
    public void metaSelect(ActionEvent actionEvent) throws IOException{
//        File temp = metaFileChooser.showOpenDialog(getStage());
//        // Get reference to the MetaShape file
//        if (temp != null) {
//            metaFile = temp;
//            setHomeDir(temp);
//            loadCheckMeta.setText("Meta Loaded");
//            loadCheckMeta.setFill(Paint.valueOf("Green"));
//        }


        //get FXML URLs
        String menuBarFXMLFileName = "fxml/menubar/MenuBar.fxml";
        URL menuBarURL = getClass().getClassLoader().getResource(menuBarFXMLFileName);
        assert menuBarURL != null : "cant find " + menuBarFXMLFileName;
        // Make a new Menu Bar Controller to handle the unzipping menu
        FXMLLoader menuBarFXMLLoader = new FXMLLoader(menuBarURL);
        Parent menuBarRoot = menuBarFXMLLoader.load();
        MenubarController menuBarController = menuBarFXMLLoader.getController();
        menuBarController.unzip();

    }

    /**
     * This function digs through the MetaShape project set in metaFile to look for the required files.
     * @return Returns whether it was successfully able to locate all files or not
     */
    public boolean extractFromMetaShapeProject(){
        // Null check MetaShape project file
        if(metaFile == null){
            log.error("No MetaShape project file found");
            return false;
        }
        File parentDirectory = metaFile.getParentFile();
        if(parentDirectory == null){
            log.error("Parent directory not set");
            return false;
        }
        // Getting the project name
        String fileName = metaFile.getName();
        String projectName;
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
             projectName = fileName.substring(0, lastDotIndex);
        }else{
            log.error("Unable to get project name");
            return false;
        }



        String pathToPhotoDir = projectName + ".files/0/0/thumbnails/thumbnails.zip";
        // Load Photo Directory
        //photoDir =

        // Load PLY file
        String pathToPly = projectName + ".files/0/0/model/model.zip";
        plyFile = extractFromZipFile(new File(parentDirectory,pathToPly), "mesh.ply");

        // Load camera positions
        String pathToCamerasFile = projectName + ".files/0/chunk.zip";
        cameraFile = extractFromZipFile(new File(parentDirectory,pathToCamerasFile), "doc.xml");

        // Return true if all files were successfully unzipped
        return (photoDir != null && plyFile != null && cameraFile != null);
    }

    /**
     * This function will extract a desired file from inside a zip folder
     * and create a temp file that gets deleted upon closing
     * @param zipFile the path of the zip file
     * @param embeddedFileName the name/path inside the zip file to the desired file
     * @return
     */
    public static File extractFromZipFile(File zipFile, String embeddedFileName) {
        File extractedFile = null;

        try (ZipFile zf = new ZipFile(zipFile)) {
            ZipEntry entry = zf.getEntry(embeddedFileName);
            if (entry != null && !entry.isDirectory()) {
                InputStream is = zf.getInputStream(entry);
                extractedFile = File.createTempFile("temp", ".ply");
                extractedFile.deleteOnExit(); // Delete the file when JVM exits
                FileOutputStream fos = new FileOutputStream(extractedFile);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.close();
                is.close();
            }
        } catch (IOException e) {
            log.error("An error occurred while attempting extract from zip file:", e);
        }

        return extractedFile;
    }
}
