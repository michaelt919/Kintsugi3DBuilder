package tetzlaff.phong.app;

import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import tetzlaff.gl.helpers.InteractiveGraphics;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.phong.PhongRenderer;
import tetzlaff.ulf.ULFRendererList;
import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;
import tetzlaff.window.glfw.GLFWWindow;
import tetzlaff.window.KeyCodes;

public class PhongProgram
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
	        PhongRenderer renderer = new PhongRenderer(window, fileChooser.getSelectedFile(), viewTrackball, lightTrackball);
	        
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
	    		else if (keycode == KeyCodes.ZERO)
	    		{
	    			renderer.setMode(PhongRenderer.NO_TEXTURE_MODE);
	    		}
	    		else if (keycode == KeyCodes.ONE)
	    		{
	    			renderer.setMode(PhongRenderer.FULL_TEXTURE_MODE);
	    		}
	    		else if (keycode == KeyCodes.TWO)
	    		{
	    			renderer.setMode(PhongRenderer.PLASTIC_TEXTURE_MODE);
	    		}
	    		else if (keycode == KeyCodes.THREE)
	    		{
	    			renderer.setMode(PhongRenderer.METALLIC_TEXTURE_MODE);
	    		}
	    		else if (keycode == KeyCodes.FOUR)
	    		{
	    			renderer.setMode(PhongRenderer.DIFFUSE_TEXTURE_MODE);
	    		}
	    		else if (keycode == KeyCodes.FIVE)
	    		{
	    			renderer.setMode(PhongRenderer.NORMAL_TEXTURE_ONLY_MODE);
	    		}
	    		else if (keycode == KeyCodes.SIX)
	    		{
	    			renderer.setMode(PhongRenderer.DIFFUSE_NO_SHADING_MODE);
	    		}
	    		else if (keycode == KeyCodes.EQUAL)
	    		{
	    			if (renderer.getMode() == PhongRenderer.METALLIC_TEXTURE_MODE)
	    			{
	    				renderer.setMetallicIntensity(renderer.getMetallicIntensity() * 1.1f);
	    			}
	    			else
	    			{
	    				renderer.setSpecularIntensity(renderer.getSpecularIntensity() * 1.1f);
	    			}
	    		}
	    		else if (keycode == KeyCodes.MINUS)
	    		{
	    			if (renderer.getMode() == PhongRenderer.METALLIC_TEXTURE_MODE)
	    			{
	    				renderer.setMetallicIntensity(renderer.getMetallicIntensity() * 0.9f);
	    			}
	    			else
	    			{
	    				renderer.setSpecularIntensity(renderer.getSpecularIntensity() * 0.9f);
	    			}
	    		}
	    		else if (keycode == KeyCodes.RIGHT_BRACKET)
	    		{
	    			if (renderer.getMode() == PhongRenderer.FULL_TEXTURE_MODE)
	    			{
		    			renderer.setSpecularRoughnessTextureScale(renderer.getSpecularRoughnessTextureScale() * 1.1f);
	    			}
	    			else
	    			{
		    			renderer.setSpecularRoughness(renderer.getSpecularRoughness() * 1.1f);
	    			}
	    		}
	    		else if (keycode == KeyCodes.LEFT_BRACKET)
	    		{
	    			if (renderer.getMode() == PhongRenderer.FULL_TEXTURE_MODE)
	    			{
		    			renderer.setSpecularRoughnessTextureScale(renderer.getSpecularRoughnessTextureScale() * 0.9f);
	    			}
	    			else
	    			{
		    			renderer.setSpecularRoughness(renderer.getSpecularRoughness() * 0.9f);
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
