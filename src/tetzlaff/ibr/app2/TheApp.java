package tetzlaff.ibr.app2;

import tetzlaff.ibr.rendering2.*;
import tetzlaff.ibr.rendering2.tools.ToolModel2;
import tetzlaff.ibr.gui2.app.GUIApp2;

import com.sun.glass.ui.Application;

public class TheApp {

    private static RootModel rootModel;

    public static void main(String[] args) {


        //allow render thread to modify user interface thread
        System.setProperty("glass.disableThreadChecks", "true");
        //TODO see com.sun.glass.ui.Application.java line 434




        System.out.println("Creating Models");
        createModels();

        System.out.println("Starting JavaFX UI");
        startJavaFXUI();

        System.out.println("Starting Render Window");
        startRenderWindow();

        System.out.println("Boot Complete");

    }

    private static void createModels(){
        rootModel = new RootModel(
                new CameraModel3(),
                new LightModel3(),
                new ToolModel3()
        );
    }

    private static void startJavaFXUI(){
        (new Thread(new ThreadableUI())).start();
    }

    private static void startRenderWindow(){
//        (new Thread(new ThreadableRender())).start();
        IBRelight2.runProgram();
    }

    public static RootModel getRootModel() {
        return rootModel;
    }
}
