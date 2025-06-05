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

package kintsugi3d.builder.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleFloatProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.app.SynchronizedWindow;
import kintsugi3d.builder.app.WindowSynchronization;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import kintsugi3d.builder.javafx.controllers.scene.ProgressBarsController;
import kintsugi3d.builder.javafx.controllers.scene.RootSceneController;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import kintsugi3d.builder.javafx.internal.SettingsModelImpl;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.builder.preferences.GlobalUserPreferencesManager;
import kintsugi3d.builder.preferences.serialization.JacksonUserPreferencesSerializer;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.util.ShadingParameterMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MainApplication extends Application
{
    private static final Logger log = LoggerFactory.getLogger(MainApplication.class);

    private static MainApplication appInstance;

    private static String[] arguments;

    public MainApplication()
    {
        appInstance = this;
    }

    public static MainApplication getAppInstance()
    {
        return appInstance;
    }

    public static void setArgs(String[] args) {
        arguments = args;
    }

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
            // Commit the current user preferences to disk
            try
            {
                log.info("Saving user preferences file to {}", JacksonUserPreferencesSerializer.getPreferencesFile());
                GlobalUserPreferencesManager.getInstance().save();
                log.debug("User preferences file saved successfully!");
            }
            catch (Exception e)
            {
                log.error("An error occurred saving user preferences", e);
                Platform.runLater(() ->
                {
                    new Alert(AlertType.ERROR, "An error occurred while saving user preferences. Your preferences may have been lost.\nCheck the log for more info.").showAndWait();
                });
            }

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
                log.error("Exception occurred confirming application quit:", e);
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
        primaryStage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));

        //get FXML URLs
        String menuBarFXMLFileName = "fxml/menubar/MenuBar.fxml";
        URL menuBarURL = getClass().getClassLoader().getResource(menuBarFXMLFileName);
        assert menuBarURL != null : "cant find " + menuBarFXMLFileName;

        String sceneFXMLFileName = "fxml/scene/RootScene.fxml";
        URL sceneURL = getClass().getClassLoader().getResource(sceneFXMLFileName);
        assert sceneURL != null : "cant find " + sceneFXMLFileName;

        String welcomeWindowFXMLFileName = "fxml/scene/WelcomeWindow.fxml";
        URL welcomeWindowURL = getClass().getClassLoader().getResource(welcomeWindowFXMLFileName);
        assert welcomeWindowURL != null : "cant find " + welcomeWindowFXMLFileName;

        String progressBarsFXMLFileName = "fxml/scene/ProgressBars.fxml";
        URL progressBarsURL = getClass().getClassLoader().getResource(progressBarsFXMLFileName);
        assert progressBarsURL != null : "cant find " + progressBarsFXMLFileName;

        //init fxml loaders
        FXMLLoader sceneFXMLLoader = new FXMLLoader(sceneURL);
        FXMLLoader menuBarFXMLLoader = new FXMLLoader(menuBarURL);
        FXMLLoader welcomeWindowFXMLLoader = new FXMLLoader(welcomeWindowURL);
        FXMLLoader progressBarsFXMLLoader = new FXMLLoader(progressBarsURL);

        //load Parents
        Parent menuBarRoot = menuBarFXMLLoader.load();
        Parent sceneRoot = sceneFXMLLoader.load();
        Parent welcomeRoot = welcomeWindowFXMLLoader.load();
        Parent progressBarsRoot = progressBarsFXMLLoader.load();

        //load Controllers
        RootSceneController sceneController = sceneFXMLLoader.getController();
        MenubarController menuBarController = menuBarFXMLLoader.getController();
        WelcomeWindowController welcomeWindowController = welcomeWindowFXMLLoader.getController();
        ProgressBarsController progressBarsController = progressBarsFXMLLoader.getController();

        //load stages
        primaryStage.setTitle("Kintsugi 3D Builder");
        primaryStage.setScene(new Scene(menuBarRoot));
        primaryStage.setMaximized(true);

        Stage welcomeStage = new Stage();
        welcomeStage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        welcomeStage.setTitle("Welcome!");
        welcomeStage.setScene(new Scene(welcomeRoot));
        welcomeStage.initOwner(primaryStage.getScene().getWindow());

        Stage sceneStage = new Stage();
        sceneStage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        sceneStage.setTitle("Scene");
        sceneStage.setScene(new Scene(sceneRoot));

        Stage progressBarsStage = new Stage();
        progressBarsStage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        progressBarsStage.setTitle("Progress");
        progressBarsStage.setScene(new Scene(progressBarsRoot));
        progressBarsStage.initOwner(primaryStage.getScene().getWindow());
        progressBarsStage.setResizable(false); //remove minimize and maximize buttons from system nav

        //set positions

        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

//        primaryStage.setX(primaryScreenBounds.getMinX() + 10);
//        primaryStage.setY(primaryScreenBounds.getMinY() + 10);

        welcomeStage.setX(primaryScreenBounds.getMinX() + 10);
        welcomeStage.setY(primaryScreenBounds.getMinY() + 120);

        primaryStage.show();

        sceneStage.setX(primaryScreenBounds.getMinX() + primaryScreenBounds.getWidth() - 430);
        sceneStage.setY(primaryScreenBounds.getMinY() + 10);
        sceneStage.initOwner(primaryStage.getScene().getWindow());

//        sceneStage.show();
//        sceneStage.setMinWidth(sceneStage.getWidth());
//        sceneStage.setMaxWidth(sceneStage.getWidth());

//        String libraryFXMLFileName = "fxml/library/Library.fxml";
//        URL libraryURL = getClass().getClassLoader().getResource(libraryFXMLFileName);
//        assert libraryURL != null : "cant find " + libraryFXMLFileName;
//        FXMLLoader libraryFXMLLoader = new FXMLLoader(libraryURL);
//        LibraryController libraryController = libraryFXMLLoader.getController();

//        Parent libraryRoot = libraryFXMLLoader.load();
//        Stage libraryStage = new Stage();
//        libraryStage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
//        libraryStage.setTitle("Library");
//        libraryStage.setScene(new Scene(libraryRoot));

//        libraryStage.setX(primaryScreenBounds.getMinX() + 10);
//        libraryStage.setY(primaryScreenBounds.getMinY() + 50);
//        libraryStage.initOwner(primaryStage.getScene().getWindow());
//        libraryStage.show();

        primaryStage.requestFocus();
        primaryStage.show();

        //only show the welcome window after determining that no projects are being loaded from command line
        if(arguments.length == 0){
            welcomeStage.show();
        }

        MultithreadModels.getInstance().getCanvasModel().addCanvasChangedListener(
            canvas -> menuBarController.getFramebufferView().setCanvas(canvas));

        SettingsModelImpl settingsModel = InternalModels.getInstance().getSettingsModel();
        settingsModel.createBooleanSetting("lightCalibrationMode", false);
        settingsModel.createObjectSetting("currentLightCalibration", Vector2.ZERO);
        settingsModel.createBooleanSetting("occlusionEnabled", true, true);
        settingsModel.createBooleanSetting("fresnelEnabled", false, true);
        settingsModel.createBooleanSetting("pbrGeometricAttenuationEnabled", false, true);
        settingsModel.createBooleanSetting("relightingEnabled", false);
        settingsModel.createBooleanSetting("shadowsEnabled", false, true);
        settingsModel.createBooleanSetting("visibleLightsEnabled", true);
        settingsModel.createBooleanSetting("lightWidgetsEnabled", false);
        settingsModel.createBooleanSetting("visibleCameraPosesEnabled", false);
        settingsModel.createBooleanSetting("visibleSavedCameraPosesEnabled", false);
        settingsModel.createSettingFromProperty("weightExponent", Number.class,
            StaticUtilities.clamp(1, 100, new SimpleFloatProperty(16.0f)), true);
        settingsModel.createSettingFromProperty("isotropyFactor", Number.class,
            StaticUtilities.clamp(0, 1, new SimpleFloatProperty(0.0f)), true);
        settingsModel.createSettingFromProperty("occlusionBias", Number.class,
            StaticUtilities.clamp(0, 0.1, new SimpleFloatProperty(0.0025f)), true);
        settingsModel.createObjectSetting("weightMode", ShadingParameterMode.PER_PIXEL, true);
        settingsModel.createBooleanSetting("is3DGridEnabled", true, true);
        settingsModel.createBooleanSetting("isCameraVisualEnabled", false, true);
        settingsModel.createBooleanSetting("compassEnabled", false, true);
        settingsModel.createBooleanSetting("multisamplingEnabled", false, true);
        settingsModel.createBooleanSetting("halfResolutionEnabled", false, true);
        settingsModel.createBooleanSetting("sceneWindowOpen", false);
        settingsModel.createBooleanSetting("buehlerAlgorithm", true, true);
        settingsModel.createNumericSetting("buehlerViewCount", 5, true);

        // Load user preferences, injecting where needed
        log.info("Loading user preferences from file {}", JacksonUserPreferencesSerializer.getPreferencesFile());
        GlobalUserPreferencesManager.getInstance().load();

        if (GlobalUserPreferencesManager.getInstance().hasStartupFailures())
        {
            ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            ButtonType showLog = new ButtonType("Show Log", ButtonBar.ButtonData.YES);
            Alert alert = new Alert(AlertType.WARNING, "An error occurred loading your user preferences, and they may have been reverted to their defaults. No action is needed.\nCheck the log for more info.", ok, showLog);
            ((Button) alert.getDialogPane().lookupButton(showLog)).setOnAction(event -> {
                menuBarController.help_console();
            });
            alert.show();
        }

        //distribute to controllers
        sceneController.init(
            InternalModels.getInstance().getCameraModel(),
            InternalModels.getInstance().getLightingModel(),
            InternalModels.getInstance().getEnvironmentModel(),
            InternalModels.getInstance().getObjectModel(),
            InternalModels.getInstance().getProjectModel(),
            MultithreadModels.getInstance().getSceneViewportModel());

        //init progress bars first so other controllers can access the progress bar fxml components
        progressBarsController.init(progressBarsStage);

        welcomeWindowController.init(welcomeStage, Rendering.getRequestQueue(), InternalModels.getInstance(),
                () -> getHostServices().showDocument("https://michaelt919.github.io/Kintsugi3DBuilder/Kintsugi3DDocumentation.pdf"));

        menuBarController.init(primaryStage, InternalModels.getInstance(),
            () -> getHostServices().showDocument("https://michaelt919.github.io/Kintsugi3DBuilder/Kintsugi3DDocumentation.pdf"));

        // Open scene window from the menu
        settingsModel.getBooleanProperty("sceneWindowOpen").addListener(sceneWindowOpen ->
        {
            boolean shouldOpen = settingsModel.getBoolean("sceneWindowOpen");
            if (shouldOpen && !sceneStage.isShowing())
            {
                sceneStage.show();
            }
            else if (!shouldOpen && sceneStage.isShowing())
            {
                sceneStage.hide();
            }
        });

        // Synchronize menu state if the scene window is closed using the "X"
        sceneStage.setOnCloseRequest(event ->
        {
            if (settingsModel.getBoolean("sceneWindowOpen"))
            {
                settingsModel.set("sceneWindowOpen", false);
            }
        });

        SynchronizedWindow menuBarWindow = new StageSynchronization(primaryStage);

        //set up close and focusGained
        WindowSynchronization.getInstance().addListener(menuBarWindow);

        primaryStage.setOnCloseRequest(event ->
        {
            // Consume the event and let the window synchronization system close the stage later if the user confirms that they want to exit.
            event.consume();
            WindowSynchronization.getInstance().quit();
        });

//        welcomeStage.setOnCloseRequest(event ->
//        {
//            // Consume the event and let the window synchronization system close the stage later if the user confirms that they want to exit.
//            event.consume();
//            WindowSynchronization.getInstance().quit();
//        });

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
