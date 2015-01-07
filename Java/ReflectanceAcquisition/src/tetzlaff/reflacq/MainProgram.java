package tetzlaff.reflacq;

import java.io.IOException;

import tetzlaff.gl.helpers.InteractiveGraphics;
import tetzlaff.gl.helpers.MultiDrawable;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.lightfield.UnstructuredLightField;
import tetzlaff.lightfield.UnstructuredLightFieldRenderer;
import tetzlaff.window.glfw.GLFWWindow;

public class MainProgram
{
    public static void main(String[] args) 
    {
    	GLFWWindow window = new GLFWWindow(300, 300, "Light Field Renderer", true, 4);
    	
    	Trackball trackball = new Trackball(1.0f);
        trackball.addAsWindowListener(window);
        
        MultiDrawable<UnstructuredLightFieldRenderer> ulfs = new MultiDrawable<UnstructuredLightFieldRenderer>();
        
        try
        {
        	UnstructuredLightField lightField = UnstructuredLightField.loadFromDirectory("pumpkin-warts");
        	ulfs.add(new UnstructuredLightFieldRenderer(window, lightField, trackball));
            InteractiveApplication app = InteractiveGraphics.createApplication(window, window, ulfs);
            window.show();
    		app.run();
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        
        GLFWWindow.closeAllWindows();
    }
}
