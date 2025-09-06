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
import kintsugi3d.builder.javafx.util.SquareResolution;
import kintsugi3d.builder.javafx.util.StaticUtilities;

public class SpecularFitController extends DeferredProjectSettingsControllerBase
{
    @FXML private Pane root;

    @FXML private Accordion advancedAccordion;
    @FXML private CheckBox smithCheckBox;
    @FXML private TextField unsuccessfulLMIterationsTextField;
    @FXML private ComboBox<SquareResolution> resolutionComboBox;

    @FXML private TextField basisCountTextField;
    @FXML private TextField mfdResolutionTextField;
    @FXML private TextField specularComplexityTextField;
    @FXML private TextField convergenceToleranceTextField;
    @FXML private TextField specularMinWidthTextField;
    @FXML private TextField specularSmoothnessTextField;
    @FXML private TextField metallicityTextField;
    @FXML private CheckBox translucencyCheckBox;
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
        getLocalSettingsModel().bindNumericComboBox(resolutionComboBox, "textureSize", SquareResolution::new, SquareResolution::getSize);
        getLocalSettingsModel().bindNonNegativeIntegerSetting(basisCountTextField, "basisCount", 256);
        getLocalSettingsModel().bindNormalizedSetting(specularMinWidthTextField, "specularMinWidthFrac");
        getLocalSettingsModel().bindNormalizedSetting(specularSmoothnessTextField, "specularMaxWidthFrac");
        getLocalSettingsModel().bindBooleanSetting(translucencyCheckBox, "constantTermEnabled");
        getLocalSettingsModel().bindNonNegativeIntegerSetting(mfdResolutionTextField, "basisResolution", 8192);
        getLocalSettingsModel().bindNormalizedSetting(specularComplexityTextField, "basisComplexityFrac");
        getLocalSettingsModel().bindNormalizedSetting(metallicityTextField, "metallicity");
        getLocalSettingsModel().bindBooleanSetting(smithCheckBox, "smithMaskingShadowingEnabled");
        getLocalSettingsModel().bindEpsilonSetting(convergenceToleranceTextField, "convergenceTolerance");
        getLocalSettingsModel().bindBooleanSetting(normalRefinementCheckBox, "normalOptimizationEnabled");
        getLocalSettingsModel().bindNormalizedSetting(minNormalDampingTextField, "minNormalDamping");
        getLocalSettingsModel().bindNonNegativeIntegerSetting(normalSmoothingIterationsTextField, "normalSmoothIterations", 8192);
        getLocalSettingsModel().bindNonNegativeIntegerSetting(unsuccessfulLMIterationsTextField, "unsuccessfulLMIterationsAllowed", Integer.MAX_VALUE);
        getLocalSettingsModel().bindBooleanSetting(openViewerOnComplete, "openViewerOnProcessingComplete");

        setCanAdvance(true);
        setCanConfirm(true);
    }

    @FXML
    public boolean confirm()
    {
        if (!applySettings())
        {
            return false;
        }

        if (Global.state().getIOModel().getProgressMonitor().isConflictingProcess())
        {
            error("Failed to start process", "Another process is already running.");
            return false;
        }

        // Run as a graphics request that optimizes from scratch.
        // Automatically pulls settings from project settings.
        Rendering.getRequestQueue().addGraphicsRequest(new SpecularFitRequest());

        return true;
    }

}
