package tetzlaff.ibr.gui2.app;

import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tetzlaff.ibr.app2.TheApp;
import tetzlaff.ibr.gui2.controllers.menu_bar.MenubarController;
import tetzlaff.ibr.gui2.controllers.scene.RootSceneController;
import tetzlaff.ibr.rendering2.CameraModel3;
import tetzlaff.ibr.rendering2.EnvironmentMapModel3;
import tetzlaff.ibr.rendering2.LightModel3;
import tetzlaff.ibr.rendering2.ToolModel3;

public class GUIApp2 extends Application{

    @Override
    public void start(Stage menuBarStage) throws Exception {





        //get FXML URLs
        final String menuBarFXMLFileName = "fxml/menu_bar/MenuBar.fxml";
        final URL menuBarURL = getClass().getClassLoader().getResource(menuBarFXMLFileName);
        assert menuBarURL != null: "cant find " + menuBarFXMLFileName;

        final String libraryFXMLFileName = "fxml/library/Library.fxml";
        final URL libraryURL = getClass().getClassLoader().getResource(libraryFXMLFileName);
        assert libraryURL != null: "cant find " + libraryFXMLFileName;

        final String sceneFXMLFileName = "fxml/scene/RootScene.fxml";
        final URL sceneURL = getClass().getClassLoader().getResource(sceneFXMLFileName);
        assert sceneURL != null: "cant find " + sceneFXMLFileName;

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
        menuBarStage.setTitle("IBR2 Menu Bar");
        menuBarStage.setScene(new Scene(menuBarRoot));

        Stage libraryStage = new Stage();
        libraryStage.setTitle("IBR2 Library");
        libraryStage.setScene(new Scene(libraryRoot));

        Stage sceneStage = new Stage();
        sceneStage.setTitle("IBR2 Scene");
        sceneStage.setScene(new Scene(sceneRoot));

        //set positions
        final double librarySection = 0.2;
        final double sceneSection = 0.3;

        final double extra = 0;

        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

        menuBarStage.setX(primaryScreenBounds.getMinX()-4);
        menuBarStage.setY(primaryScreenBounds.getMinY());
        menuBarStage.setWidth(primaryScreenBounds.getWidth()+8);

        menuBarStage.initStyle(StageStyle.UNDECORATED);

        menuBarStage.show();
        double menuBarHeight = menuBarStage.getHeight();

        libraryStage.setX(primaryScreenBounds.getMinX()-extra);
        libraryStage.setY(primaryScreenBounds.getMinY() + menuBarHeight - extra);
        libraryStage.setHeight(primaryScreenBounds.getHeight() - menuBarHeight + 2*extra);
        libraryStage.setWidth(primaryScreenBounds.getWidth()*librarySection + 2*extra);
        //libraryStage.show();

        sceneStage.setX(primaryScreenBounds.getMinX() + primaryScreenBounds.getWidth()*(1-sceneSection) - extra);
        sceneStage.setWidth(primaryScreenBounds.getWidth()*sceneSection + 2*extra);
        sceneStage.setY(primaryScreenBounds.getMinY() + menuBarHeight - extra);
        sceneStage.setHeight(primaryScreenBounds.getHeight() - menuBarHeight + 2*extra);

        sceneStage.initStyle(StageStyle.UNDECORATED);
        sceneStage.show();

        menuBarStage.hide();//this is just to have the menu bar have focus on the application starts, only aesthetic value.
        menuBarStage.show();




        //get models
        final CameraModel3 cameraModel3 = TheApp.getRootModel().getCameraModel3();
        final EnvironmentMapModel3 environmentMapModel3 = TheApp.getRootModel().getEnvironmentMapModel3();
        final LightModel3 lightModel3 = TheApp.getRootModel().getLightModel3();
        final ToolModel3 toolModel3 = TheApp.getRootModel().getToolModel3();

        //distribute to controllers
        sceneController.init2(cameraModel3, lightModel3, environmentMapModel3, toolModel3);
        menuBarController.init2(toolModel3);

    }


    public static void  launchWrapper(String args){
        launch(args);
    }
}
