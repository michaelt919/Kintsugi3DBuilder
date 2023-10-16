/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.export.specular;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.core.IBRRequestQueue;
import kintsugi3d.builder.core.IBRRequestUI;
import kintsugi3d.builder.core.Kintsugi3DBuilderState;
import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.fit.settings.SpecularFitRequestParams;
import kintsugi3d.gl.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class SpecularFitRequestUI implements IBRRequestUI
{
    private static final Logger log = LoggerFactory.getLogger(SpecularFitRequestUI.class);
    @FXML private CheckBox smithCheckBox;
    @FXML private TextField unsuccessfulLMIterationsTextField;
    @FXML private TextField widthTextField;
    @FXML private TextField heightTextField;

    @FXML private TextField basisCountTextField;
    @FXML private TextField mfdResolutionTextField;
    @FXML private TextField convergenceToleranceTextField;
    @FXML private TextField specularSmoothnessTextField;
    @FXML private TextField metallicityTextField;
    @FXML private CheckBox translucencyCheckBox;
    @FXML private CheckBox normalRefinementCheckBox;
    @FXML private TextField minNormalDampingTextField;
    @FXML private TextField normalSmoothingIterationsTextField;

    @FXML private CheckBox exportTextureLODsCheckbox;
    @FXML private CheckBox openViewerOnComplete;

    @FXML private Button runButton;

    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    private final FileChooser fileChooser = new FileChooser();

    private Kintsugi3DBuilderState modelAccess;
    private Stage stage;

    private File lastDirectory;
    private File lastViewSet;

    public static SpecularFitRequestUI create(Window window, Kintsugi3DBuilderState modelAccess) throws IOException
    {
        String fxmlFileName = "fxml/export/SpecularFitRequestUI.fxml";
        URL url = SpecularFitRequestUI.class.getClassLoader().getResource(fxmlFileName);
        assert url != null : "Can't find " + fxmlFileName;

        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent parent = fxmlLoader.load();
        SpecularFitRequestUI svdRequestUI = fxmlLoader.getController();
        svdRequestUI.modelAccess = modelAccess;

        svdRequestUI.stage = new Stage();
        svdRequestUI.stage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        svdRequestUI.stage.setTitle("Specular fit request");
        svdRequestUI.stage.setScene(new Scene(parent));
        svdRequestUI.stage.initOwner(window);
        return svdRequestUI;
    }

    @FXML
    public void cancelButtonAction()
    {
        stage.close();
    }

    @Override
    public <ContextType extends Context<ContextType>> void prompt(IBRRequestQueue<ContextType> requestQueue)
    {
        stage.show();

        runButton.setOnAction(event ->
        {
            //stage.close();

            SpecularFitRequestParams settings = new SpecularFitRequestParams(new TextureResolution(
                    Integer.parseInt(widthTextField.getText()),
                    Integer.parseInt(heightTextField.getText())),
                modelAccess.getSettingsModel());
            settings.setGamma(modelAccess.getSettingsModel().getFloat("gamma"));

            int basisCount = Integer.parseInt(basisCountTextField.getText());
            settings.getSpecularBasisSettings().setBasisCount(basisCount);
            int microfacetDistributionResolution = Integer.parseInt(mfdResolutionTextField.getText());
            settings.getSpecularBasisSettings().setBasisResolution(microfacetDistributionResolution);

            settings.getExportSettings().setCombineWeights(true /* combineWeightsCheckbox.isSelected() */);
            settings.getExportSettings().setOpenViewerOnceComplete(openViewerOnComplete.isSelected());

            // Specular / general settings
            double convergenceTolerance = Double.parseDouble(convergenceToleranceTextField.getText());
            settings.setConvergenceTolerance(convergenceTolerance);
            double specularSmoothness = Double.parseDouble(specularSmoothnessTextField.getText());
            settings.getSpecularBasisSettings().setSpecularSmoothness(specularSmoothness);
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

            settings.getExportSettings().setGenerateLowResTextures(exportTextureLODsCheckbox.isSelected());

            // glTF export settings
            settings.getExportSettings().setGlTFEnabled(true);
            settings.getExportSettings().setGlTFPackTextures(false);

            // Image cache settings
            settings.getImageCacheSettings().setCacheParentDirectory(ApplicationFolders.getFitCacheRootDirectory().toFile());
            settings.getImageCacheSettings().setTextureWidth(settings.getTextureResolution().width);
            settings.getImageCacheSettings().setTextureHeight(settings.getTextureResolution().height);
            settings.getImageCacheSettings().setTextureSubdiv( // TODO expose this in the interface
                (int)Math.ceil(Math.max(settings.getTextureResolution().width, settings.getTextureResolution().height) / 256.0));
            settings.getImageCacheSettings().setSampledSize(256); // TODO expose this in the interface

            SpecularFitRequest request = new SpecularFitRequest(settings, modelAccess);

//            if (priorSolutionCheckBox.isSelected() && priorSolutionField.getText() != null && !priorSolutionField.getText().isEmpty())
//            {
//                // Run as a "Graphics request" that doesn't require IBR resources to be loaded (since we're using a prior solution)
//                settings.setPriorSolutionDirectory(new File(priorSolutionField.getText()));
//                requestQueue.addGraphicsRequest(request);
//            }
//            else
//            {
                // Run as an IBR request that optimizes from scratch.
                requestQueue.addIBRRequest(request);
//            }
        });
    }
}
