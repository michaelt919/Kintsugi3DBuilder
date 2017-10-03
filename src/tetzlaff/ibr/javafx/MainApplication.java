package tetzlaff.ibr.javafx;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Screen;
import javafx.stage.Stage;
import tetzlaff.gl.window.Key;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.ModifierKeysBuilder;
import tetzlaff.ibr.app.Rendering;
import tetzlaff.ibr.app.SynchronizedWindow;
import tetzlaff.ibr.app.WindowSynchronization;
import tetzlaff.ibr.javafx.controllers.menubar.MenubarController;
import tetzlaff.ibr.javafx.controllers.scene.RootSceneController;
import tetzlaff.ibr.javafx.controllers.scene.SceneModel;
import tetzlaff.ibr.javafx.internal.CameraModelImpl;
import tetzlaff.ibr.javafx.internal.EnvironmentModelImpl;
import tetzlaff.ibr.javafx.internal.LightingModelImpl;
import tetzlaff.ibr.javafx.internal.ObjectModelImpl;
import tetzlaff.ibr.tools.DragToolType;
import tetzlaff.ibr.tools.KeyPressToolType;
import tetzlaff.ibr.tools.ToolBindingModel;
import tetzlaff.util.KeyPress;
import tetzlaff.util.MouseMode;

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
            // TODO make this not hang up the program when called from the JavaFX thread.
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

    @Override
    public void start(Stage primaryStage) throws IOException
    {

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
        libraryStage.setTitle("Library");
        libraryStage.setScene(new Scene(libraryRoot));

        Stage sceneStage = new Stage();
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

        //get models
        CameraModelImpl cameraModel = InternalModels.getInstance().getCameraModel();
        EnvironmentModelImpl environmentMapModel = InternalModels.getInstance().getEnvironmentModel();
        LightingModelImpl lightingModel = InternalModels.getInstance().getLightingModel();
        ObjectModelImpl objectModel = InternalModels.getInstance().getObjectModel();

        SceneModel sceneModel = new SceneModel();

        //distribute to controllers
        sceneController.init(cameraModel, lightingModel, environmentMapModel, objectModel, sceneModel);
        menuBarController.init(primaryStage.getScene().getWindow(), Rendering.getRequestQueue(), InternalModels.getInstance(), sceneModel);

        // Bind tools
        ToolBindingModel toolBindingModel = MultithreadModels.getInstance().getToolModel();

        toolBindingModel.setDragTool(new MouseMode(0, ModifierKeys.NONE), DragToolType.ORBIT);
        toolBindingModel.setDragTool(new MouseMode(1, ModifierKeys.NONE), DragToolType.PAN);
        toolBindingModel.setDragTool(new MouseMode(2, ModifierKeys.NONE), DragToolType.PAN);
        toolBindingModel.setDragTool(new MouseMode(0, ModifierKeysBuilder.begin().alt().end()), DragToolType.TWIST);
        toolBindingModel.setDragTool(new MouseMode(1, ModifierKeysBuilder.begin().alt().end()), DragToolType.DOLLY);
        toolBindingModel.setDragTool(new MouseMode(2, ModifierKeysBuilder.begin().alt().end()), DragToolType.DOLLY);
        toolBindingModel.setDragTool(new MouseMode(0, ModifierKeysBuilder.begin().shift().end()), DragToolType.ROTATE_ENVIRONMENT);
        toolBindingModel.setDragTool(new MouseMode(1, ModifierKeysBuilder.begin().shift().end()), DragToolType.FOCAL_LENGTH);
        toolBindingModel.setDragTool(new MouseMode(2, ModifierKeysBuilder.begin().shift().end()), DragToolType.FOCAL_LENGTH);
        toolBindingModel.setDragTool(new MouseMode(1, ModifierKeysBuilder.begin().control().shift().end()), DragToolType.LOOK_AT_POINT);
        toolBindingModel.setDragTool(new MouseMode(2, ModifierKeysBuilder.begin().control().shift().end()), DragToolType.LOOK_AT_POINT);
        toolBindingModel.setDragTool(new MouseMode(0, ModifierKeysBuilder.begin().control().end()), DragToolType.OBJECT_ROTATION);
        toolBindingModel.setDragTool(new MouseMode(1, ModifierKeysBuilder.begin().control().end()), DragToolType.OBJECT_CENTER);
        toolBindingModel.setDragTool(new MouseMode(2, ModifierKeysBuilder.begin().control().end()), DragToolType.OBJECT_CENTER);
        //toolBindingModel.setDragTool(new MouseMode(0, ModifierKeysBuilder.begin().control().alt().end()), DragToolType.OBJECT_TWIST);

        toolBindingModel.setKeyPressTool(new KeyPress(Key.UP, ModifierKeys.NONE), KeyPressToolType.ENVIRONMENT_BRIGHTNESS_UP_LARGE);
        toolBindingModel.setKeyPressTool(new KeyPress(Key.DOWN, ModifierKeys.NONE), KeyPressToolType.ENVIRONMENT_BRIGHTNESS_DOWN_LARGE);
        toolBindingModel.setKeyPressTool(new KeyPress(Key.RIGHT, ModifierKeys.NONE), KeyPressToolType.ENVIRONMENT_BRIGHTNESS_UP_SMALL);
        toolBindingModel.setKeyPressTool(new KeyPress(Key.LEFT, ModifierKeys.NONE), KeyPressToolType.ENVIRONMENT_BRIGHTNESS_DOWN_SMALL);
        toolBindingModel.setKeyPressTool(new KeyPress(Key.L, ModifierKeys.NONE), KeyPressToolType.TOGGLE_LIGHTS);
        toolBindingModel.setKeyPressTool(new KeyPress(Key.L, ModifierKeysBuilder.begin().control().end()), KeyPressToolType.TOGGLE_LIGHT_WIDGETS);

        SynchronizedWindow menuBarWindow = new StageSynchronization(primaryStage);

        //set up close and focusGained
        WindowSynchronization.getInstance().addListener(menuBarWindow);

        sceneStage.setOnCloseRequest(event ->
        {
            boolean closeConfirmed = WindowSynchronization.getInstance().quit();
            if (!closeConfirmed)
            {
                event.consume();
            }
        });

        primaryStage.setOnCloseRequest(event ->
        {
            boolean closeConfirmed = WindowSynchronization.getInstance().quit();
            if (!closeConfirmed)
            {
                event.consume();
            }
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
    }

    public static void launchWrapper(String args)
    {
        launch(args);
    }
}
