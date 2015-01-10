package tetzlaff.reflacq;

import java.util.Arrays;

import tetzlaff.gl.helpers.InteractiveGraphics;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.ulf.ULFRendererList;
import tetzlaff.ulf.ULFToTexturesList;
import tetzlaff.window.glfw.GLFWWindow;

public class TexGenProgram
{
    public static void main(String[] args) 
    {
    	GLFWWindow ulrWindow = new GLFWWindow(800, 800, "Unstructured Light Field Renderer", 280, 140, true, 4);
    	ulrWindow.enableDepthTest();
    	ulrWindow.enableBackFaceCulling();
    	
    	GLFWWindow ulfToTexWindow = new GLFWWindow(800, 800, "Texture Space Visualization", 1100, 140, true, 4);
    	ulfToTexWindow.enableDepthTest();
    	ulfToTexWindow.enableBackFaceCulling();
    	
    	Trackball trackball = new Trackball(1.0f);
        trackball.addAsWindowListener(ulrWindow);
        
    	ULFRendererList model = new ULFRendererList(ulrWindow, trackball);
    	ULFToTexturesList ulfToTexList = new ULFToTexturesList(ulfToTexWindow, trackball);
    	ULFUserInterface gui = new ULFUserInterface(model);
    	
        gui.addSelectedLightFieldListener(lf -> 
        {
        	ulfToTexList.setSelectedItem(lf);
        	if (ulfToTexList.getSelectedItem() == null)
        	{
        		try 
        		{
					ulfToTexList.addFromDirectory(lf.directoryPath);
				} 
        		catch (Exception e)
        		{
					e.printStackTrace();
				}
        	}
        });
        
        InteractiveApplication ulrApp = InteractiveGraphics.createApplication(ulrWindow, ulrWindow, model.getDrawable());
        InteractiveApplication ulfToTexApp = InteractiveGraphics.createApplication(ulfToTexWindow, ulfToTexWindow, ulfToTexList.getDrawable());
        
        gui.show();
        ulrWindow.show();
        ulfToTexWindow.show();
		InteractiveApplication.runSimultaneous(Arrays.asList(ulrApp, ulfToTexApp));
        
        GLFWWindow.closeAllWindows();
        System.exit(0);
    }
}
