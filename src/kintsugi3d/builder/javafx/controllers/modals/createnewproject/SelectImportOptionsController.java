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
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.LooseFilesInputSource;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.MetashapeProjectInputSource;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.RealityCaptureInputSource;
import kintsugi3d.builder.javafx.controllers.paged.DataSourcePageControllerBase;
import kintsugi3d.builder.javafx.controllers.paged.DataSupplierPageController;
import kintsugi3d.builder.javafx.controllers.paged.SimpleDataTransformerPage;

public class SelectImportOptionsController extends DataSourcePageControllerBase<InputSource>
{
    @FXML private ToggleButton metashapeImportButton;
    @FXML private ToggleButton looseFilesImportButton;
    @FXML private ToggleButton realityCaptureImportButton;
    private final ToggleGroup buttons = new ToggleGroup();

    @FXML private Pane rootPane;

    @Override
    public Region getRootNode()
    {
        return rootPane;
    }

    @Override
    public void initPage()
    {
        buttons.getToggles().add(metashapeImportButton);
        buttons.getToggles().add(looseFilesImportButton);
        buttons.getToggles().add(realityCaptureImportButton);

        this.getCanAdvanceObservable().bind(buttons.selectedToggleProperty().isNotNull());

        //add dummy input sources so we can add info to them later
        metashapeImportButton.setOnAction(e -> onButtonAction(metashapeImportButton, this::metashape));

        looseFilesImportButton.setOnAction(e -> onButtonAction(looseFilesImportButton, this::looseFiles));

        realityCaptureImportButton.setOnAction(e -> onButtonAction(realityCaptureImportButton, this::realityCapture));
    }

    @Override
    public void refresh()
    {
    }

    @Override
    public boolean confirm()
    {
        return false;
    }

    public void onButtonAction(Toggle button, Runnable buttonAction)
    {
        if (button.isSelected())
        {
            buttonAction.run();
        }
        else
        {
            getPage().setNextPage(null);
        }
    }

    private <ControllerType extends DataSupplierPageController<? super InputSource, InputSource>>
    void setupInputSource(String nextPageFXML, InputSource inputSource)
    {
        var nextPage = getPageFrameController().createPage(nextPageFXML,
            SimpleDataTransformerPage<InputSource, InputSource, ControllerType>::new);
        this.getPage().setOutData(inputSource);
        this.getPage().setNextPage(nextPage);
    }

    // Have this so we can navigate to loose files selection from inside an error message somewhere else
    public void looseFiles()
    {
        this.<CustomImportController>setupInputSource(
            "/fxml/modals/createnewproject/CustomImport.fxml", new LooseFilesInputSource());
    }

    // Have this so we can navigate to loose files selection from inside an error message somewhere else
    public void realityCapture()
    {
        this.<CustomImportController>setupInputSource(
            "/fxml/modals/createnewproject/CustomImport.fxml", new RealityCaptureInputSource());
    }

    public void metashape()
    {
        this.<MetashapeImportController>setupInputSource(
            "/fxml/modals/createnewproject/MetashapeImport.fxml", new MetashapeProjectInputSource());
    }
}
