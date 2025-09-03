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

package kintsugi3d.builder.javafx.core;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import kintsugi3d.builder.app.SynchronizedWindow;
import kintsugi3d.builder.app.WindowSynchronization;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.javafx.controllers.scene.RootSceneController;
import kintsugi3d.builder.javafx.internal.ObservableGeneralSettingsModel;
import kintsugi3d.builder.preferences.GlobalUserPreferencesManager;
import kintsugi3d.builder.preferences.serialization.JacksonUserPreferencesSerializer;
import kintsugi3d.builder.state.DefaultSettings;
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
    private static final Logger LOG = LoggerFactory.getLogger(MainApplication.class);

    private static String[] arguments;

    public static void setArgs(String[] args)
    {
        arguments = args;
    }

    public static final String ICON_PATH = "Kintsugi3D-icon.png";

    private static Image icon = null;

    public static Image getIcon()
    {
        return icon;
    }

    public static void initAccelerators(Scene scene)
    {
        MainWindowController.getInstance().initAccelerators(scene);
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
                LOG.info("Saving user preferences file to {}", JacksonUserPreferencesSerializer.getPreferencesFile());
                GlobalUserPreferencesManager.getInstance().save();
                LOG.debug("User preferences file saved successfully!");
            }
            catch (Exception e)
            {
                LOG.error("An error occurred saving user preferences", e);
                Platform.runLater(() ->
                    new Alert(AlertType.ERROR, "An error occurred while saving user preferences. Your preferences may have been lost.\nCheck the log for more info.").showAndWait());
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
                LOG.error("Exception occurred confirming application quit:", e);
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
        if (icon == null)
        {
            icon = new Image(new File(ICON_PATH).toURI().toURL().toString());
        }

        primaryStage.getIcons().add(icon);

        //get FXML URLs
        String mainWindowFXMLFileName = "/fxml/main/MainWindow.fxml";
        URL mainWindowURL = getClass().getResource(mainWindowFXMLFileName);
        assert mainWindowURL != null : "cant find " + mainWindowFXMLFileName;

        String sceneFXMLFileName = "/fxml/scene/RootScene.fxml";
        URL sceneURL = getClass().getResource(sceneFXMLFileName);
        assert sceneURL != null : "cant find " + sceneFXMLFileName;

        String welcomeWindowFXMLFileName = "/fxml/WelcomeWindow.fxml";
        URL welcomeWindowURL = getClass().getResource(welcomeWindowFXMLFileName);
        assert welcomeWindowURL != null : "cant find " + welcomeWindowFXMLFileName;

        String progressBarsFXMLFileName = "/fxml/ProgressBars.fxml";
        URL progressBarsURL = getClass().getResource(progressBarsFXMLFileName);
        assert progressBarsURL != null : "cant find " + progressBarsFXMLFileName;

        //init fxml loaders
        FXMLLoader sceneFXMLLoader = new FXMLLoader(sceneURL);
        FXMLLoader mainWindowFXMLLoader = new FXMLLoader(mainWindowURL);
        FXMLLoader welcomeWindowFXMLLoader = new FXMLLoader(welcomeWindowURL);
        FXMLLoader progressBarsFXMLLoader = new FXMLLoader(progressBarsURL);

        //load Parents
        Parent mainWindowRoot = mainWindowFXMLLoader.load();
        Parent sceneRoot = sceneFXMLLoader.load();
        Parent welcomeRoot = welcomeWindowFXMLLoader.load();
        Parent progressBarsRoot = progressBarsFXMLLoader.load();

        //load Controllers
        RootSceneController sceneController = sceneFXMLLoader.getController();
        MainWindowController mainWindowController = mainWindowFXMLLoader.getController();
        WelcomeWindowController welcomeWindowController = welcomeWindowFXMLLoader.getController();
        ProgressBarsController progressBarsController = progressBarsFXMLLoader.getController();

        //load stages
        primaryStage.setTitle("Kintsugi 3D Builder");
        primaryStage.setScene(new Scene(mainWindowRoot));
        primaryStage.setMaximized(true);

        Stage welcomeStage = new Stage();
        welcomeStage.getIcons().add(icon);
        welcomeStage.setTitle("Welcome!");
        welcomeStage.setScene(new Scene(welcomeRoot));
        welcomeStage.initOwner(primaryStage.getScene().getWindow());

        Stage sceneStage = new Stage();
        sceneStage.getIcons().add(icon);
        sceneStage.setTitle("Scene");
        sceneStage.setScene(new Scene(sceneRoot));

        Stage progressBarsStage = new Stage();
        progressBarsStage.getIcons().add(icon);
        progressBarsStage.setTitle("Progress");
        progressBarsStage.setScene(new Scene(progressBarsRoot));
        progressBarsStage.initOwner(primaryStage.getScene().getWindow());
        progressBarsStage.setResizable(false); //remove minimize and maximize buttons from system nav

        //set positions

        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

        welcomeStage.setX(primaryScreenBounds.getMinX() + 10);
        welcomeStage.setY(primaryScreenBounds.getMinY() + 120);

        primaryStage.show();

        sceneStage.setX(primaryScreenBounds.getMinX() + primaryScreenBounds.getWidth() - 430);
        sceneStage.setY(primaryScreenBounds.getMinY() + 10);
        sceneStage.initOwner(primaryStage.getScene().getWindow());

        primaryStage.requestFocus();
        primaryStage.show();

        //only show the welcome window after determining that no projects are being loaded from command line
        if (arguments.length == 0)
        {
            welcomeStage.show();
        }

        Global.state().getCanvasModel().addCanvasChangedListener(
            canvas -> mainWindowController.getFramebufferView().setCanvas(canvas));

        ObservableGeneralSettingsModel settingsModel = JavaFXState.getInstance().getSettingsModel();
        DefaultSettings.applyGlobalDefaults(settingsModel);

        // Load user preferences, injecting where needed
        LOG.info("Loading user preferences from file {}", JacksonUserPreferencesSerializer.getPreferencesFile());
        GlobalUserPreferencesManager.getInstance().load();

        if (GlobalUserPreferencesManager.getInstance().hasStartupFailures())
        {
            ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            ButtonType showLog = new ButtonType("Show Log", ButtonBar.ButtonData.YES);
            Alert alert = new Alert(AlertType.WARNING, "An error occurred loading your user preferences, and they may have been reverted to their defaults. No action is needed.\nCheck the log for more info.", ok, showLog);
            ((Button) alert.getDialogPane().lookupButton(showLog)).setOnAction(
                event -> ExperienceManager.getInstance().getExperience("Log").tryOpen());
            alert.show();
        }

        //distribute to controllers
        sceneController.init(
            JavaFXState.getInstance().getCameraModel(),
            JavaFXState.getInstance().getLightingModel(),
            JavaFXState.getInstance().getEnvironmentModel(),
            JavaFXState.getInstance().getObjectModel(),
            JavaFXState.getInstance().getProjectModel(),
            Global.state().getSceneViewportModel());

        //init progress bars first so other controllers can access the progress bar fxml components
        progressBarsController.init(progressBarsStage);

        mainWindowController.init(primaryStage, JavaFXState.getInstance(),
            () -> getHostServices().showDocument("https://michaelt919.github.io/Kintsugi3DBuilder/Kintsugi3DDocumentation.pdf"));

        welcomeWindowController.init(welcomeStage,
            () -> getHostServices().showDocument("https://michaelt919.github.io/Kintsugi3DBuilder/Kintsugi3DDocumentation.pdf"));

        initAccelerators(welcomeStage.getScene());
        initAccelerators(progressBarsStage.getScene());

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

        SynchronizedWindow mainWindow = new StageSynchronization(primaryStage);

        //set up close and focusGained
        WindowSynchronization.getInstance().addListener(mainWindow);

        primaryStage.setOnCloseRequest(event ->
        {
            // Consume the event and let the window synchronization system close the stage later if the user confirms that they want to exit.
            event.consume();
            WindowSynchronization.getInstance().quit();
        });

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
