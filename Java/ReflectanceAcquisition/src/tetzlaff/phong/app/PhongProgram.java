package tetzlaff.phong.app;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import tetzlaff.gl.helpers.InteractiveGraphics;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.phong.PhongRenderer;
import tetzlaff.ulf.ULFRendererList;
import tetzlaff.window.glfw.GLFWWindow;

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
	        InteractiveApplication app = InteractiveGraphics.createApplication(window, window, renderer);
	        window.show();
			app.run();
		}
        
        GLFWWindow.closeAllWindows();
        System.exit(0);
    }
}
