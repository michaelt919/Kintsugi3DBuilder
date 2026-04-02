/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.modals.workflow;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.fit.SpecularFitRequest;
import kintsugi3d.builder.javafx.controllers.modals.ProjectSettingsControllerBase;
import kintsugi3d.builder.javafx.util.SquareResolution;
import kintsugi3d.builder.javafx.util.StaticUtilities;

public class SpecularTexturesFitController extends ProjectSettingsControllerBase
{
    @FXML private Pane root;
    @FXML private Accordion advancedAccordion;
    @FXML private CheckBox smithCheckBox;
    @FXML private TextField unsuccessfulLMIterationsTextField;
    @FXML private ComboBox<SquareResolution> resolutionComboBox;
    @FXML private TextField convergenceToleranceTextField;
    @FXML private CheckBox normalRefinementCheckBox;
    @FXML private TextField minNormalDampingTextField;
    @FXML private TextField normalSmoothingIterationsTextField;
    @FXML private CheckBox openViewerOnComplete;

    @Override
    public Region getRootNode()
    {
        return root;
    }

    @Override
    public void initPage()
    {
        StaticUtilities.makeSquareResolutionComboBox(resolutionComboBox);

        advancedAccordion.expandedPaneProperty().addListener(
            (observable, oldValue, newValue) ->
                // Use Platform.runLater since the scene layout seems to not be updated yet at this point.
                Platform.runLater(root.getScene().getWindow()::sizeToScene));

        // Bind settings
        bindNumericComboBox(resolutionComboBox, "textureSize", SquareResolution::new, SquareResolution::getSize);
        bindBooleanSetting(smithCheckBox, "smithMaskingShadowingEnabled");
        bindFloatSetting(convergenceToleranceTextField, "convergenceTolerance", 0, 1);

        // if disabled, should discard any existing optimized normal map (or revert to imported normal map)
        bindBooleanSetting(normalRefinementCheckBox, "normalOptimizationEnabled");

        bindNormalizedSetting(minNormalDampingTextField, "minNormalDamping");
        bindIntegerSetting(normalSmoothingIterationsTextField, "normalSmoothIterations", 0, 8192);
        bindIntegerSetting(unsuccessfulLMIterationsTextField, "unsuccessfulLMIterationsAllowed", 0, Integer.MAX_VALUE);
        bindBooleanSetting(openViewerOnComplete, "openViewerOnProcessingComplete");

        setCanAdvance(true);
        setCanConfirm(true);
    }

    protected boolean shouldOptimizeBasis()
    {
        return false;
    }

    @FXML
    public boolean confirm()
    {
        // Apply settings so they're seen by the SpecularFitRequest and also remembered for later.
        applySettings();

        if (Global.state().getIOModel().getProgressMonitor().isConflictingProcess())
        {
            error("Failed to start process", "Another process is already running.");
            return false;
        }

        // Run as a graphics request that optimizes from scratch.
        // Automatically pulls settings from project settings.
        SpecularFitRequest request = new SpecularFitRequest();
        request.getSettings().setShouldOptimizeBasis(shouldOptimizeBasis());
        Rendering.getRequestQueue().addGraphicsRequest(request);

        return true;
    }
}
