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
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.LooseFilesInputSource;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.MetashapeProjectInputSource;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.RealityCaptureInputSource;
import kintsugi3d.builder.javafx.controllers.paged.*;

import java.util.function.BiFunction;

public class SelectImportOptionsController
    extends PageControllerBase<DataSourcePage<InputSource, SelectImportOptionsController>>
    implements PageController<DataSourcePage<InputSource, SelectImportOptionsController>>
{

    @FXML private ToggleButton metashapeImportButton;
    @FXML private ToggleButton looseFilesImportButton;
    @FXML private ToggleButton realityCaptureImportButton;
    ToggleGroup buttons = new ToggleGroup();

    @FXML private Pane rootPane;

    @Override
    public Region getRootNode()
    {
        return rootPane;
    }

    @Override
    public void init()
    {
        buttons.getToggles().add(metashapeImportButton);
        buttons.getToggles().add(looseFilesImportButton);
        buttons.getToggles().add(realityCaptureImportButton);

        //add dummy input sources so we can add info to them later
        metashapeImportButton.setOnAction(e -> handleButtonSelect(metashapeImportButton,
            "/fxml/modals/createnewproject/MetashapeImport.fxml",
            SimpleDataPassthroughPage<InputSource, MetashapeImportController>::new,
            new MetashapeProjectInputSource()));

        looseFilesImportButton.setOnAction(e -> handleButtonSelect(looseFilesImportButton,
            "/fxml/modals/createnewproject/CustomImport.fxml",
            SimpleDataPassthroughPage<InputSource, CustomImportController>::new,
            new LooseFilesInputSource()));

        realityCaptureImportButton.setOnAction(e -> handleButtonSelect(realityCaptureImportButton,
            "/fxml/modals/createnewproject/CustomImport.fxml",
            SimpleDataPassthroughPage<InputSource, CustomImportController>::new,
            new RealityCaptureInputSource()));
    }

    @Override
    public void refresh()
    {
    }

    @Override
    public void finish()
    {
    }

    public void handleButtonSelect(ToggleButton button,
        String path,  BiFunction<String, FXMLLoader, ? extends DataReceiverPage<InputSource, ?>> pageConstructor,
        InputSource source)
    {
        if (button.isSelected())
        {
            getPage().setNextPage(getPageFrameController().createPage(path, pageConstructor));
            getPageFrameController().updatePrevAndNextButtons();
            this.getPage().setData(source);
        }
        else
        {
            getPage().setNextPage(null);
            getPageFrameController().updatePrevAndNextButtons();
        }
    }

    // Have this so we can navigate to loose files selection from inside an error message somewhere else
    public void looseFilesSelect()
    {
        getPage().setNextPage(getPageFrameController().createPage(
            "/fxml/modals/createnewproject/CustomImport.fxml",
            SimpleDataPassthroughPage<InputSource, CustomImportController>::new));
        getPageFrameController().nextPage();
    }
}
