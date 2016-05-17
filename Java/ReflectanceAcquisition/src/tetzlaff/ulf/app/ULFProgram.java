package tetzlaff.ulf.app;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import tetzlaff.gl.Program;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.helpers.InteractiveGraphics;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.ulf.ULFRendererList;
import tetzlaff.window.glfw.GLFWWindow;

/**
 * ULFProgram is a container for the main entry point of the Unstructured Light Field
 * rendering program.
 * 
 * @author Michael Tetzlaff
 */
public class ULFProgram
{
    /**
     * The main entry point for the Unstructured Light Field (ULF) renderer application.
     * @param args The usual command line arguments
     */
    public static void main(String[] args) 
    {
    	System.getenv();
    	System.setProperty("org.lwjgl.util.DEBUG", "true");
    	
    	// Check for and print supported image formats (some are not as easy as you would think)
    	checkSupportedImageFormats();

    	// Create a GLFW window for integration with LWJGL (part of the 'view' in this MVC arrangement)
    	GLFWWindow window = new GLFWWindow(800, 800, "Unstructured Light Field Renderer", true, 4);
    	window.enableDepthTest();

    	// Add a trackball controller to the window for rotating the object (also responds to mouse scrolling)
    	// This is the 'controller' in the MVC arrangement.
    	Trackball trackball = new Trackball(1.0f);
        trackball.addAsWindowListener(window);
        
        Program<OpenGLContext> program;
        try
        {
    		program = window.getShaderProgramBuilder()
    				.addShader(ShaderType.VERTEX, new File("shaders/ibr/ulr.vert"))
    				.addShader(ShaderType.FRAGMENT, new File("shaders/ibr/ulr.frag"))
    				.createProgram();
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        	throw new IllegalStateException("The shader program could not be initialized.", e);
        }

        // Create a new 'renderer' to be attached to the window and the GUI.
        // This is the object that loads the ULF models and handles drawing them.  This object abstracts
        // the underlying data and provides ways of triggering events via the trackball and the user
        // interface later when it is passed to the ULFUserInterface object.
        ULFRendererList<OpenGLContext> model = new ULFRendererList<OpenGLContext>(window, program, null, trackball);

    	// Create a new application to run our event loop and give it the GLFWWindow for polling
    	// of events and the OpenGL context.  The ULFRendererList provides the drawable.
        InteractiveApplication app = InteractiveGraphics.createApplication(window, window, model.getDrawable());

        // Create a user interface that examines the ULFRendererList for renderer settings and
        // selecting between different loaded models.
        ULFConfigFrame gui = new ULFConfigFrame(model, window.isHighDPI());
        gui.showGUI();
        
    	// Make everything visible and start the event loop
    	window.show();
		app.run();

		// The event loop has terminated so cleanup the windows and exit with a successful return code.
        GLFWWindow.closeAllWindows();
        System.exit(0);
    }
        
    public static void checkSupportedImageFormats()
    {
    	Set<String> set = new HashSet<String>();
    	
        // Get list of all informal format names understood by the current set of registered readers
        String[] formatNames = ImageIO.getReaderFormatNames();

        for (int i = 0; i < formatNames.length; i++) {
            set.add(formatNames[i].toLowerCase());
        }

        System.out.println("Supported image formats: " + set);    	
    }
}
