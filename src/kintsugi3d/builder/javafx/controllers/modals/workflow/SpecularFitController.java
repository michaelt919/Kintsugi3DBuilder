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
import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.IOModel;
import kintsugi3d.builder.fit.SpecularFitRequest;
import kintsugi3d.builder.fit.settings.SpecularFitRequestParams;
import kintsugi3d.builder.javafx.controllers.paged.NonDataPageControllerBase;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.javafx.internal.ObservableSettingsModel;
import kintsugi3d.builder.javafx.util.SquareResolution;
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

    private final ObservableSettingsModel localSettingsModel = new ObservableSettingsModel();

    @Override
    public Region getRootNode()
    {
        return root;
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
        IOModel ioModel = Global.state().getIOModel();
        if (ioModel.hasValidHandler())
        {
            localSettingsModel.copyFrom(ioModel.getLoadedViewSet().getProjectSettings());
        }
    }

    @FXML
    public boolean cancel()
    {
        return true;
    }

    @FXML
    public boolean confirm()
    {
        if (Global.state().getIOModel().getProgressMonitor().isConflictingProcess())
        {
            return false;
        }

        int textureSize = resolutionComboBox.getValue().getSize();
        SpecularFitRequestParams settings = new SpecularFitRequestParams(textureSize, textureSize);

        int basisCount = Integer.parseInt(basisCountTextField.getText());
        settings.getSpecularBasisSettings().setBasisCount(basisCount);
        int basisResolution = Integer.parseInt(mfdResolutionTextField.getText());
        settings.getSpecularBasisSettings().setBasisResolution(basisResolution);

        settings.getExportSettings().setCombineWeights(true /* combineWeightsCheckbox.isSelected() */);
        settings.getExportSettings().setOpenViewerOnceComplete(openViewerOnComplete.isSelected());

        // Specular / general settings
        double convergenceTolerance = Double.parseDouble(convergenceToleranceTextField.getText());
        settings.setConvergenceTolerance(convergenceTolerance);

        double specularMinWidth = Double.parseDouble(specularMinWidthTextField.getText());
        int specularMinWidthDiscrete = (int) Math.round(specularMinWidth * basisResolution);
        settings.getSpecularBasisSettings().setSpecularMinWidth(specularMinWidthDiscrete);

        double specularMaxWidth = Double.parseDouble(specularSmoothnessTextField.getText());
        settings.getSpecularBasisSettings().setSpecularMaxWidth((int) Math.round(specularMaxWidth * basisResolution));

        double specularComplexity = Double.parseDouble(specularComplexityTextField.getText());
        settings.getSpecularBasisSettings().setBasisComplexity(
            (int) Math.round(specularComplexity * (basisResolution - specularMinWidthDiscrete + 1)));

        double metallicity = Double.parseDouble(metallicityTextField.getText());
        settings.getSpecularBasisSettings().setMetallicity(metallicity);

        settings.setShouldIncludeConstantTerm(translucencyCheckBox.isSelected());

        // Normal estimation settings
        boolean normalRefinementEnabled = normalRefinementCheckBox.isSelected();
        settings.getNormalOptimizationSettings().setNormalRefinementEnabled(normalRefinementEnabled);
        double minNormalDamping = Double.parseDouble(minNormalDampingTextField.getText());
        // Negative values shouldn't break anything here.
        settings.getNormalOptimizationSettings().setMinNormalDamping(minNormalDamping);
        int normalSmoothingIterations = Integer.parseInt(normalSmoothingIterationsTextField.getText());
        // Negative values shouldn't break anything here.
        settings.getNormalOptimizationSettings().setNormalSmoothingIterations(normalSmoothingIterations);

        // Settings which shouldn't usually need to be changed
        settings.getSpecularBasisSettings().setSmithMaskingShadowingEnabled(smithCheckBox.isSelected());
        boolean levenbergMarquardtEnabled = true;
        settings.getNormalOptimizationSettings().setLevenbergMarquardtEnabled(levenbergMarquardtEnabled);
        int unsuccessfulLMIterationsAllowed = Integer.parseInt(unsuccessfulLMIterationsTextField.getText());
        settings.getNormalOptimizationSettings().setUnsuccessfulLMIterationsAllowed(unsuccessfulLMIterationsAllowed);
        settings.getReconstructionSettings().setReconstructAll(false);

        // glTF export settings
        settings.getExportSettings().setGlTFEnabled(true);
        settings.getExportSettings().setGlTFPackTextures(false);

        // Image cache settings
        settings.getImageCacheSettings().setCacheParentDirectory(ApplicationFolders.getFitCacheRootDirectory().toFile());

        SpecularFitRequest request = new SpecularFitRequest(settings);
        request.setRequestCompleteCallback(() -> Global.state().getProjectModel().setProjectProcessed(true));
        request.setIOErrorCallback(e -> ExceptionHandling.error("Error executing specular fit request:", e));

        // Run as a graphics request that optimizes from scratch.
        Rendering.getRequestQueue().addGraphicsRequest(request);

        return true;
    }
}
