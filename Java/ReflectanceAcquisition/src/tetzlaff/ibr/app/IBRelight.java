package tetzlaff.ibr.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import tetzlaff.gl.Program;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.FirstPersonController;
import tetzlaff.gl.helpers.InteractiveGraphics;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.OverrideableLightController;
import tetzlaff.gl.helpers.TrackballLightController;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.ibr.rendering.HardcodedLightController;
import tetzlaff.ibr.rendering.ImageBasedRendererList;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.interactive.Refreshable;
import tetzlaff.window.glfw.GLFWWindow;
//import org.lwjgl.opengl.GLDebugMessageCallback;
//import static org.lwjgl.opengl.KHRDebug.*;

/**
 * ULFProgram is a container for the main entry point of the Unstructured Light Field
 * rendering program.
 * 
 * @author Michael Tetzlaff
 */
public class IBRelight
{
	private static final boolean DEBUG = true;
	
	private static class MetaLightController implements OverrideableLightController
	{
		public boolean hardcodedMode = false;
		public TrackballLightController normalController;
		public HardcodedLightController hardcodedController;

		@Override
		public int getLightCount() 
		{
			return hardcodedMode ? hardcodedController.getLightCount() : normalController.getLightCount();
		}

		@Override
		public Vector3 getLightColor(int i) 
		{
			return hardcodedMode ? hardcodedController.getLightColor(i) : normalController.getLightColor(i);
		}
		
		@Override
		public void setLightColor(int i, Vector3 lightColor)
		{
			normalController.setLightColor(i, lightColor);
		}
		
		@Override
		public Matrix4 getLightMatrix(int i) 
		{
			return hardcodedMode ? hardcodedController.getLightMatrix(i) : normalController.getLightMatrix(i);
		}
		
		public CameraController asCameraController()
		{
			return hardcodedMode ? hardcodedController.asCameraController() : normalController.asCameraController();
		}

		@Override
		public void overrideCameraPose(Matrix4 cameraPoseOverride) 
		{
			if (hardcodedMode)
			{
				hardcodedController.overrideCameraPose(cameraPoseOverride);
			}
			else
			{
				normalController.overrideCameraPose(cameraPoseOverride);
			}
		}

		@Override
		public void removeCameraPoseOverride() 
		{
			if (hardcodedMode)
			{
				hardcodedController.removeCameraPoseOverride();
			}
			else
			{
				normalController.removeCameraPoseOverride();
			}
		}

		@Override
		public Vector3 getAmbientLightColor() 
		{
			return hardcodedMode ? hardcodedController.getAmbientLightColor() : normalController.getAmbientLightColor();
		}

		@Override
		public void setAmbientLightColor(Vector3 ambientLightColor) 
		{
			normalController.setAmbientLightColor(ambientLightColor);
		}

		@Override
		public boolean getEnvironmentMappingEnabled() 
		{
			return hardcodedMode ? hardcodedController.getEnvironmentMappingEnabled() : normalController.getEnvironmentMappingEnabled();
		}

		@Override
		public void setEnvironmentMappingEnabled(boolean enabled) 
		{
			normalController.setEnvironmentMappingEnabled(enabled);
		}

		@Override
		public int getSelectedLightIndex() 
		{
			return hardcodedMode ? hardcodedController.getSelectedLightIndex() : normalController.getSelectedLightIndex();
		}
	}
	
	public static void runProgram()
	{
		System.getenv();
    	System.setProperty("org.lwjgl.util.DEBUG", "true");
    	
    	// Check for and print supported image formats (some are not as easy as you would think)
    	checkSupportedImageFormats();

    	// Create a GLFW window for integration with LWJGL (part of the 'view' in this MVC arrangement)
    	GLFWWindow window = new GLFWWindow(800, 800, "IBRelight", true, 4);
    	window.enableDepthTest();
    	
//    	org.lwjgl.opengl.GL11.glEnable(GL_DEBUG_OUTPUT);
//    	GLDebugMessageCallback debugCallback = new GLDebugMessageCallback() 
//    	{
//			@Override
//			public void invoke(int source, int type, int id, int severity, int length, long message, long userParam) 
//			{
//		    	if (severity == GL_DEBUG_SEVERITY_HIGH)
//		    	{
//		    		System.err.println(org.lwjgl.system.MemoryUtil.memDecodeASCII(message));
//		    	}
//			}
//    		
//    	};
//    	glDebugMessageCallback(debugCallback, 0L);

    	Program<OpenGLContext> program;
        try
        {
    		program = window.getShaderProgramBuilder()
    				.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
    				.addShader(ShaderType.FRAGMENT, new File("shaders/ibr/ibr.frag"))
    				.createProgram();
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        	throw new IllegalStateException("The shader program could not be initialized.", e);
        }
        
        MetaLightController metaLightController = new MetaLightController();
        
        TrackballLightController lightController = new TrackballLightController();
        lightController.addAsWindowListener(window);
    	metaLightController.normalController = lightController;
        
        FirstPersonController fpController = new FirstPersonController();
        fpController.addAsWindowListener(window);
        
        window.addMouseButtonPressListener((win, buttonIndex, mods) -> 
        {
        	fpController.setEnabled(false);
    	});
        
        // Hybrid FP + Trackball controls
        CameraController cameraController = () -> fpController.getViewMatrix().times(metaLightController.asCameraController().getViewMatrix());

        // Create a new 'renderer' to be attached to the window and the GUI.
        // This is the object that loads the ULF models and handles drawing them.  This object abstracts
        // the underlying data and provides ways of triggering events via the trackball and the user
        // interface later when it is passed to the ULFUserInterface object.
        ImageBasedRendererList<OpenGLContext> model = new ImageBasedRendererList<OpenGLContext>(window, program, cameraController, metaLightController);
    	
    	HardcodedLightController hardcodedLightController = 
    			new HardcodedLightController(
					() -> model.getSelectedItem().getActiveViewSet(),
					() -> model.getSelectedItem().getActiveProxy());
    	hardcodedLightController.addAsWindowListener(window);
    	metaLightController.hardcodedController = hardcodedLightController;
        
        window.addCharacterListener((win, c) -> {
        	if (c == 'p')
        	{
        		System.out.println("reloading program...");
        		
	        	try
	        	{
	        		// reload program
	        		Program<OpenGLContext> newProgram = window.getShaderProgramBuilder()
		    				.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
	        				.addShader(ShaderType.FRAGMENT, new File("shaders/ibr/ibr.frag"))
							.createProgram();
		        	
		        	if (model.getProgram() != null)
	        		{
	        			model.getProgram().delete();
	        		}
		        	model.setProgram(newProgram);
		        	
		        	model.getSelectedItem().reloadHelperShaders();
				} 
	        	catch (Exception e) 
	        	{
					e.printStackTrace();
				}
        	}
        	else if (c == 'l')
        	{
        		//metaLightController.hardcodedMode = !metaLightController.hardcodedMode;
        	}
        	else if (c == ' ')
        	{
        		fpController.setEnabled(!fpController.getEnabled());
        	}
        });

    	// Create a new application to run our event loop and give it the GLFWWindow for polling
    	// of events and the OpenGL context.  The ULFRendererList provides the drawable.
        InteractiveApplication app = InteractiveGraphics.createApplication(window, window, model.getDrawable());
        app.addRefreshable(new Refreshable() 
        {
			@Override
			public void initialize() 
			{
			}

			@Override
			public void refresh() 
			{
				fpController.update();
			}

			@Override
			public void terminate() 
			{
			}
        });

//        // Fire up the Qt Interface
//		// Prepare the Qt GUI system
//        QApplication.initialize(args);
//        QCoreApplication.setOrganizationName("UW Stout");
//        QCoreApplication.setOrganizationDomain("uwstout.edu");
//        QCoreApplication.setApplicationName("PhotoScan Helper");
//        
//        // As far as I can tell, the OS X native menu bar doesn't work in Qt Jambi
//        // The Java process owns the native menu bar and won't relinquish it to Qt
//        QApplication.setAttribute(ApplicationAttribute.AA_DontUseNativeMenuBar);
        
        // Create a user interface that examines the ULFRendererList for renderer settings and
        // selecting between different loaded models.
        IBRelightConfigFrame gui = new IBRelightConfigFrame(model, lightController, window.isHighDPI());
        gui.showGUI();        
        //app.addPollable(gui); // Needed for Qt UI
        
    	// Make everything visible and start the event loop
    	window.show();
		app.run();

		// The event loop has terminated so cleanup the windows and exit with a successful return code.
        GLFWWindow.closeAllWindows();
	}
	
    /**
     * The main entry point for the Unstructured Light Field (ULF) renderer application.
     * @param args The usual command line arguments
     * @throws FileNotFoundException 
     */
    public static void main(String[] args) throws FileNotFoundException 
    {
    	if (!DEBUG)
    	{
	    	PrintStream out = new PrintStream("out.log");
	    	PrintStream err = new PrintStream("err.log");
    		System.setOut(out);
    		System.setErr(err);
    	}

		runProgram();
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
