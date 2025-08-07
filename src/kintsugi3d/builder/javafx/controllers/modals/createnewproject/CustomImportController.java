/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.modals.createnewproject;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.LooseFilesInputSource;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.RealityCaptureInputSource;
import kintsugi3d.builder.javafx.controllers.paged.DataTransformerPageControllerBase;
import kintsugi3d.builder.javafx.controllers.paged.SimpleDataReceiverPage;
import kintsugi3d.util.RecentProjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class CustomImportController extends DataTransformerPageControllerBase<InputSource, InputSource>
{
    private static final Logger log = LoggerFactory.getLogger(CustomImportController.class);

    @FXML private Text loadCheckCameras;
    @FXML private Text loadCheckObj;
    @FXML private Text loadCheckImages;
    @FXML private VBox root;
    @FXML private Text camPositionsTxt;
    @FXML private CheckBox undistortImagesCheckBox;

    private Stage thisStage;

    private final FileChooser camFileChooser = new FileChooser();
    private final FileChooser objFileChooser = new FileChooser();
    private final DirectoryChooser photoDirectoryChooser = new DirectoryChooser();

    private File cameraFile;
    private File meshFile;
    private File photoDir;

    private InputSource source;

    /**
     * Overridden by child class to enable hot swap
     *
     * @return
     */
    protected boolean shouldHotSwap()
    {
        return false;
    }

    @Override
    public Region getRootNode()
    {
        return root;
    }

    @Override
    public void init()
    {
        File recentFile = RecentProjects.getMostRecentDirectory();
        setInitDirectories(recentFile);

        camFileChooser.getExtensionFilters().add(new ExtensionFilter("Reality Capture CSV file", "*.csv"));
        camFileChooser.getExtensionFilters().add(new ExtensionFilter("Agisoft Metashape XML file", "*.xml"));
        objFileChooser.getExtensionFilters().add(new ExtensionFilter("Wavefront OBJ file", "*.obj"));
        objFileChooser.getExtensionFilters().add(new ExtensionFilter("Stanford PLY file", "*.ply"));

        camFileChooser.setTitle("Select camera positions file");
        objFileChooser.setTitle("Select object file");

        photoDirectoryChooser.setTitle("Select photo directory");

        this.getPage().setNextPage(getPageFrameController().createPage(
            "/fxml/modals/createnewproject/MasksImport.fxml",
            SimpleDataReceiverPage<InputSource>::new));

        setCanConfirm(true);
    }

    @Override
    public void refresh()
    {
        File recentFile = RecentProjects.getMostRecentDirectory();
        setInitDirectories(recentFile);

        if (source != null)
        {
            camFileChooser.getExtensionFilters().clear();

            StringBuilder cameraPositionsTextBuilder = new StringBuilder("Camera Positions\n(");
            for (ExtensionFilter filter : source.getExtensionFilters())
            {
                camFileChooser.getExtensionFilters().add(filter);
                cameraPositionsTextBuilder.append(filter.getDescription()).append(" or\n");
            }
            int finalIdx = cameraPositionsTextBuilder.length();
            cameraPositionsTextBuilder.replace(finalIdx - 4, finalIdx, ")");
            camPositionsTxt.setText(cameraPositionsTextBuilder.toString());
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
            loadCheckCameras.setText("Loaded");
            loadCheckCameras.setFill(Paint.valueOf("Green"));
        }

        if (areAllFilesLoaded())
        {
            setCanAdvance(true);
        }
    }


    @FXML
    private void objFileSelect()
    {

        File temp = objFileChooser.showOpenDialog(getStage());

        if (temp != null)
        {
            meshFile = temp;
            setHomeDir(temp);
            loadCheckObj.setText("Loaded");
            loadCheckObj.setFill(Paint.valueOf("Green"));
        }

        if (areAllFilesLoaded())
        {
            setCanAdvance(true);
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

        if (areAllFilesLoaded())
        {
            setCanAdvance(true);
        }
    }

    private boolean areAllFilesLoaded()
    {
        return (cameraFile != null) && (meshFile != null) && (photoDir != null);
    }


    private void setHomeDir(File home)
    {
        File parentDir = home.getParentFile();

        setInitDirectories(parentDir);
    }

    private void setInitDirectories(File file)
    {
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
    public boolean advance()
    {
        if (shouldHotSwap())
        {
            this.getPage().setOutData(
                new LooseFilesInputSource().setCameraFile(cameraFile)
                    .setMeshFile(meshFile)
                    .setPhotosDir(photoDir, undistortImagesCheckBox.isSelected())
                    .setHotSwap(true));
        }
        else
        {
            //overwrite old source so we can compare old and new versions in PrimaryViewSelectController
            if (source instanceof RealityCaptureInputSource)
            {
                this.getPage().setOutData(
                    new RealityCaptureInputSource()
                        .setCameraFile(cameraFile)
                        .setMeshFile(meshFile)
                        .setPhotosDir(photoDir, undistortImagesCheckBox.isSelected()));
            }
            else if (source instanceof LooseFilesInputSource)
            {
                this.getPage().setOutData(
                    new LooseFilesInputSource()
                        .setCameraFile(cameraFile)
                        .setMeshFile(meshFile)
                        .setPhotosDir(photoDir, undistortImagesCheckBox.isSelected()));
            }
            else
            {
                log.error("Error sending info to host controller. LooseFilesInputSource or RealityCaptureInputSource expected.");
            }
        }

        return true;
    }

    @Override
    public boolean confirm()
    {
        source.loadProject();
        return true;
    }

    @Override
    public void receiveData(InputSource source)
    {
        this.source = source;
    }
}
