package tetzlaff.ibr.javafx;

import java.io.IOException;
import java.net.URL;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import tetzlaff.ibr.app.Rendering;
import tetzlaff.ibr.app.SynchronizedWindow;
import tetzlaff.ibr.app.WindowSynchronization;
import tetzlaff.ibr.javafx.controllers.menu_bar.MenubarController;
import tetzlaff.ibr.javafx.controllers.scene.RootSceneController;
import tetzlaff.ibr.javafx.models.*;

public class JavaFXApp extends Application
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
    }

    @Override
    public void start(Stage menuBarStage) throws IOException
    {

        //get FXML URLs
        String menuBarFXMLFileName = "fxml/menu_bar/MenuBar.fxml";
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
        menuBarStage.setTitle("IBRelight");
        menuBarStage.setScene(new Scene(menuBarRoot));

        Stage libraryStage = new Stage();
        libraryStage.setTitle("Library");
        libraryStage.setScene(new Scene(libraryRoot));

        Stage sceneStage = new Stage();
        sceneStage.setTitle("Scene");
        sceneStage.setScene(new Scene(sceneRoot));

        //set positions

        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

        menuBarStage.setX(primaryScreenBounds.getMinX());
        menuBarStage.setY(primaryScreenBounds.getMinY());

        menuBarStage.setResizable(false);

        menuBarStage.show();

        libraryStage.setX(primaryScreenBounds.getMinX());
        libraryStage.setY(primaryScreenBounds.getMinY());
        libraryStage.setHeight(primaryScreenBounds.getHeight());
        libraryStage.initOwner(menuBarStage.getScene().getWindow());

        //libraryStage.show();

        sceneStage.setX(primaryScreenBounds.getMinX() + primaryScreenBounds.getWidth() - 400);
        sceneStage.setY(primaryScreenBounds.getMinY());
        sceneStage.setHeight(primaryScreenBounds.getHeight());
        sceneStage.initOwner(menuBarStage.getScene().getWindow());

        sceneStage.show();
        sceneStage.setMinWidth(sceneStage.getWidth());
        sceneStage.setMaxWidth(sceneStage.getWidth());

        menuBarStage.requestFocus();

        //get models
        JavaFXCameraModel cameraModel = JavaFXModelAccess.getInstance().getCameraModel();
        JavaFXEnvironmentMapModel environmentMapModel = JavaFXModelAccess.getInstance().getEnvironmentMapModel();
        JavaFXLightingModel lightingModel = JavaFXModelAccess.getInstance().getLightingModel();
        JavaFXToolSelectionModel toolModel = JavaFXModelAccess.getInstance().getToolModel();

        //distribute to controllers
        sceneController.init(cameraModel, lightingModel, environmentMapModel, toolModel);
        menuBarController.init(menuBarStage.getScene().getWindow(), toolModel, Rendering.getRequestQueue());

        SynchronizedWindow menuBarWindow = new StageSynchronization(menuBarStage);

        //set up close and focusGained
        WindowSynchronization.getInstance().addListener(menuBarWindow);

        sceneStage.setOnCloseRequest(event -> WindowSynchronization.getInstance().quit());
        menuBarStage.setOnCloseRequest(event -> WindowSynchronization.getInstance().quit());

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
    }

    public static void launchWrapper(String args)
    {
        launch(args);
    }
}
