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
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.ManualInputSource;
import kintsugi3d.builder.javafx.controllers.paged.DataSourcePageControllerBase;
import kintsugi3d.builder.javafx.core.RecentProjects;

import java.io.File;
import java.util.List;

public class ManualImportController extends DataSourcePageControllerBase<ManualInputSource>
{
    @FXML private Text loadCheckCameras;
    @FXML private Text loadCheckObj;
    @FXML private Text loadCheckImages;
    @FXML private VBox root;
    @FXML private Label camPositionsTxt;
    @FXML private CheckBox undistortImagesCheckBox;

    private Stage thisStage;

    private final FileChooser camFileChooser = new FileChooser();
    private final FileChooser objFileChooser = new FileChooser();
    private final DirectoryChooser photoDirectoryChooser = new DirectoryChooser();

    private File cameraFile;
    private File meshFile;
    private File photosDir;

    @Override
    public final Region getRootNode()
    {
        return root;
    }

    @Override
    public final void initPage()
    {
        File recentFile = RecentProjects.getMostRecentDirectory();
        setInitDirectories(recentFile);

        objFileChooser.setTitle("Select 3D model file");
        objFileChooser.getExtensionFilters().add(new ExtensionFilter("Wavefront OBJ file", "*.obj"));
        objFileChooser.getExtensionFilters().add(new ExtensionFilter("Stanford PLY file", "*.ply"));

        camFileChooser.setTitle("Select camera positions file");
        StringBuilder cameraPositionsTextBuilder = new StringBuilder("Camera Positions\n(");
        for (ExtensionFilter filter : getCameraExtensionFilters())
        {
            camFileChooser.getExtensionFilters().add(filter);
            cameraPositionsTextBuilder.append(filter.getDescription()).append(" or\n");
        }

        int finalIdx = cameraPositionsTextBuilder.length();
        cameraPositionsTextBuilder.replace(finalIdx - 4, finalIdx, ")");
        camPositionsTxt.setText(cameraPositionsTextBuilder.toString());

        photoDirectoryChooser.setTitle("Select source photo directory");

        setCanConfirm(true);
    }

    protected List<ExtensionFilter> getCameraExtensionFilters()
    {
        return List.of(new ExtensionFilter("Agisoft Metashape XML file", "*.xml"),
            new ExtensionFilter("Reality Capture CSV file", "*.csv"));
    }

    @Override
    public final void refresh()
    {
        File recentFile = RecentProjects.getMostRecentDirectory();
        setInitDirectories(recentFile);
    }

    @FXML
    private void camFileSelect()
    {
        File file = camFileChooser.showOpenDialog(getStage());

        if (file != null)
        {
            cameraFile = file;
            setHomeDir(file);
            loadCheckCameras.setText("Loaded");
            loadCheckCameras.setFill(Paint.valueOf("LimeGreen"));
        }

        if (areAllFilesLoaded())
        {
            setCanAdvance(true);
        }
    }


    @FXML
    private void objFileSelect()
    {
        File file = objFileChooser.showOpenDialog(getStage());

        if (file != null)
        {
            meshFile = file;
            setHomeDir(file);
            loadCheckObj.setText("Loaded");
            loadCheckObj.setFill(Paint.valueOf("LimeGreen"));
        }

        if (areAllFilesLoaded())
        {
            setCanAdvance(true);
        }
    }

    @FXML
    private void photoDirectorySelect()
    {

        File file = photoDirectoryChooser.showDialog(getStage());

        if (file != null)
        {
            photosDir = file;
            setHomeDir(file);
            loadCheckImages.setText("Loaded");
            loadCheckImages.setFill(Paint.valueOf("LimeGreen"));
        }

        if (areAllFilesLoaded())
        {
            setCanAdvance(true);
        }
    }

    private boolean areAllFilesLoaded()
    {
        return cameraFile != null && meshFile != null && photosDir != null;
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
        getPage().setOutData(new ManualInputSource()
            .setCameraFile(cameraFile)
            .setMeshFile(meshFile)
            .setPhotosDir(photosDir)
            .setNeedsUndistort(undistortImagesCheckBox.isSelected()));
        return true;
    }

    @Override
    public final boolean confirm()
    {
        getPage().getOutData().confirm();
        return true;
    }
}
