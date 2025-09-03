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
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.IOModel;
import kintsugi3d.builder.fit.SpecularFitRequest;
import kintsugi3d.builder.javafx.controllers.paged.NonDataPageControllerBase;
import kintsugi3d.builder.javafx.internal.ObservableGeneralSettingsModel;
import kintsugi3d.builder.javafx.util.SafeNumberStringConverter;
import kintsugi3d.builder.javafx.util.SquareResolution;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.builder.state.DefaultSettings;

public class SpecularFitController extends NonDataPageControllerBase
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

    private final ObservableGeneralSettingsModel localSettingsModel = new ObservableGeneralSettingsModel();

    @Override
    public Region getRootNode()
    {
        return root;
    }

    private void bindEpsilonSetting(TextField textField, String settingName)
    {
        StaticUtilities.makeClampedNumeric(0, 1, textField);
        SafeNumberStringConverter converter = new SafeNumberStringConverter(localSettingsModel.getFloat(settingName), "0.###E0");
        textField.setText(converter.toString(localSettingsModel.getFloat(settingName)));
        textField.textProperty().bindBidirectional(
            localSettingsModel.getNumericProperty(settingName),
            converter);
    }

    private void bindNormalizedSetting(TextField textField, String settingName)
    {
        StaticUtilities.makeClampedNumeric(0, 1, textField);
        SafeNumberStringConverter converter = new SafeNumberStringConverter(localSettingsModel.getFloat(settingName));
        textField.setText(converter.toString(localSettingsModel.getFloat(settingName)));
        textField.textProperty().bindBidirectional(
            localSettingsModel.getNumericProperty(settingName),
            converter);
    }

    private void bindNonNegativeIntegerSetting(TextField textField, String settingName, int maxValue)
    {
        StaticUtilities.makeClampedInteger(0, maxValue, textField);
        SafeNumberStringConverter converter = new SafeNumberStringConverter(localSettingsModel.getInt(settingName));
        textField.setText(converter.toString(localSettingsModel.getInt(settingName)));
        textField.textProperty().bindBidirectional(
            localSettingsModel.getNumericProperty(settingName),
            converter);
    }

    private void bindBooleanSetting(CheckBox checkBox, String settingName)
    {
        checkBox.setSelected(localSettingsModel.getBoolean(settingName));
        checkBox.selectedProperty().bindBidirectional(
            localSettingsModel.getBooleanProperty(settingName));
    }

    @Override
    public void initPage()
    {
        DefaultSettings.applyProjectDefaults(localSettingsModel);

        resolutionComboBox.setItems(FXCollections.observableArrayList(
            new SquareResolution(256), new SquareResolution(512), new SquareResolution(1024),
            new SquareResolution(2048), new SquareResolution(4096), new SquareResolution(8192)));
        resolutionComboBox.getSelectionModel().select(3); // 2048x2048

        advancedAccordion.expandedPaneProperty().addListener(
            (observable, oldValue, newValue) ->
                // Use Platform.runLater since the scene layout seems to not be updated yet at this point.
                Platform.runLater(root.getScene().getWindow()::sizeToScene));

        setCanAdvance(true);
        setCanConfirm(true);
    }

    @Override
    public void refresh()
    {
        // Populate local model with the current project settings.
        IOModel ioModel = Global.state().getIOModel();
        if (ioModel.hasValidHandler())
        {
            localSettingsModel.copyFrom(ioModel.getLoadedViewSet().getProjectSettings());
        }

        // Manually bind resolution both ways as a combo box.
        resolutionComboBox.valueProperty().addListener(
            (obs, oldValue, newValue) ->
                localSettingsModel.set("textureSize", newValue.getSize()));
        localSettingsModel.getNumericProperty("textureSize").addListener(
            (obs, oldValue, newValue) ->
                resolutionComboBox.setValue(new SquareResolution(newValue.intValue())));
        resolutionComboBox.setValue(new SquareResolution(localSettingsModel.getInt("textureSize")));

        // Bind everything else.
        bindNonNegativeIntegerSetting(basisCountTextField, "basisCount", 256);
        bindNormalizedSetting(specularMinWidthTextField, "specularMinWidthFrac");
        bindNormalizedSetting(specularSmoothnessTextField, "specularMaxWidthFrac");
        bindBooleanSetting(translucencyCheckBox, "constantTermEnabled");
        bindNonNegativeIntegerSetting(mfdResolutionTextField, "basisResolution", 8192);
        bindNormalizedSetting(specularComplexityTextField, "basisComplexityFrac");
        bindNormalizedSetting(metallicityTextField, "metallicity");
        bindBooleanSetting(smithCheckBox, "smithMaskingShadowingEnabled");
        bindEpsilonSetting(convergenceToleranceTextField, "convergenceTolerance");
        bindBooleanSetting(normalRefinementCheckBox, "normalOptimizationEnabled");
        bindNormalizedSetting(minNormalDampingTextField, "minNormalDamping");
        bindNonNegativeIntegerSetting(normalSmoothingIterationsTextField, "normalSmoothIterations", 8192);
        bindNonNegativeIntegerSetting(unsuccessfulLMIterationsTextField, "unsuccessfulLMIterationsAllowed", Integer.MAX_VALUE);
        bindBooleanSetting(openViewerOnComplete, "openViewerOnProcessingComplete");
    }

    @FXML
    public boolean cancel()
    {
        return true;
    }

    @FXML
    public boolean confirm()
    {
        IOModel ioModel = Global.state().getIOModel();
        if (!ioModel.hasValidHandler())
        {
            error("Failed to start process", "No project is loaded.");
            return false;
        }

        // Apply settings so they're seen by the SpecularFitRequest and also remembered for later.
        ioModel.getLoadedViewSet().getProjectSettings().copyFrom(localSettingsModel);

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
