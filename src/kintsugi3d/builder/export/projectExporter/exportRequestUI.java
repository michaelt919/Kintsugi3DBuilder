package kintsugi3d.builder.export.projectExporter;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.paint.Color;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Background;
import javafx.scene.layout.CornerRadii;
import kintsugi3d.builder.core.IBRRequestQueue;
import kintsugi3d.builder.core.IBRRequestUI;
import kintsugi3d.builder.core.Kintsugi3DBuilderState;
import kintsugi3d.builder.export.specular.SpecularFitRequest;
import kintsugi3d.builder.export.specular.SpecularFitRequestUI;
import kintsugi3d.builder.fit.settings.ExportSettings;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import kintsugi3d.gl.core.Context;

public class exportRequestUI implements IBRRequestUI {

    private Kintsugi3DBuilderState modelAccess;
    private Stage window;
    Button button;
    @FXML private Scene scene;
    private Stage stage;
    private Button runButton;

    @FXML Label combineWeightsLabel = new Label("combineWeights: ");
    @FXML ComboBox<Boolean> combineWeightsBox = new ComboBox<>();
    Label lowresLabel = new Label("generatelowrestexture: ");
    ComboBox<Boolean> generateLowResTexturesBox = new ComboBox<>();
    Label minTexLabel = new Label("minimumTextureResolution: ");
    ComboBox<Integer> minimumTextureResolutionBox = new ComboBox<>();
    Label glTFEnabledLabel = new Label("glTFEnabled: ");
    ComboBox<Boolean> glTFEnabledBox = new ComboBox<>();
    Label glTFPackLabel = new Label("glTFPackTextures: ");
    ComboBox<Boolean> glTFPackTexturesBox = new ComboBox<>();
    Label openViewerLabel = new Label("openViewerOnceComplete: ");
    ComboBox<Boolean> openViewerOnceCompleteBox = new ComboBox<>();
    Label[] labelArray = {combineWeightsLabel, lowresLabel, minTexLabel, glTFEnabledLabel, glTFPackLabel, openViewerLabel};

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
        exportRequest.stage.setTitle("Export request");
        exportRequest.stage.setScene(new Scene(parent));
        exportRequest.stage.initOwner(window);
        return exportRequest;
    }

    @Override
    public <ContextType extends Context<ContextType>> void prompt(IBRRequestQueue<ContextType> requestQueue){

        stage.show();

        runButton.setOnAction(event ->
        {
            ExportSettings settings = new ExportSettings();
            GridPane grid = new GridPane();
            grid.setPadding(new Insets(10, 10, 10, 10));
            grid.setVgap(8);
            grid.setHgap(10);
            BackgroundFill background_fill = new BackgroundFill(Color.BLACK,  CornerRadii.EMPTY, Insets.EMPTY);
            grid.setBackground(new Background(background_fill));
            //window = primaryStage;
            window.setTitle("Export");

            for (int i = 0; i < labelArray.length; i++) {
                GridPane.setConstraints(labelArray[i], 0, i);
                labelArray[i].setTextFill(Color.WHITE);
                if (i == labelArray.length - 1) GridPane.setConstraints(button, 1, i + 2);
            }
            combineWeightsBox.setPromptText(String.valueOf(settings.isCombineWeights()));
            combineWeightsBox.getItems().addAll(true, false);
            GridPane.setConstraints(combineWeightsBox, 1, 0);

            generateLowResTexturesBox.setPromptText(String.valueOf(settings.isGenerateLowResTextures()));
            generateLowResTexturesBox.getItems().addAll(true, false);
            GridPane.setConstraints(generateLowResTexturesBox, 1, 1);

            minimumTextureResolutionBox.setPromptText(String.valueOf(settings.getMinimumTextureResolution()));
            minimumTextureResolutionBox.getItems().addAll(128, 64, 32, 16);
            GridPane.setConstraints(minimumTextureResolutionBox, 1, 2);

            glTFEnabledBox.setPromptText(String.valueOf(settings.isGlTFEnabled()));
            glTFEnabledBox.getItems().addAll(true, false);
            GridPane.setConstraints(glTFEnabledBox, 1, 3);

            glTFPackTexturesBox.setPromptText(String.valueOf(settings.isGlTFPackTextures()));
            glTFPackTexturesBox.getItems().addAll(true, false);
            GridPane.setConstraints(glTFPackTexturesBox, 1, 4);

            openViewerOnceCompleteBox.setPromptText(String.valueOf(settings.isOpenViewerOnceComplete()));
            openViewerOnceCompleteBox.getItems().addAll(true, false);
            GridPane.setConstraints(openViewerOnceCompleteBox, 1, 5);


            combineWeightsBox.setOnAction(e -> settings.setCombineWeights(combineWeightsBox.getValue()));
            generateLowResTexturesBox.setOnAction(e -> settings.setGenerateLowResTextures(generateLowResTexturesBox.getValue()));
            minimumTextureResolutionBox.setOnAction(e -> settings.setMinimumTextureResolution(minimumTextureResolutionBox.getValue()));
            glTFEnabledBox.setOnAction(e -> settings.setGlTFEnabled(glTFEnabledBox.getValue()));
            glTFPackTexturesBox.setOnAction(e -> settings.setGlTFPackTextures(glTFPackTexturesBox.getValue()));
            openViewerOnceCompleteBox.setOnAction(e -> settings.setOpenViewerOnceComplete(openViewerOnceCompleteBox.getValue()));

            grid.getChildren().addAll(combineWeightsBox, generateLowResTexturesBox, minimumTextureResolutionBox,
                    glTFEnabledBox, glTFPackTexturesBox, openViewerOnceCompleteBox, combineWeightsLabel, lowresLabel,
                    minTexLabel, glTFEnabledLabel, glTFPackLabel, openViewerLabel, button);
            Scene scene = new Scene(grid, 300, 300);
            scene.setFill(Color.BLACK);
            window.setScene(scene);

            window.show();
//          requestQueue.addIBRRequest();
        });

    }
}
