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

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.paged.DataReceiverPageControllerBase;
import kintsugi3d.builder.javafx.controllers.paged.PageController;
import kintsugi3d.builder.javafx.controllers.paged.SimpleDataReceiverPage;

import java.io.File;

public class MasksImportController
    extends DataReceiverPageControllerBase<InputSource> implements PageController<InputSource>
{
    @FXML private Pane rootPane;

    @FXML private ToggleButton useProjectMasksButton;
    @FXML private ToggleButton customMasksDirButton;
    @FXML private ToggleButton noMasksButton;
    private ToggleGroup toggleGroup;

    private DirectoryChooser masksDirectoryChooser;

    private InputSource source;
    private File fileChooserMasksDir; //represents the file chosen through file chooser, which may or may not be the final masks selection

    @Override
    public Region getRootNode()
    {
        return rootPane;
    }

    @Override
    public void init()
    {
        masksDirectoryChooser = new DirectoryChooser();

        toggleGroup = new ToggleGroup();
        toggleGroup.getToggles().add(useProjectMasksButton);
        toggleGroup.getToggles().add(customMasksDirButton);
        toggleGroup.getToggles().add(noMasksButton);

        this.getCanAdvanceObservable().bind(toggleGroup.selectedToggleProperty().isNotNull());

        customMasksDirButton.setOnAction(this::chooseMasksDir);

        getPage().setNextPage(getPageFrameController().createPage(
            "/fxml/modals/createnewproject/PrimaryViewSelect.fxml",
            SimpleDataReceiverPage<InputSource, OrientationViewSelectController>::new));

        setCanConfirm(true);
    }

    @Override
    public void refresh()
    {
        masksDirectoryChooser.setInitialDirectory(source.getInitialMasksDirectory());
        useProjectMasksButton.setDisable(!source.doEnableProjectMasksButton());
    }

    @Override
    public boolean advance()
    {
        if (customMasksDirButton.isSelected())
        {
            source.setMasksDirectory(fileChooserMasksDir);
        }
        else if (noMasksButton.isSelected())
        {
            source.setMasksDirectory(null);
        }
        // Otherwise, project should already have this masks directory initialized from earlier page

        return true;
    }

    @Override
    public boolean confirm()
    {
        source.loadProject();
        return true;
    }

    @FXML
    private void chooseMasksDir(ActionEvent e)
    {
        // Don't show directory chooser when deselecting
        if (!customMasksDirButton.isSelected())
        {
            return;
        }

        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        File file = masksDirectoryChooser.showDialog(stage.getOwner());
        if (file != null)
        {
            masksDirectoryChooser.setInitialDirectory(file);
            fileChooserMasksDir = file;
        }
    }

    @Override
    public void receiveData(InputSource source)
    {
        this.source = source;
    }
}
