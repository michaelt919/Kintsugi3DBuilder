/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleFloatProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.ibrelight.app.Rendering;
import tetzlaff.ibrelight.app.SynchronizedWindow;
import tetzlaff.ibrelight.app.WindowSynchronization;
import tetzlaff.ibrelight.core.StandardRenderingMode;
import tetzlaff.ibrelight.export.general.GeneralRenderRequestUI;
import tetzlaff.ibrelight.javafx.controllers.menubar.MenubarController;
import tetzlaff.ibrelight.javafx.controllers.scene.RootSceneController;
import tetzlaff.ibrelight.javafx.controllers.scene.SceneModel;
import tetzlaff.ibrelight.javafx.internal.SettingsModelImpl;
import tetzlaff.ibrelight.javafx.util.StaticUtilities;
import tetzlaff.util.ShadingParameterMode;

public class MainApplication extends Application
{
    private static class StageSynchronization implements SynchronizedWindow
    {
        private final Stage stage;

        StageSynchronization(Stage sceneStage)
        {
            this.stage = sceneStage;
        }

        @Override
        public boolean isFocused()
        {
            return stage.isFocused();
        }

        @Override
        public void focus()
        {
            stage.toFront();
        }

        @Override
        public void quit()
        {
            Platform.runLater(stage::close);
        }

        @Override
        public boolean confirmQuit()
        {
            FutureTask<Boolean> confirmationTask = new FutureTask<>(() ->
            {
                Dialog<ButtonType> confirmation = new Alert(AlertType.CONFIRMATION, "If you click OK, any unsaved changes will be lost.");
                confirmation.setTitle("Exit Confirmation");
                confirmation.setHeaderText("Are you sure you want to exit?");
                return confirmation.showAndWait()
                    .filter(Predicate.isEqual(ButtonType.OK))
                    .isPresent();
            });

            Platform.runLater(confirmationTask);

            try
            {
                // Blocks until the user confirms or cancels.
                return confirmationTask.get();
            }
            catch (InterruptedException | ExecutionException e)
            {
                e.printStackTrace();
                return false;
            }
        }
    }

    private static final Collection<Consumer<Stage>> START_LISTENERS = new ArrayList<>(1);

    public static void addStartListener(Consumer<Stage> startListener)
    {
        START_LISTENERS.add(startListener);
    }

    @Override
    public void start(Stage primaryStage) throws IOException
    {
        primaryStage.getIcons().add(new Image(new File("ibr-icon.png").toURI().toURL().toString()));

        //get FXML URLs
        String menuBarFXMLFileName = "fxml/menubar/MenuBar.fxml";
        URL menuBarURL = getClass().getClassLoader().getResource(menuBarFXMLFileName);
        assert menuBarURL != null : "cant find " + menuBarFXMLFileName;

        String libraryFXMLFileName = "fxml/library/Library.fxml";
        URL libraryURL = getClass().getClassLoader().getResource(libraryFXMLFileName);
        assert libraryURL != null : "cant find " + libraryFXMLFileName;

        String sceneFXMLFileName = "fxml/scene/RootScene.fxml";
        URL sceneURL = getClass().getClassLoader().getResource(sceneFXMLFileName);
        assert sceneURL != null : "cant find " + sceneFXMLFileName;

        //init fxml loaders
        FXMLLoader sceneFXMLLoader = new FXMLLoader(sceneURL);
        FXMLLoader libraryFXMLLoader = new FXMLLoader(libraryURL);
        FXMLLoader menuBarFXMLLoader = new FXMLLoader(menuBarURL);

        //load Parents
        Parent menuBarRoot = menuBarFXMLLoader.load();
        Parent libraryRoot = libraryFXMLLoader.load();
        Parent sceneRoot = sceneFXMLLoader.load();

        //load Controllers
        RootSceneController sceneController = sceneFXMLLoader.getController();
        MenubarController menuBarController = menuBarFXMLLoader.getController();
//        LibraryController libraryController = libraryFXMLLoader.getController();

        //load stages
        primaryStage.setTitle("IBRelight");
        primaryStage.setScene(new Scene(menuBarRoot));

        Stage libraryStage = new Stage();
        libraryStage.getIcons().add(new Image(new File("ibr-icon.png").toURI().toURL().toString()));
        libraryStage.setTitle("Library");
        libraryStage.setScene(new Scene(libraryRoot));

        Stage sceneStage = new Stage();
        sceneStage.getIcons().add(new Image(new File("ibr-icon.png").toURI().toURL().toString()));
        sceneStage.setTitle("Scene");
        sceneStage.setScene(new Scene(sceneRoot));

        //set positions

        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

        primaryStage.setX(primaryScreenBounds.getMinX() + 10);
        primaryStage.setY(primaryScreenBounds.getMinY() + 10);

        primaryStage.setResizable(false);

        primaryStage.show();

        libraryStage.setX(primaryScreenBounds.getMinX() + 10);
        libraryStage.setY(primaryScreenBounds.getMinY() + 50);
        libraryStage.initOwner(primaryStage.getScene().getWindow());

        //libraryStage.show();

        sceneStage.setX(primaryScreenBounds.getMinX() + primaryScreenBounds.getWidth() - 430);
        sceneStage.setY(primaryScreenBounds.getMinY() + 10);
        sceneStage.initOwner(primaryStage.getScene().getWindow());

        sceneStage.show();
        sceneStage.setMinWidth(sceneStage.getWidth());
        sceneStage.setMaxWidth(sceneStage.getWidth());

        primaryStage.requestFocus();

        SettingsModelImpl settingsModel = InternalModels.getInstance().getSettingsModel();
        settingsModel.createBooleanSetting("lightCalibrationMode", false);
        settingsModel.createObjectSetting("currentLightCalibration", Vector2.ZERO);
        settingsModel.createBooleanSetting("occlusionEnabled", true);
        settingsModel.createBooleanSetting("fresnelEnabled", false);
        settingsModel.createBooleanSetting("pbrGeometricAttenuationEnabled", false);
        settingsModel.createBooleanSetting("relightingEnabled", true);
        settingsModel.createBooleanSetting("shadowsEnabled", false);
        settingsModel.createBooleanSetting("visibleLightsEnabled", true);
        settingsModel.createBooleanSetting("lightWidgetsEnabled", false);
        settingsModel.createBooleanSetting("visibleCameraPosesEnabled", false);
        settingsModel.createBooleanSetting("visibleSavedCameraPosesEnabled", false);
        settingsModel.createSettingFromProperty("gamma", Number.class,
            StaticUtilities.clamp(1, 5, new SimpleFloatProperty(2.2f)));
        settingsModel.createSettingFromProperty("weightExponent", Number.class,
            StaticUtilities.clamp(1, 100, new SimpleFloatProperty(16.0f)));
        settingsModel.createSettingFromProperty("isotropyFactor", Number.class,
            StaticUtilities.clamp(0, 1, new SimpleFloatProperty(0.0f)));
        settingsModel.createSettingFromProperty("occlusionBias", Number.class,
            StaticUtilities.clamp(0, 0.1, new SimpleFloatProperty(0.0025f)));
        settingsModel.createObjectSetting("weightMode", ShadingParameterMode.PER_PIXEL);
        settingsModel.createObjectSetting("renderingMode", StandardRenderingMode.IMAGE_BASED);
        settingsModel.createBooleanSetting("is3DGridEnabled", false);
        settingsModel.createBooleanSetting("compassEnabled", false);
        settingsModel.createBooleanSetting("multisamplingEnabled", false);
        settingsModel.createBooleanSetting("halfResolutionEnabled", false);
        settingsModel.createBooleanSetting("buehlerAlgorithm", true);
        settingsModel.createNumericSetting("buehlerViewCount", 5);
        settingsModel.createSettingFromProperty("distance", Number.class,
                StaticUtilities.clamp(0, 100, new SimpleFloatProperty(0.0025f)));
        settingsModel.createSettingFromProperty("aperture", Number.class,
                StaticUtilities.clamp(0, 1, new SimpleFloatProperty(0.0025f)));
        settingsModel.createSettingFromProperty("focal", Number.class,
                StaticUtilities.clamp(0, 100, new SimpleFloatProperty(0.0025f)));
        SceneModel sceneModel = new SceneModel();


        //distribute to controllers
        sceneController.init(
            InternalModels.getInstance().getCameraModel(),
            InternalModels.getInstance().getLightingModel(),
            InternalModels.getInstance().getEnvironmentModel(),
            InternalModels.getInstance().getObjectModel(),
            InternalModels.getInstance().getSettingsModel(),
            sceneModel,
            MultithreadModels.getInstance().getSceneViewportModel());

        menuBarController.init(primaryStage.getScene().getWindow(), Rendering.getRequestQueue(), InternalModels.getInstance(), sceneModel,
            () -> getHostServices().showDocument("https://docs.google.com/document/d/1jM4sr359-oacpom0TrGLYSqCUdHFEprnvsCn5oVwTEI/edit?usp=sharing"));

        SynchronizedWindow menuBarWindow = new StageSynchronization(primaryStage);

        //set up close and focusGained
        WindowSynchronization.getInstance().addListener(menuBarWindow);

        sceneStage.setOnCloseRequest(event ->
        {
            // Consume the event and let the window synchronization system close the stage later if the user confirms that they want to exit.
            event.consume();
            WindowSynchronization.getInstance().quit();
        });

        primaryStage.setOnCloseRequest(event ->
        {
            // Consume the event and let the window synchronization system close the stage later if the user confirms that they want to exit.
            event.consume();
            WindowSynchronization.getInstance().quit();
        });

        // Focus synchronization not working quite right.
//        sceneStage.focusedProperty().addListener(event ->
//        {
//            if (sceneStage.isFocused())
//            {
//                WindowSynchronization.getInstance().focusGained(sceneWindow);
//            }
//            else
//            {
//                WindowSynchronization.getInstance().focusLost(sceneWindow);
//            }
//        });
//        menuBarStage.focusedProperty().addListener(event ->
//        {
//            if (menuBarStage.isFocused())
//            {
//                WindowSynchronization.getInstance().focusGained(menuBarWindow);
//            }
//            else
//            {
//                WindowSynchronization.getInstance().focusLost(menuBarWindow);
//            }
//        });

        for (Consumer<Stage> l : START_LISTENERS)
        {
            l.accept(primaryStage);
        }
    }

    public static void launchWrapper(String args)
    {
        launch(args);
    }
}
