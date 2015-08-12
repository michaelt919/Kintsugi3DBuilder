package tetzlaff.ulf.app;

import java.awt.EventQueue;

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
    	GLFWWindow window = new GLFWWindow(800, 800, "Unstructured Light Field Renderer", true, 1);
    	window.enableDepthTest();
    	
    	Trackball trackball = new Trackball(1.0f);
        trackball.addAsWindowListener(window);
        
        final ULFRendererList<OpenGLContext> model = new ULFRendererList<OpenGLContext>(window, trackball);        
      
        // Trying to help AWT/Swing not deadlock, but this command also deadlocks!
        EventQueue.invokeLater(new Runnable() {
				public void run() {
					ULFUserInterface gui = new ULFUserInterface(model);
					gui.show();
				}
			}
        );

        InteractiveApplication app = InteractiveGraphics.createApplication(window, window, model.getDrawable());
        window.show();
		app.run();
        
        GLFWWindow.closeAllWindows();
        System.exit(0);
    }
}
