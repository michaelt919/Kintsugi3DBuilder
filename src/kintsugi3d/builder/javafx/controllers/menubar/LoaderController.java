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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
import kintsugi3d.util.RecentProjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class LoaderController implements Initializable
{
    private static final Logger log = LoggerFactory.getLogger(LoaderController.class);

    @FXML private ChoiceBox<String> primaryViewChoiceBox;
    @FXML private Text loadCheckCameras;
    @FXML private Text loadCheckObj;
    @FXML private Text loadCheckImages;
    @FXML private GridPane root;

    private Stage thisStage;

    private final FileChooser camFileChooser = new FileChooser();
    private final FileChooser objFileChooser = new FileChooser();
    private final DirectoryChooser photoDirectoryChooser = new DirectoryChooser();

    private File cameraFile;
    private File objFile;
    private File photoDir;

    private Runnable loadStartCallback;
    private Consumer<ViewSet> viewSetCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {

        setHomeDir(new File(System.getProperty("user.home")));
        camFileChooser.getExtensionFilters().add(new ExtensionFilter("Agisoft Metashape XML file", "*.xml"));
        objFileChooser.getExtensionFilters().add(new ExtensionFilter("Wavefront OBJ file", "*.obj"));

        camFileChooser.setTitle("Select camera positions file");
        objFileChooser.setTitle("Select object file");
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
        if ((cameraFile != null) && (objFile != null) && (photoDir != null))
        {
            if (loadStartCallback != null)
            {
                loadStartCallback.run();
            }

            if (viewSetCallback != null)
            {
                MultithreadModels.getInstance().getLoadingModel().addViewSetLoadCallback(viewSetCallback);
            }

            new Thread(() ->
                MultithreadModels.getInstance().getLoadingModel().loadFromAgisoftFiles(
                        cameraFile.getPath(), cameraFile, objFile, photoDir,
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
        List<String> items = RecentProjects.getItemsFromRecentsFile();

        if(items.isEmpty())
        {
            File parentDir;
            parentDir = home.getParentFile();
            camFileChooser.setInitialDirectory(parentDir);
            objFileChooser.setInitialDirectory(parentDir);
            photoDirectoryChooser.setInitialDirectory(parentDir);
        }
        else
        {
            for (int i = items.get(0).length()-1; i > 0; i--) {
                if (items.get(0).charAt(i) == '\\')
                {
//                    projectFileChooser.setInitialDirectory(new File(items.get(0).substring(0,i)));
                    camFileChooser.setInitialDirectory(new File(items.get(0).substring(0,i)));
                    objFileChooser.setInitialDirectory(new File(items.get(0).substring(0,i)));
                    photoDirectoryChooser.setInitialDirectory(new File(items.get(0).substring(0,i)));
                    break;
                }
            }
        }
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
}
