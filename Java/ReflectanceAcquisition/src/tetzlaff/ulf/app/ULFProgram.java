package tetzlaff.ulf.app;

import tetzlaff.gl.helpers.InteractiveGraphics;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.ulf.ULFRendererList;
import tetzlaff.window.glfw.GLFWWindow;

public class ULFProgram
{
    public static void main(String[] args) 
    {
    	GLFWWindow window = new GLFWWindow(800, 800, "Unstructured Light Field Renderer", true);
    	window.enableDepthTest();
    	
    	Trackball trackball = new Trackball(1.0f);
        trackball.addAsWindowListener(window);
        
        ULFRendererList<OpenGLContext> model = new ULFRendererList<OpenGLContext>(window, trackball);
    	ULFUserInterface gui = new ULFUserInterface(model);
        
        InteractiveApplication app = InteractiveGraphics.createApplication(window, window, model.getDrawable());
        window.show();
        gui.show();
		app.run();
        
        GLFWWindow.closeAllWindows();
        System.exit(0);
    }
}
