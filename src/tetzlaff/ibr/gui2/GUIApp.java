package tetzlaff.ibr.gui2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;

public class GUIApp extends Application{

    @Override
    public void start(Stage menuBarStage) throws Exception {
        //We will make the menu bar our primary stage



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

        //load Parents
        Parent menuBarRoot = FXMLLoader.load(menuBarURL);
        Parent libraryRoot = FXMLLoader.load(libraryURL);
        Parent sceneRoot = FXMLLoader.load(sceneURL);

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

        final double extra = 6;

        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

        menuBarStage.setX(primaryScreenBounds.getMinX()-4);
        menuBarStage.setY(primaryScreenBounds.getMinY());
        menuBarStage.setWidth(primaryScreenBounds.getWidth()+8);
        menuBarStage.show();
        double menuBarHeight = menuBarStage.getHeight();

        libraryStage.setX(primaryScreenBounds.getMinX()-extra);
        libraryStage.setY(primaryScreenBounds.getMinY() + menuBarHeight - extra);
        libraryStage.setHeight(primaryScreenBounds.getHeight() - menuBarHeight + 2*extra);
        libraryStage.setWidth(primaryScreenBounds.getWidth()*librarySection + 2*extra);
        libraryStage.show();

        sceneStage.setX(primaryScreenBounds.getMinX() + primaryScreenBounds.getWidth()*(1-sceneSection) - extra);
        sceneStage.setWidth(primaryScreenBounds.getWidth()*sceneSection + 2*extra);
        sceneStage.setY(primaryScreenBounds.getMinY() + menuBarHeight - extra);
        sceneStage.setHeight(primaryScreenBounds.getHeight() - menuBarHeight + 2*extra);
        sceneStage.show();

        menuBarStage.hide();//this is just to have the menu bar have focus on the application starts, only aesthetic value.
        menuBarStage.show();

    }


    public static void  launchWrapper(String args){
        launch(args);
    }
}
