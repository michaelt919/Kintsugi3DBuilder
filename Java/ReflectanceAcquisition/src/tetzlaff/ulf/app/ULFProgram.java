package tetzlaff.ulf.app;

import com.bugsplatsoftware.client.BugSplat;

import java.io.File;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.lwjgl.LWJGLUtil;

import com.trolltech.qt.core.QCoreApplication;
import com.trolltech.qt.core.Qt.ApplicationAttribute;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QStyle;

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
	public static final String VERSION = "1.0a";
	public static final String LOG_FILE = "output.log";
	public static final String ERR_FILE = "errors.log";
	
    /**
     * The main entry point for the Unstructured Light Field (ULF) renderer application.
     * @param args The usual command line arguments
     */
    public static void main(String[] args)
    {
    	// Surround with try-catch for BugSpat integration
    	try {
            // Init the bugsplat library with the database, application and version parameters
            BugSplat.Init("berriers_uwstout_edu", "ULFRenderer", VERSION);
            
            // Prepare log files, delete any old ones
            File logFile = new File(LOG_FILE);
            File errFile = new File(ERR_FILE);
            if(logFile.exists()) { logFile.delete(); }
            if(errFile.exists()) { errFile.delete(); }

            // Redirect system output to log files
            System.setOut(new PrintStream(logFile));
            System.setErr(new PrintStream(errFile));
            
            // Add the logs to BugSplat crash report
            BugSplat.AddAdditionalFile(logFile.getPath());
            BugSplat.AddAdditionalFile(errFile.getPath());
            
            // Initialize the system environment vars and LWJGL
	    	System.getenv();
	    	LWJGLUtil.initialize();
	    	
	    	System.out.println("System: " + System.getProperty("os.name"));
	    	
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
	
			// Prepare the Qt GUI system
	        if(System.getProperty("os.name").startsWith("Windows")) {
	        	QApplication.setDesktopSettingsAware(false);
	        }
	        
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
	        app.addPollable(gui);
	        
	        // Move windows to nice initial locations
	        gui.move(0, 0);
	        int titleBarHeight = 0;
	        int frameWidth = 0;
	        if(System.getProperty("os.name").startsWith("Windows")) {
	            titleBarHeight = QApplication.style().pixelMetric(QStyle.PixelMetric.PM_TitleBarHeight);
	            frameWidth = 12;        	
	        }
	        
	        window.setWindowPosition(gui.size().width() + 2*frameWidth, titleBarHeight);
	                
	        // Make everything visible (important things happen here for Qt and GLFW    
	        window.show();
	        gui.showGUI();
	        
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
	
	        // Start the main event loop
			app.run();
    	} catch(Throwable t) {
            // Set crash description (useful for filtering)
            BugSplat.SetDescription("Crash Report, " + System.getProperty("os.name"));
            
    		// Let BugSplat handle the exception and exit unsuccessfully
    		BugSplat.HandleException(new Exception(t));

    		System.exit(1);
    	} finally {
    		// Always cleanup the windows
	        GLFWWindow.closeAllWindows();
    	}
    	
    	// Exit successfully
        System.exit(0);
    }
    
    /**
     * A simple method to check and print out all image formats supported by the ImageIO API
     * including plugins from JAI.
     */
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
