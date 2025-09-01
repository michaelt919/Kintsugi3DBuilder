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
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
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

        StaticUtilities.makeClampedNumeric(0, Float.MAX_VALUE, occlusionBiasTextField);
        StaticUtilities.makeClampedNumeric(0, 1, edgeProximityMarginTextField);
        StaticUtilities.makeClampedNumeric(0, 1, edgeProximityCutoffTextField);

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

    private <T> void bind(StringProperty uiProperty, Property<T> settingsProperty, StringConverter<T> converter)
    {
        // Local model automatically updates when the UI is change
        // Add a listener to automatically push to the project settings model.
        uiProperty.setValue(converter.toString(settingsProperty.getValue()));
        uiProperty.bindBidirectional(settingsProperty, converter);
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
        bind(occlusionBiasTextField.textProperty(), localSettingsModel.getNumericProperty("occlusionBias"),
            new SafeDecimalNumberStringConverter(projectSettingsModel.getFloat("occlusionBias")));

        bind(edgeProximityWeightCheckBox.selectedProperty(), localSettingsModel.getBooleanProperty("edgeProximityWeightEnabled"));
        bind(edgeProximityMarginTextField.textProperty(), localSettingsModel.getNumericProperty("edgeProximityMargin"),
            new SafeDecimalNumberStringConverter(projectSettingsModel.getFloat("edgeProximityMargin")));
        bind(edgeProximityCutoffTextField.textProperty(), localSettingsModel.getNumericProperty("edgeProximityCutoff"),
            new SafeDecimalNumberStringConverter(projectSettingsModel.getFloat("edgeProximityCutoff")));

        occlusionBiasSlider.valueProperty().bindBidirectional(localSettingsModel.getNumericProperty("occlusionBias"));
        edgeProximityMarginSlider.valueProperty().bindBidirectional(localSettingsModel.getNumericProperty("edgeProximityMargin"));
        edgeProximityCutoffSlider.valueProperty().bindBidirectional(localSettingsModel.getNumericProperty("edgeProximityCutoff"));
    }
    
    @Override
    public boolean cancel()
    {
        // Revert back to the settings when the window was opened.
        projectSettingsModel.copyFrom(revertSettingsModel);
        return true;
    }
}
