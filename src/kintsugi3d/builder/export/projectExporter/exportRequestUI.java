package kintsugi3d.builder.export.projectExporter;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;
import kintsugi3d.builder.core.IBRRequestQueue;
import kintsugi3d.builder.core.IBRRequestUI;
import kintsugi3d.builder.core.Kintsugi3DBuilderState;
import kintsugi3d.builder.fit.settings.ExportSettings;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import kintsugi3d.gl.core.Context;

public class exportRequestUI implements IBRRequestUI {

    private Kintsugi3DBuilderState modelAccess;
    @FXML private Stage stage;

    //Initalizes all the variables in the FXML file
    @FXML private Button runButton;
    @FXML private CheckBox combineWeightsCheckBox;
    @FXML private CheckBox generateLowResolutionCheckBox;
    @FXML private CheckBox glTFEnabledCheckBox;
    @FXML private CheckBox glTFPackTexturesCheckBox;
    @FXML private CheckBox openViewerOnceCheckBox;
    @FXML private TextField minimumTextureResolutionTextField;


    //ComboBox<Boolean>[] boxArray = {combineWeightsBox, generateLowResTexturesBox, glTFEnabledBox, glTFPackTexturesBox, openViewerOnceCompleteBox};
    public static exportRequestUI create(Window window, Kintsugi3DBuilderState modelAccess) throws IOException {
        String fxmlFileName = "fxml/export/ExportRequestUI.fxml";
        URL url = exportRequestUI.class.getClassLoader().getResource(fxmlFileName);
        assert url != null : "Can't find " + fxmlFileName;

        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent parent = fxmlLoader.load();
        exportRequestUI exportRequest = fxmlLoader.getController();
        exportRequest.modelAccess = modelAccess;

        exportRequest.stage = new Stage();
        exportRequest.stage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        exportRequest.stage.setTitle("Export Request");
        exportRequest.stage.setScene(new Scene(parent));
        exportRequest.stage.initOwner(window);
        return exportRequest;
    }

    @Override
    public <ContextType extends Context<ContextType>> void prompt(IBRRequestQueue<ContextType> requestQueue){
        ExportSettings settings = new ExportSettings();

        stage.show();

        //Sets all the values to what they are in settings
        combineWeightsCheckBox.setSelected(settings.isCombineWeights());
        generateLowResolutionCheckBox.setSelected(settings.isGenerateLowResTextures());
        glTFEnabledCheckBox.setSelected(settings.isGlTFEnabled());
        glTFPackTexturesCheckBox.setSelected(settings.isGlTFPackTextures());
        openViewerOnceCheckBox.setSelected(settings.isOpenViewerOnceComplete());
        int getMinimumTexRes = settings.getMinimumTextureResolution();
        minimumTextureResolutionTextField.setText(Integer.toString(getMinimumTexRes));

        runButton.setOnAction(event ->
        {
            //Once the function is ran they set the settings to the new selected values
            settings.setCombineWeights(combineWeightsCheckBox.isSelected());
            settings.setGenerateLowResTextures(generateLowResolutionCheckBox.isSelected());
            settings.setGlTFEnabled(glTFEnabledCheckBox.isSelected());
            settings.setGlTFPackTextures(glTFPackTexturesCheckBox.isSelected());
            settings.setOpenViewerOnceComplete(openViewerOnceCheckBox.isSelected());

            int minimumTextureRes = Integer.parseInt(minimumTextureResolutionTextField.getText());
            settings.setMinimumTextureResolution(minimumTextureRes);

        });
    }

    @FXML
    public void cancelButtonAction()
    {
        stage.close();
    }
}
