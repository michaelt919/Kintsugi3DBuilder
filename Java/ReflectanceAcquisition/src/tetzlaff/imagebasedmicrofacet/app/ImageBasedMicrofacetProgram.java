package tetzlaff.imagebasedmicrofacet.app;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.lwjgl.LWJGLUtil;
//import org.lwjgl.opengl.GLDebugMessageCallback;
//import static org.lwjgl.opengl.KHRDebug.*;





import tetzlaff.gl.Program;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.FirstPersonController;
import tetzlaff.gl.helpers.InteractiveGraphics;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.imagebasedmicrofacet.ImageBasedMicrofacetRendererList;
import tetzlaff.imagebasedmicrofacet.TrackballLightController;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.interactive.Refreshable;
import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;
import tetzlaff.window.glfw.GLFWWindow;

/**
 * ULFProgram is a container for the main entry point of the Unstructured Light Field
 * rendering program.
 * 
 * @author Michael Tetzlaff
 */
public class ImageBasedMicrofacetProgram
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
    	GLFWWindow window = new GLFWWindow(800, 800, "Image-Based Microfacet Renderer", true, 4);
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
    				.addShader(ShaderType.VERTEX, new File("shaders/ibr/ulr.vert"))
    				.addShader(ShaderType.FRAGMENT, new File("shaders/ibr/ibmfr.frag"))
    				.createProgram();
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        	throw new IllegalStateException("The shader program could not be initialized.", e);
        }

    	Program<OpenGLContext> indexProgram;
        try
        {
        	indexProgram = window.getShaderProgramBuilder()
    				.addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
    				.addShader(ShaderType.FRAGMENT, new File("shaders/ibr/ibmfr_index.frag"))
    				.createProgram();
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        	throw new IllegalStateException("The shader program could not be initialized.", e);
        }
        
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

        // Create a new 'renderer' to be attached to the window and the GUI.
        // This is the object that loads the ULF models and handles drawing them.  This object abstracts
        // the underlying data and provides ways of triggering events via the trackball and the user
        // interface later when it is passed to the ULFUserInterface object.
        ImageBasedMicrofacetRendererList<OpenGLContext> model = new ImageBasedMicrofacetRendererList<OpenGLContext>(window, program, indexProgram, cameraController, lightController);
        
        window.addCharacterListener((win, c) -> {
        	if (c == 'p')
        	{
        		System.out.println("reloading program...");
        		
	        	try
	        	{
	        		// reload program
	        		Program<OpenGLContext> newProgram = window.getShaderProgramBuilder()
	        				.addShader(ShaderType.VERTEX, new File("shaders/ibr/ulr.vert"))
	        				.addShader(ShaderType.FRAGMENT, new File("shaders/ibr/ibmfr.frag"))
							.createProgram();
		        	
		        	Program<OpenGLContext> newIndexProgram = window.getShaderProgramBuilder()
		    				.addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
		    				.addShader(ShaderType.FRAGMENT, new File("shaders/ibr/ibmfr_index.frag"))
							.createProgram();
		        	
		        	if (model.getProgram() != null)
	        		{
	        			model.getProgram().delete();
	        		}
		        	model.setProgram(newProgram);
		        	
		        	if (model.getIndexProgram() != null)
	        		{
	        			model.getIndexProgram().delete();
	        		}
		        	model.setIndexProgram(newIndexProgram);
				} 
	        	catch (Exception e) 
	        	{
					e.printStackTrace();
				}
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
        ImageBasedMicrofacetConfigFrame gui = new ImageBasedMicrofacetConfigFrame(model, window.isHighDPI());
        gui.showGUI();        
        //app.addPollable(gui); // Needed for Qt UI
        
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