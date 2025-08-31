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

package kintsugi3d.builder.javafx.controllers.modals;

import javafx.beans.property.Property;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.javafx.controllers.paged.NonDataPageControllerBase;
import kintsugi3d.builder.javafx.internal.ObservableSettingsModel;
import kintsugi3d.builder.javafx.util.SafeDecimalNumberStringConverter;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.builder.state.DefaultSettings;
import kintsugi3d.builder.state.SettingsModel;
import kintsugi3d.builder.state.impl.SimpleSettingsModel;

public class MaskOptionsController extends NonDataPageControllerBase
{
    @FXML private Pane rootNode;
    
    @FXML private CheckBox occlusionCheckBox;
    @FXML private TextField occlusionBiasTextField;
    @FXML private Slider occlusionBiasSlider;

    @FXML private CheckBox edgeProximityWeightCheckBox;
    @FXML private TextField edgeProximityMarginTextField;
    @FXML private Slider edgeProximityMarginSlider;
    @FXML private TextField edgeProximityCutoffTextField;
    @FXML private Slider edgeProximityCutoffSlider;

    private SettingsModel projectSettingsModel;
    private final ObservableSettingsModel revertSettingsModel = new ObservableSettingsModel();
    private final ObservableSettingsModel localSettingsModel = new ObservableSettingsModel();

    @Override
    public Region getRootNode()
    {
        return rootNode;
    }

    @Override
    public void initPage()
    {
        // Local instance that contains properties
        DefaultSettings.applyProjectDefaults(localSettingsModel);
        
        // Remember what to set back to if the user cancels
        DefaultSettings.applyProjectDefaults(revertSettingsModel);

        StaticUtilities.makeClampedNumeric(0, 0.04, occlusionBiasTextField);
        StaticUtilities.makeClampedNumeric(0, 1.0, edgeProximityMarginTextField);
        StaticUtilities.makeClampedNumeric(0, 0.1, edgeProximityCutoffTextField);

        setCanAdvance(true);
        setCanConfirm(true);
    }

    private <T> void bind(Property<T> uiProperty, Property<T> settingsProperty)
    {
        // Local model automatically updates when the UI is change
        // Add a listener to automatically push to the project settings model.
        uiProperty.setValue(settingsProperty.getValue());
        uiProperty.bindBidirectional(settingsProperty);
        settingsProperty.addListener(obs ->
            projectSettingsModel.copyFrom(localSettingsModel));
    }

    @Override
    public void refresh()
    {
        if (Global.state().getIOModel().hasValidHandler())
        {
            this.projectSettingsModel = Global.state().getIOModel().getLoadedViewSet().getProjectSettings();
        }
        else
        {
            this.projectSettingsModel = new SimpleSettingsModel();
            DefaultSettings.applyProjectDefaults(projectSettingsModel);
        }

        // Need to copy first so that it doesn't start syncing bound properties before we're initialized.
        this.localSettingsModel.copyFrom(projectSettingsModel);
        this.revertSettingsModel.copyFrom(localSettingsModel); // Remember what to set back to if the user cancels

        bind(occlusionCheckBox.selectedProperty(), localSettingsModel.getBooleanProperty("occlusionEnabled"));
        bind(occlusionBiasSlider.valueProperty(), localSettingsModel.getNumericProperty("occlusionBias"));

        bind(edgeProximityWeightCheckBox.selectedProperty(), localSettingsModel.getBooleanProperty("edgeProximityWeightEnabled"));
        bind(edgeProximityMarginSlider.valueProperty(), localSettingsModel.getNumericProperty("edgeProximityMargin"));
        bind(edgeProximityCutoffSlider.valueProperty(), localSettingsModel.getNumericProperty("edgeProximityCutoff"));

        // The slider is the authority for what the value is on the UI end.
        // Not sure if this is actually what we want as it causes issues if the text bounds are larger than the slider bounds.
        // If we want users to be able to enter out-of-bounds values we'd need to make the text field the authority
        // and flip it around.
        occlusionBiasTextField.textProperty().bindBidirectional(occlusionBiasSlider.valueProperty(),
            new SafeDecimalNumberStringConverter(0.0025f));
        edgeProximityMarginTextField.textProperty().bindBidirectional(edgeProximityMarginSlider.valueProperty(),
            new SafeDecimalNumberStringConverter(0.1f));
        edgeProximityCutoffTextField.textProperty().bindBidirectional(edgeProximityCutoffSlider.valueProperty(),
            new SafeDecimalNumberStringConverter(0.01f));
    }
    
    @Override
    public boolean cancel()
    {
        // Revert back to the settings when the window was opened.
        projectSettingsModel.copyFrom(revertSettingsModel);
        return true;
    }
}
