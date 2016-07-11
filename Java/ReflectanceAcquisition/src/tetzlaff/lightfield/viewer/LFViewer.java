/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package tetzlaff.lightfield.viewer;

import com.bugsplatsoftware.client.BugSplat;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
//import java.nio.LongBuffer; // TODO: Update OpenCL code (not needed while this is disabled)
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
// import java.util.List; // TODO: Update OpenCL code (not needed while this is disabled)
import java.util.Set;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

// import org.lwjgl.BufferUtils; // TODO: Update OpenCL code (not needed while this is disabled)
// import org.lwjgl.LWJGLUtil;	// TODO: No longer present in LWJGL 3.0.0b build 64 (CONFIRM)
// import org.lwjgl.opencl.*;	// TODO: Disabled for now (need to update for LWJGL 3.0.0b build 64)



import com.trolltech.qt.core.QCoreApplication;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.ApplicationAttribute;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QMessageBox;
import com.trolltech.qt.gui.QStyle;





import tetzlaff.gl.Program;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.helpers.InteractiveGraphics;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.helpers.GLExceptionTranslator;
import tetzlaff.helpers.MessageBox;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.lightfield.LFRendererList;
import tetzlaff.lightfield.LightField;
import tetzlaff.window.WindowPosition;
import tetzlaff.window.WindowSize;
import tetzlaff.window.glfw.GLFWWindow;

/**
 * LFViewer is a container for the main entry point of the Light Field Viewer program.
 * 
 * @author Michael Tetzlaff
 */
public class LFViewer
{
	/**
	 * Flag that indicates compiling in development mode (set to false to build deploy package)
	 */
	public static final boolean DEV_MODE = true;
	
	/**
	 * Flag that indicates if standard out and standard error should be redirected to a file.
	 * (This should be set to FALSE when building the deployment package, good to follow DEV_MODE).
	 */
	public static final boolean DISABLE_STDOUT_REDIRECT = DEV_MODE;
	
	/**
	 * Current version number (increment on milestone releases)
	 */
	public static final String VERSION = "1.0a2";
	
	/**
	 * Name of the file used to log standard out (System.out)
	 */
	public static final String LOG_FILE = "output";

	/**
	 * Name of the file used to log standard error (System.err)
	 */
	public static final String ERR_FILE = "errors";
	
	/**
	 * Quick shortcut to tell if it is running on windows.
	 */
	public static boolean OS_IS_WINDOWS;
	
	/**
	 * The User Preferences for the LFViewer application
	 */
	private static final Preferences PREFS = Preferences.userNodeForPackage(LFViewer.class);
	
	/**
	 * The GLFW window object that is the main rendering window
	 */
	private static GLFWWindow window = null;
	
	/**
	 * Reference to the main interactive application
	 */
	private static InteractiveApplication app;
	
	/**
	 * Our best guess at the number of megabytes available as VRam (queried from OpenCL)
	 */
	private static long videoMemBestGuessMB = -1;
	
    /**
     * The main entry point for the Light Field (LF) Viewer application.
     * @param args The usual command line arguments
     */
    public static void main(String[] args)
    {
        File logFile = new File("");
        File errFile = new File("");
        
    	// Surround with try-catch for BugSpat integration
    	try {
            // Init the bugsplat library with the database, application and version parameters
            BugSplat.Init("berriers_uwstout_edu", "LFViewer", VERSION);
            
            // Prepare log files, delete any old ones
            if(!DEV_MODE)
            {
	            logFile = File.createTempFile(LOG_FILE, ".log");
	            errFile = File.createTempFile(ERR_FILE, ".log");
            }
            else
            {
            	logFile = new File(LOG_FILE + ".txt"); logFile = logFile.getAbsoluteFile();
            	errFile = new File(ERR_FILE + ".txt"); errFile = errFile.getAbsoluteFile();
            }
            if(logFile.exists()) { logFile.delete(); }
            if(errFile.exists()) { errFile.delete(); }

            // Redirect system output to log files
            if(!DISABLE_STDOUT_REDIRECT)
            {
            	System.setOut(new PrintStream(logFile));
            	System.setErr(new PrintStream(errFile));
            }
            
            // Add header with timestamp to logs
            String timestamp = new SimpleDateFormat("MM/dd/yyy HH:mm:ss").format(new Date());
            System.out.println("LF Viewer " + VERSION + ", Standard Log (" + timestamp + ")");
            System.err.println("LF Viewer " + VERSION + ", Error Log (" + timestamp + ")");
            
            // Initialize the system environment vars and LWJGL
	    	System.getenv();
//	    	LWJGLUtil.initialize();	// TODO: seems to be gone in latest LWJGL 3.0.0b (build 64) CONFIRM!!

	    	// Use OpenCL to make an informed guess at GPU memory
	    //    checkGPUMemoryCapabilities();

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
	        System.out.println("**** Video Memory ***");
	        System.out.println("* DISABLED"); //Best Guess : " + (videoMemBestGuessMB<0?"??":videoMemBestGuessMB) + "MB");
	        System.out.println("*********************\n");

	        // Set the windows OS flag
	        OS_IS_WINDOWS = System.getProperty("os.name").contains("win");
	        OS_IS_WINDOWS = OS_IS_WINDOWS || System.getProperty("os.name").contains("Win");
	        
	    	// Check for and print supported image formats (some are not as easy as you would think)
	    	checkSupportedImageFormats();

	    	// Create a GLFW window for integration with LWJGL (part of the 'view' in this MVC arrangement)
	    	window = new GLFWWindow(800, 800, "Light Field Viewer", true, 4);
	    	window.enableDepthTest();
	
	    	// Add a trackball controller to the window for rotating the object (also responds to mouse scrolling)
	    	// This is the 'controller' in the MVC arrangement.
	    	Trackball trackball = new Trackball(1.0f);
	        trackball.addAsWindowListener(window);
	
	        // Create a new 'renderer' to be attached to the window and the GUI.
	        // This is the object that loads the light field models and handles drawing them.  This object abstracts
	        // the underlying data and provides ways of triggering events via the trackball and the user interface later.
	        LFRendererList<OpenGLContext> model = new LFRendererList<OpenGLContext>(window, trackball);
	        
	        window.addCharacterListener((win, c) -> {
	        	if (c == 'p')
	        	{
	        		System.out.println("reloading program...");
	        		
		        	try
		        	{
		        		Program<OpenGLContext> newProgram = window.getShaderProgramBuilder()
		    					.addShader(ShaderType.VERTEX, new File(LightField.SHADER_RESOURCE_DIRECTORY, "ulr.vert"))
		    					.addShader(ShaderType.FRAGMENT, new File(LightField.SHADER_RESOURCE_DIRECTORY, "ulr.frag"))
		    					.createProgram();
			        	
			        	if (model.getProgram() != null)
		        		{
		        			model.getProgram().delete();
		        		}
			        	model.setProgram(newProgram);
					} 
		        	catch (Exception e) 
		        	{
						e.printStackTrace();
					}
	        	}
	        	else if (c == 'r')
	        	{
	        		if(model.getDrawable() != null)
	        		{
	        			model.getDrawable().resetFPSRange();
	        		}
	        	}
	        });
	
	    	// Create a new application to run our event loop and give it the GLFWWindow for polling
	    	// of events and the OpenGL context.  The LFRendererList provides the drawable.
	        app = InteractiveGraphics.createApplication(window, window, model.getDrawable());
	        
	        app.setExceptionTranslator(new GLExceptionTranslator());
	        
	        // Create an anonymous wrapper for QMessageBox
	        InteractiveApplication.setMessageBox(new MessageBox() {
				@Override
				public void info(String title, String message) {
					QMessageBox.information(null, title, message);
				}

				@Override
				public Response question(String title, String message) {
					QMessageBox.StandardButton button = QMessageBox.question(null, title, message);
					switch(button)
					{
						case Yes:
						case Ok: return MessageBox.Response.YES;
						case No: return MessageBox.Response.NO;
						
						default:
						case Cancel: return MessageBox.Response.CANCEL;
					}
				}

				@Override
				public void warning(String title, String message) {
					QMessageBox.warning(null, title, message);
				}

				@Override
				public void error(String title, String message) {
					QMessageBox.critical(null, title, message);
				}
	        });
	
			// Prepare the Qt GUI system
	        if(System.getProperty("os.name").startsWith("Windows")) {
	        	QApplication.setDesktopSettingsAware(false);
	        }
	        
	        // Set the QCoreApplication default properties
	        QApplication.initialize(args);
	        QCoreApplication.setOrganizationName("Cultural Heritage Imaging");
	        QCoreApplication.setOrganizationDomain("culturalheritageimaging.org");
	        QCoreApplication.setApplicationName("LF Viewer");
	        
	        // As far as I can tell, the OS X native menu bar doesn't work in Qt Jambi
	        // The Java process owns the native menu bar and won't relinquish it to Qt
	        QApplication.setAttribute(ApplicationAttribute.AA_DontUseNativeMenuBar);
	        
	        // Create a user interface that examines the LFRendererList for renderer settings and
	        // selecting between different loaded models.
	        LFConfigQWidget gui = new LFConfigQWidget(model, window.isHighDPI(), null);
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
	
	        // Set the GL window (so other funcs can access) and start the main event loop
			app.run();
    	} catch(Exception e) {
    		e.printStackTrace();
	        QApplication.initialize(args);
        	QMessageBox.critical(null, "Critical Error",
        			"An error occured while initializing or running the main program.  Please see errors.txt for more details.");

        	// Set crash description (useful for filtering)
            BugSplat.SetDescription("Crash Report, Exception, " + System.getProperty("os.name"));
            
            // Take care of the log files
            if(!DISABLE_STDOUT_REDIRECT)
            {
	            if(errFile.exists()) { BugSplat.AddAdditionalFile(errFile.getAbsolutePath()); }
	            if(logFile.exists()) { BugSplat.AddAdditionalFile(logFile.getAbsolutePath()); }
            }
            
    		// Let BugSplat handle the exception and set error exit code
    		BugSplat.HandleException(e);
    	} catch(Throwable t) {
    		t.printStackTrace();
	        QApplication.initialize(args);
        	QMessageBox.critical(null, "Critical Error",
        			"An error occured while initializing or running the main program.  Please see errors.txt for more details.");

        	// Set crash description (useful for filtering)
            BugSplat.SetDescription("Crash Report, Error, " + System.getProperty("os.name"));
            
            // Take care of the log files
            if(!DISABLE_STDOUT_REDIRECT)
            {
	            if(errFile.exists()) { BugSplat.AddAdditionalFile(errFile.getAbsolutePath()); }
	            if(logFile.exists()) { BugSplat.AddAdditionalFile(logFile.getAbsolutePath()); }
            }
            
    		// Let BugSplat handle the exception and set error exit code
    		BugSplat.HandleException(new Exception(t));
    	} finally {
    		// Always cleanup the windows
	        GLFWWindow.closeAllWindows();
	        QCoreApplication.quit();
    	}    	
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
    
	// TODO: Update for latest version of LWJGL 3, currently unused
//  private static void checkGPUMemoryCapabilities()
//  {
//  	try {
//
//	    	//CL.create();
//  		long maxMem = -1;
//	    	CLPlatform platform = CLPlatform.getPlatforms().get(0);
//	    	if(platform != null)
//	    	{
//	    		List<CLDevice> devices = platform.getDevices(CL10.CL_DEVICE_TYPE_GPU);
//				LongBuffer memSize = BufferUtils.createLongBuffer(1);
//				for(CLDevice device : devices)
//				{
//					CL10.clGetDeviceInfo(device., CL10.CL_DEVICE_GLOBAL_MEM_SIZE, memSize, null);
//					if(memSize.get(0) > maxMem) { maxMem = memSize.get(0); }
//				}
//	    	}
//	    	
//	    	if(maxMem > 0)
//	    	{
//	    		videoMemBestGuessMB = maxMem/1048576;
//	    	}
//	    	//CL.destroy();
//  	} catch(Exception e) {
//  		e.printStackTrace();
//  	}
//  }
    
    public static WindowSize getRenderingWindowSize()
    {
    	return window.getWindowSize();
    }
    
    public static WindowPosition getRendringWindowPosition()
    {
    	return window.getWindowPosition();
    }
    
    /**
     * A useful method to manually generate a bug report and submit it to the
     * BugSplat server.  The information contained in this report is similar to
     * a crash report but also includes a screenshot.
     * @throws IOException
     */
    public static void generateBugReport() throws IOException
    {
    	// Set a waiting cursor to discourage interaction
    	QApplication.setOverrideCursor(new QCursor(Qt.CursorShape.WaitCursor));
    	
        // Prepare for a screenshot
    	File screenshot = File.createTempFile("screenshot", ".png");
    	if(screenshot.exists()) { screenshot.delete(); }

    	// Request and wait for the screenshot
    	app.requestDebugDump("png", screenshot);
    	int tries = 0, maxTries = 100;
        while(!screenshot.exists() && tries < maxTries)
        {
        	try { Thread.sleep(100); }
        	catch (InterruptedException e) {}
        	tries++;
        }
        
        // Attach it if it is there
        if(screenshot.exists()) {
        	BugSplat.AddAdditionalFile(screenshot.getAbsolutePath());
        }

        // Go back to normal cursor
    	QApplication.restoreOverrideCursor();

    	// Create the report
        BugSplat.SetDescription("Manual Report, " + System.getProperty("os.name"));
        BugSplat.HandleException(new Exception("Manual Bug Report"));
    }
    
    // Query the installed amount of VRAM (note: this may be -1 if it could not be determined)
    public static long getVRAMMegabytes()
    {
    	return videoMemBestGuessMB;
    }
    
    // User preferences access functions    
    public static File getLastCamDefFileDirectory()
    {
    	return getSanitizedPrefsDir("lastCamDefFileDirectory");
    }
    
    public static void setLastCamDefFileDirectory(File lastCamDefFileDir)
    {
    	setSanitizedPrefsDir("lastCamDefFileDirectory", lastCamDefFileDir);
    }

    public static File getLastSequenceFileDirectory()
    {
    	return getSanitizedPrefsDir("lastSequenceFileDirectory");
    }
    
    public static void setLastSequenceFileDirectory(File lastSequenceFileDir)
    {
    	setSanitizedPrefsDir("lastSequenceFileDirectory", lastSequenceFileDir);
    }
    
    // Helper function to catch fringe cases and make sure the directories are valid
    private static File getSanitizedPrefsDir(String key)
    {
    	File prefsDir = new File(PREFS.get(key, System.getProperty("user.home")));
    	if(!prefsDir.exists()) { prefsDir = new File(System.getProperty("user.home")); }
    	return prefsDir;
    }
    
    private static void setSanitizedPrefsDir(String key, File dir)
    {
    	if(!dir.isDirectory()) { dir = dir.getParentFile(); }
    	PREFS.put(key, dir.getAbsolutePath());
    }
}
