package tetzlaff.ulf.app;

import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.lwjgl.LWJGLUtil;

import com.trolltech.qt.core.QCoreApplication;
import com.trolltech.qt.core.Qt.ApplicationAttribute;
import com.trolltech.qt.gui.QApplication;

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
    	LWJGLUtil.initialize();
    	
    	// Check for and print supported image formats (some are not as easy as you would think)
    	checkSupportedImageFormats();

    	// Create a GLFW window for integration with LWJGL (part of the 'view' in this MVC arrangement)
    	GLFWWindow window = new GLFWWindow(800, 800, "Unstructured Light Field Renderer", true, 4);
    	window.enableDepthTest();

    	// Add a trackball controller to the window for rotating the object (also responds to mouse scrolling)
    	// This is the 'controller' in the MVC arrangement.
    	Trackball trackball = new Trackball(1.0f);
        trackball.addAsWindowListener(window);

        // Create a new 'renderer' to be attached to the window and the GUI.
        // This is the object that loads the ULF models and handles drawing them.  This object abstracts
        // the underlying data and provides ways of triggering events via the trackball and the user
        // interface later when it is passed to the ULFUserInterface object.
        ULFRendererList<OpenGLContext> model = new ULFRendererList<OpenGLContext>(window, trackball);

    	// Create a new application to run our event loop and give it the GLFWWindow for polling
    	// of events and the OpenGL context.  The ULFRendererList provides the drawable.
        InteractiveApplication app = InteractiveGraphics.createApplication(window, window, model.getDrawable());

        // Fire up the Qt Interface
		// Prepare the Qt GUI system
        QApplication.initialize(args);
        QCoreApplication.setOrganizationName("UW Stout");
        QCoreApplication.setOrganizationDomain("uwstout.edu");
        QCoreApplication.setApplicationName("PhotoScan Helper");
        
        // As far as I can tell, the OS X native menu bar doesn't work in Qt Jambi
        // The Java process owns the native menu bar and won't relinquish it to Qt
        QApplication.setAttribute(ApplicationAttribute.AA_DontUseNativeMenuBar);
        
        // Create a user interface that examines the ULFRendererList for renderer settings and
        // selecting between different loaded models.
        ULFConfigQWidget gui = new ULFConfigQWidget(model, window.isHighDPI(), null);
        gui.showGUI();        
        app.addPollable(gui);
        
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
