package tetzlaff.reflacq;

import tetzlaff.gl.helpers.InteractiveGraphics;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.ulf.ULFRenderableListModel;
import tetzlaff.window.glfw.GLFWWindow;

public class MainProgram
{
    public static void main(String[] args) 
    {
    	GLFWWindow window = new GLFWWindow(800, 800, "Unstructured Light Field Renderer", true, 4);
    	window.enableDepthTest();
    	
    	Trackball trackball = new Trackball(1.0f);
        trackball.addAsWindowListener(window);
        
    	ULFRenderableListModel model = new ULFRenderableListModel(window, trackball);
    	ULFUserInterface gui = new ULFUserInterface(model);
        
        InteractiveApplication app = InteractiveGraphics.createApplication(window, window, model.getDrawable());
        window.show();
        gui.show();
		app.run();
        
        GLFWWindow.closeAllWindows();
        System.exit(0);
    }
}
