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
