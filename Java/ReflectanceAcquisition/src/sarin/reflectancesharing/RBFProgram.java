package sarin.reflectancesharing;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.lwjgl.LWJGLUtil;

import tetzlaff.gl.helpers.InteractiveGraphics;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.helpers.GLExceptionTranslator;
import tetzlaff.interactive.InteractiveApplication;
//import tetzlaff.window.ModifierKeys;
//import tetzlaff.window.Window;
import tetzlaff.window.WindowPosition;
import tetzlaff.window.WindowSize;
import tetzlaff.window.glfw.GLFWWindow;
import tetzlaff.window.listeners.KeyPressListener;

/**
 * ULFProgram is a container for the main entry point of the Unstructured Light Field
 * rendering program.
 * 
 * @author Michael Tetzlaff
 */
public class RBFProgram
{
	/**
	 * The GLFW window object that is the main rendering window
	 */
	private static GLFWWindow window = null;

	// debug mode
	public static int debugMode = 0;
	
	// increment rbf function
	public static int increment = 0;
	
	/**
	 * Reference to the main interactive application
	 */
	private static InteractiveApplication app;
	
    public static void main(String[] args)
    {
    	// Initialize the system environment vars and LWJGL
    	System.getenv();
    	LWJGLUtil.initialize();

    	// Output some system information
        System.out.println("\n****** Sys Info *****");	    	
    	System.out.println("OS      : " + System.getProperty("os.name"));
    	System.out.println("Arch    : " + System.getProperty("os.arch"));
    	System.out.println("Version : " + System.getProperty("os.version"));
        System.out.println("***** Java Info *****");
        System.out.println("* Version : " + System.getProperty("java.version"));
        System.out.println("* Vendor  : " + System.getProperty("java.vendor"));
        System.out.println("* Home    : " + System.getProperty("java.home"));
        System.out.println("****** JVM Info *****");
        System.out.println("* Name         : " + System.getProperty("java.vm.name"));
        System.out.println("* Version      : " + System.getProperty("java.vm.version"));
        System.out.println("* Vendor       : " + System.getProperty("java.vm.vendor"));
        System.out.println("* Spec Name    : " + System.getProperty("java.vm.specification.name"));
        System.out.println("* Spec Version : " + System.getProperty("java.vm.specification.version"));
        System.out.println("* Spec Vendor  : " + System.getProperty("java.vm.specification.vendor"));
        System.out.println("*********************\n");

    	// Check for and print supported image formats (some are not as easy as you would think)
    	checkSupportedImageFormats();

    	// Create a GLFW window for integration with LWJGL (part of the 'view' in this MVC arrangement)
    	window = new GLFWWindow(800, 800, "Unstructured Light Field Renderer", true, 4);
    	window.enableDepthTest();
    	
    	// Add a trackball controller to the window for rotating the object (also responds to mouse scrolling)
    	// This is the 'controller' in the MVC arrangement.
    	Trackball trackball = new Trackball(1.0f);
        trackball.addAsWindowListener(window);

        // TODO Sarin
        // Load data from matrix solution
        
        // What constructor variables do you need?
        //String geometryFile = "C:\\Users\\Sarin\\Downloads\\sphere-phong-100x100px\\manifold.obj";
        //String geometryFile = "C:\\Users\\Sarin\\Downloads\\sphere-100x100-reduced\\sphere.obj";
        //String geometryFile = "C:\\Users\\Sarin\\Downloads\\sphere2\\sphere.obj";
        //String geometryFile = "C:\\Users\\Sarin\\Downloads\\sphere-phong-512x512px\\manifold.obj";
        String geometryFile = "C:\\Users\\Sarin\\Downloads\\sphere-less-specular\\sphere.obj";
        //String geometryFile = "C:\\Users\\Sarin\\Downloads\\sphere-diffuse\\sphere.obj";
       // String geometryFile = "C:\\Users\\Sarin\\Downloads\\sphere-specular\\sphere.obj";
        

        RBFRenderer<OpenGLContext> renderer = new RBFRenderer<OpenGLContext>(window, new File(geometryFile), trackball);

    	KeyPressListener listener = (window, keycode, mods) ->
    	{
    		System.out.println("Key pressed: " + keycode);
    		// key 1
    		if( keycode == 49 ){
    			debugMode = 1;
    		}
    		// key 2 
    		else if ( keycode == 50 ){
    			debugMode = 2;
    		}
    		// key 0
    		else if ( keycode == 48 ){
    			debugMode = 0;
    		}
    		// key 3
    		else if ( keycode == 51 ){
    			debugMode = 3;
    		}
    		// key 4
    		else if ( keycode == 52 ){
    			debugMode = 4;
    		}
    		// key 5 
    		else if ( keycode == 53 ){
    			debugMode = 5;
    		}
    		// key 6
    		else if ( keycode == 54 ){
    			debugMode = 6;
    		}
    		// key 7
    		else if ( keycode == 55 ){
    			debugMode = 7;
    		}
    		// key 8
    		else if ( keycode == 56 ){
    			debugMode = 8;
    		}
    		// key 9
    		else if ( keycode == 57 ){
    			debugMode = 9;
    		}
    		else if ( keycode == 65 ){
    			debugMode = 10;
    		}
    		else if( keycode == 265){
    			debugMode = 11;
    			increment += 1;
    			System.out.println("Increment: " + increment);
    			renderer.setIncrementMode(increment);
    		}
    		else if( keycode == 264){
    			debugMode = 11;
    			increment -= 1;
    			System.out.println("Increment: " + increment);
    			renderer.setIncrementMode(increment);
    		}
			renderer.setDebugMode(debugMode);
			System.out.println("Debug mode: " + debugMode);
    	};
    	
    	window.addKeyPressListener(listener);
        
    	// Create a new application to run our event loop and give it the GLFWWindow for polling
    	// of events and the OpenGL context.  The ULFRendererList provides the drawable.
        app = InteractiveGraphics.createApplication(window, window, renderer);
        
        app.setExceptionTranslator(new GLExceptionTranslator());
                
        // Make everything visible (important things happen here for Qt and GLFW    
        window.show();
        
        /* Doesn't work on Windows 8.1, just leaving out for now
         * TODO: Need to find a reliable way to detect system scaling in Windows
        // Warn about scaled GUIs on windows
        if(System.getProperty("os.name").startsWith("Windows") && gui.physicalDpiX() != gui.logicalDpiX())
        {
        	QMessageBox.warning(gui, "Window Scaling Detected",
        			"It appears you are using Windows OS scaling (common for HighDPI/Retina displays).\n" +
        			"This may cause problems with the user interface.  We recommend disabling this option\n" +
        			"and instead selecting a desktop resolution that is lower than the native resolution.\n\n" +
        			"Scaling and resolution are set in the control panel 'Display' area.");
        }
        */

        // Set the GL window (so other funcs can access) and start the main event loop
		app.run();
    }

    
    /**
     * A simple method to check and print out all image formats supported by the ImageIO API
     * including plugins from JAI.
     */
    private static void checkSupportedImageFormats()
    {
    	Set<String> set = new HashSet<String>();
    	
        // Get list of all informal format names understood by the current set of registered readers
        String[] formatNames = ImageIO.getReaderFormatNames();

        for (int i = 0; i < formatNames.length; i++) {
            set.add(formatNames[i].toLowerCase());
        }

        System.out.println("Supported image formats: " + set);
    }
    
    public static WindowSize getRenderingWindowSize()
    {
    	return window.getWindowSize();
    }
    
    public static WindowPosition getRendringWindowPosition()
    {
    	return window.getWindowPosition();
    }
}
