package tetzlaff.ibr.alexkautz_workspace.mount_olympus;

import tetzlaff.ibr.alexkautz_workspace.IBRelight2;
import tetzlaff.ibr.alexkautz_workspace.user_interface.GuiApp;

public class RunMe {
    public static void main(String[] args) {
        System.out.println("Alex Kautz");
        System.out.println("Start Main");

        System.out.println("Initlising Paramiters");
        PassedParameters.init(null, "Hello World");

        System.out.println("Starting JavaFX UI");
        startJavaFXUI();

        System.out.println("Starting Render Window");
        startRenderWindow();

        System.out.println("End Main");
    }

    private static void startJavaFXUI(){
        Thread guiThread = new Thread(new GuiApp());

        guiThread.start();
    }

    private static void startRenderWindow(){

        IBRelight2.runProgram();
    }

}
