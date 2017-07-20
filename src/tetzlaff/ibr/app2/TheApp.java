package tetzlaff.ibr.app2;

import tetzlaff.ibr.alexkautz_workspace.mount_olympus.PassedParameters;
import tetzlaff.ibr.app2.IBRelight2;
import tetzlaff.ibr.alexkautz_workspace.user_interface.GuiApp;

public class TheApp {

    public static void main(String[] args) {

        System.out.println("Creating Models");

        System.out.println("Starting JavaFX UI");
        startJavaFXUI();

        System.out.println("Starting Render Window");
        startRenderWindow();

    }

    private static void createModels(){

    }

    private static void startJavaFXUI(){
        Thread guiThread = new Thread(new GuiApp());

        guiThread.start();
    }

    private static void startRenderWindow(){

        IBRelight2.runProgram();
    }

}
