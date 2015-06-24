package tetzlaff.reflacq;

import java.io.IOException;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.window.glfw.GLFWWindow;

public class TexGenProgram
{
	public static void main(String[] args)
    {
		TexGenUserInterface gui = new TexGenUserInterface();
    	
		gui.addExecuteButtonActionListener(e -> 
		{
	        try
	        {
	        	OpenGLContext context = new GLFWWindow(800, 800, "Texture Generation");
	    		new TexGenExecutor(context, gui.getCameraFile(), gui.getModelFile(), gui.getImageDirectory(), gui.getMaskDirectory(), gui.getOutputDirectory(), gui.getParameters())
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
