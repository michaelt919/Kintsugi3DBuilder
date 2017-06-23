package tetzlaff.ibr.alexkautz_workspace.user_interface;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tetzlaff.ibr.alexkautz_workspace.mount_olympus.*;

import java.net.URL;

public class GuiApp extends Application implements Runnable{

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        launch();
    }

    /**
     * The main entry point for all JavaFX applications.
     * The start method is called after the init method has returned,
     * and after the system is ready for the application to begin running.
     * <p>
     * <p>
     * NOTE: This method is called on the JavaFX Application Thread.
     * </p>
     *
     * @param primaryStage the primary stage for this application, onto which
     *                     the application scene can be set. The primary stage will be embedded in
     *                     the browser if the application was launched as an applet.
     *                     Applications may create other stages, if needed, but they will not be
     *                     primary stages and will not be embedded in the browser.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {

        String rootFXMLFileName = "Base.fxml";

        URL please_dont_be_null = getClass().getClassLoader().getResource(rootFXMLFileName);

        if(please_dont_be_null == null) {
            throw new Exception("Cant load " + rootFXMLFileName);
//          System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Nope!~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
//          System.exit(0);
        } else {
            System.out.println(rootFXMLFileName + " Loaded as " + please_dont_be_null.toString());
        }

        primaryStage.setTitle(PassedParameters.get().getName());
        Parent root = FXMLLoader.load(please_dont_be_null);
        primaryStage.setScene(new Scene( root,
                1200, 800
        ));
        primaryStage.show();




    }
}
