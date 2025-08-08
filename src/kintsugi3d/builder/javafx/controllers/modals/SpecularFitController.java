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

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;
import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.GraphicsRequestController;
import kintsugi3d.builder.core.GraphicsRequestQueue;
import kintsugi3d.builder.fit.SpecularFitRequest;
import kintsugi3d.builder.fit.settings.SpecularFitRequestParams;
import kintsugi3d.builder.javafx.ProjectIO;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import kintsugi3d.gl.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class SpecularFitController implements GraphicsRequestController
{
    private static final Logger LOG = LoggerFactory.getLogger(SpecularFitController.class);

    @FXML private Accordion advancedAccordion;
    @FXML private CheckBox smithCheckBox;
    @FXML private TextField unsuccessfulLMIterationsTextField;
    @FXML private TextField widthTextField;
    @FXML private TextField heightTextField;

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

    @FXML private Button runButton;

    private Stage stage;

    public static SpecularFitController create(Window window) throws IOException
    {
        String fxmlFileName = "fxml/modals/SpecularFit.fxml";
        URL url = SpecularFitController.class.getClassLoader().getResource(fxmlFileName);
        assert url != null : "Can't find " + fxmlFileName;

        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent parent = fxmlLoader.load();
        SpecularFitController controller = fxmlLoader.getController();

        controller.stage = new Stage();
        controller.stage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        controller.stage.setTitle("Process Textures");
        controller.stage.setScene(new Scene(parent));
        controller.stage.initOwner(window);

        return controller;
    }

    @FXML
    public void cancelButtonAction()
    {
        stage.close();
    }

    @Override
    public <ContextType extends Context<ContextType>> void prompt(GraphicsRequestQueue<ContextType> requestQueue)
    {
        advancedAccordion.expandedPaneProperty().addListener((observable, oldValue, newValue) ->
            // Use Platform.runLater since the scene layout seems to not be updated yet at this point.
            Platform.runLater(stage::sizeToScene));

        stage.show();

        runButton.setOnAction(event ->
        {
            //stage.close();

            if (Global.state().getIOModel().getProgressMonitor().isConflictingProcess())
            {
                return;
            }

            SpecularFitRequestParams settings = new SpecularFitRequestParams(
                Integer.parseInt(widthTextField.getText()),
                Integer.parseInt(heightTextField.getText()));

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
            request.setRequestCompleteCallback(() ->
            {
                //enable shaders which only work after processing textures
                MenubarController.getInstance().setToggleableShaderDisable(false);
                MenubarController.getInstance().updateShaderList();
                MenubarController.getInstance().selectMaterialBasisShader();
            });
            
            request.setIOErrorCallback(e -> ProjectIO.handleException("Error executing specular fit request:", e));

//            if (priorSolutionCheckBox.isSelected() && priorSolutionField.getText() != null && !priorSolutionField.getText().isEmpty())
//            {
//                // Run as a "Graphics request" that doesn't require project graphics resources to be loaded (since we're using a prior solution)
//                settings.setPriorSolutionDirectory(new File(priorSolutionField.getText()));
//                requestQueue.addGraphicsRequest(request);
//            }
//            else
//            {
            // Run as a graphics request that optimizes from scratch.
            requestQueue.addGraphicsRequest(request);
//            }
        });
    }
}
