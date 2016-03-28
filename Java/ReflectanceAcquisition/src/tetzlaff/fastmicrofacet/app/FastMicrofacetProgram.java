package tetzlaff.fastmicrofacet.app;

import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import tetzlaff.fastmicrofacet.FastMicrofacetRenderer;
import tetzlaff.gl.helpers.InteractiveGraphics;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.window.KeyCodes;
import tetzlaff.window.glfw.GLFWWindow;

public class FastMicrofacetProgram
{
    public static void main(String[] args) 
    {
    	GLFWWindow window = new GLFWWindow(800, 800, "Phong Renderer", true, 4);
    	window.enableDepthTest();
    	
    	Trackball viewTrackball = new Trackball(1.0f, 0, -1, true);
    	viewTrackball.addAsWindowListener(window);
    	
    	Trackball lightTrackball = new Trackball(1.0f, 1, -1, false);
    	lightTrackball.addAsWindowListener(window);
        
        JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
		fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
		fileChooser.setFileFilter(new FileNameExtensionFilter("Wavefront OBJ files (.obj)", "obj"));
		
		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		{
	        FastMicrofacetRenderer<OpenGLContext> renderer = new FastMicrofacetRenderer<OpenGLContext>(window, fileChooser.getSelectedFile(), viewTrackball, lightTrackball);
	        
	    	window.addKeyPressListener((targetWindow, keycode, mods) -> 
	    	{
	    		if (keycode == KeyCodes.R)
	    		{
	    			JFileChooser vsetFileChooser = new JFileChooser(new File("").getAbsolutePath());
	    			vsetFileChooser.setDialogTitle("Choose a Target VSET File");
	    			vsetFileChooser.removeChoosableFileFilter(vsetFileChooser.getAcceptAllFileFilter());
	    			vsetFileChooser.setFileFilter(new FileNameExtensionFilter("View Set files (.vset)", "vset"));
	    			if (vsetFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
	    			{
	    				JFileChooser exportFileChooser = new JFileChooser(vsetFileChooser.getSelectedFile().getParentFile());
	    				exportFileChooser.setDialogTitle("Choose an Export Directory");
	    				exportFileChooser.removeChoosableFileFilter(exportFileChooser.getAcceptAllFileFilter());
	    				exportFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    				
	    				if (exportFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
	    				{
	    					try 
	    					{
	    						renderer.resample(vsetFileChooser.getSelectedFile(), exportFileChooser.getSelectedFile());
	    					} 
	    					catch (IOException ex) 
	    					{
	    						ex.printStackTrace();
	    					}
	    				}
	    			}
	    		}
	    	});
	        
	        InteractiveApplication app = InteractiveGraphics.createApplication(window, window, renderer);
	        window.show();
			app.run();
		}
        
        GLFWWindow.closeAllWindows();
        System.exit(0);
    }
}
