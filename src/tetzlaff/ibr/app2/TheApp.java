package tetzlaff.ibr.app2;

import tetzlaff.ibr.rendering2.CameraModel3;
import tetzlaff.ibr.rendering2.tools.ToolModel2;
import tetzlaff.ibr.gui2.app.GUIApp2;
import tetzlaff.ibr.rendering2.CameraModel2;
import tetzlaff.ibr.rendering2.IBRelight2;
import tetzlaff.ibr.rendering2.LightModel2;

public class TheApp {

    private static RootModel rootModel;

    public static void main(String[] args) {

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
                new LightModel2(4),
                new ToolModel2()
        );
    }

    private static void startJavaFXUI(){
        (new Thread(new ThreadableUI())).start();
    }

    private static void startRenderWindow(){
        (new Thread(new ThreadableRender())).start();
    }

    public static RootModel getRootModel() {
        return rootModel;
    }
}
