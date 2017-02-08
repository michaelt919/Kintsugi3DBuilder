package tetzlaff.texturefit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;



import java.io.PrintStream;

import javax.swing.JOptionPane;



import tetzlaff.gl.exceptions.GLOutOfMemoryException;
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
				System.gc(); // Suggest garbage collection.
	        	ex.printStackTrace();
	        	JOptionPane.showMessageDialog(gui, ex.getMessage(), "Error reading file (" + ex.getClass().getName() + ")", JOptionPane.ERROR_MESSAGE);
	        }
	        catch (GLOutOfMemoryException ex)
	        {
				System.gc(); // Suggest garbage collection.
	        	ex.printStackTrace();
	        	ByteArrayOutputStream stackTraceStream = new ByteArrayOutputStream();
	        	PrintStream stackTracePrintStream = new PrintStream(stackTraceStream);
	        	ex.printStackTrace(stackTracePrintStream);
	        	stackTracePrintStream.flush();
	        	stackTracePrintStream.close();
	        	JOptionPane.showMessageDialog(gui, "You've run out of graphics memory.  " +
	        			"Reduce the number of photos, or try using either smaller photos or pre-projected photos to reduce the amount of graphics memory usage.  Stack Trace: " + 
	        			stackTraceStream.toString(), "Out of graphics memory", JOptionPane.ERROR_MESSAGE);
	        }
	        catch (Exception ex)
	        {
	        	System.gc(); // Suggest garbage collection.
	        	ex.printStackTrace();
	        	ByteArrayOutputStream stackTraceStream = new ByteArrayOutputStream();
	        	PrintStream stackTracePrintStream = new PrintStream(stackTraceStream);
	        	ex.printStackTrace(stackTracePrintStream);
	        	stackTracePrintStream.flush();
	        	stackTracePrintStream.close();
	        	JOptionPane.showMessageDialog(gui, stackTraceStream.toString(), ex.getClass().getName(), JOptionPane.ERROR_MESSAGE);
	        }
		});
		
		gui.setVisible(true);
	}
}
