package tetzlaff.texturefit;

import java.io.IOException;

import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.window.glfw.GLFWWindow;

public class TextureFitProgram
{
	public static void main(String[] args)
    {
		TextureFitUserInterface gui = new TextureFitUserInterface();
    	
		gui.addExecuteButtonActionListener(e -> 
		{
	        try
	        {
	        	OpenGLContext context = new GLFWWindow(800, 800, "Texture Generation");
	    		new TextureFitExecutor<OpenGLContext>(context, gui.getCameraFile(), gui.getModelFile(), gui.getImageDirectory(), gui.getMaskDirectory(), 
	    				gui.getRescaleDirectory(), gui.getOutputDirectory(), gui.getParameters())
	    				.execute();
		        GLFWWindow.closeAllWindows();
		        System.out.println("Process terminated with no errors.");
	        }
	        catch (IOException ex)
	        {
	        	ex.printStackTrace();
	        }
		});
		
		gui.show();
	}
}
