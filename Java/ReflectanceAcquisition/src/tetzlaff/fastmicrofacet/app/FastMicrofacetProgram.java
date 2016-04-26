package tetzlaff.fastmicrofacet.app;

import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import tetzlaff.fastmicrofacet.FastMicrofacetRenderer;
import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.FirstPersonController;
import tetzlaff.gl.helpers.InteractiveGraphics;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.imagebasedmicrofacet.TrackballLightController;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.window.KeyCodes;
import tetzlaff.window.glfw.GLFWWindow;

public class FastMicrofacetProgram
{
    public static void main(String[] args) 
    {
    	GLFWWindow window = new GLFWWindow(800, 800, "Phong Renderer", true, 4);
    	window.enableDepthTest();
    	
    	TrackballLightController lightController = new TrackballLightController();
        lightController.addAsWindowListener(window);
         
        FirstPersonController fpController = new FirstPersonController();
        fpController.addAsWindowListener(window);
         
        window.addMouseButtonPressListener((win, buttonIndex, mods) -> 
        {
         	fpController.setEnabled(false);
     	});
         
        // Hybrid FP + Trackball controls
        CameraController cameraController = () -> fpController.getViewMatrix().times(lightController.asCameraController().getViewMatrix());
        
        JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
		fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
		fileChooser.setFileFilter(new FileNameExtensionFilter("Wavefront OBJ files (.obj)", "obj"));
		
		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		{
	        FastMicrofacetRenderer<OpenGLContext> renderer = new FastMicrofacetRenderer<OpenGLContext>(window, fileChooser.getSelectedFile(), cameraController, lightController);
	        
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
	    		else if (keycode == KeyCodes.P)
	    		{
	    			System.out.println("Reloading shaders...");
	    			try
	    			{
	    				renderer.reloadShaders();
	    			}
	    			catch(RuntimeException e)
	    			{
	    				e.printStackTrace(System.err);
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
