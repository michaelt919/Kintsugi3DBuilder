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
import tetzlaff.gl.glfw.GLFWWindow;
import tetzlaff.gl.glfw.GLFWWindowFactory;
import tetzlaff.gl.interactive.InteractiveGraphics;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.ibr.rendering.CameraBasedLightModel;
import tetzlaff.ibr.rendering.HardcodedLightModel;
import tetzlaff.ibr.rendering.ImageBasedRendererList;
import tetzlaff.ibr.util.IBRRequestQueue;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.interactive.Refreshable;
import tetzlaff.mvc.controllers.impl.FirstPersonController;
import tetzlaff.mvc.controllers.impl.TrackballController;
import tetzlaff.mvc.controllers.impl.TrackballLightController;
import tetzlaff.mvc.models.CameraModel;
import tetzlaff.mvc.models.LightModel;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.impl.BasicCameraModel;
import tetzlaff.mvc.models.impl.TrackballModel;

/**
 * ULFProgram is a container for the main entry point of the Unstructured Light Field
 * rendering program.
 * 
 * @author Michael Tetzlaff
 */
public class IBRelight
{
	private static final boolean DEBUG = true;
	
	private static class MetaLightModel implements CameraBasedLightModel
	{
		boolean hardcodedMode = false;
		LightModel normalLightModel;
		HardcodedLightModel hardcodedLightModel;

		@Override
		public int getLightCount() 
		{
			return hardcodedMode ? hardcodedLightModel.getLightCount() : normalLightModel.getLightCount();
		}

		@Override
		public Vector3 getLightColor(int i) 
		{
			return hardcodedMode ? hardcodedLightModel.getLightColor(i) : normalLightModel.getLightColor(i);
		}
		
		@Override
		public void setLightColor(int i, Vector3 lightColor)
		{
			normalLightModel.setLightColor(i, lightColor);
		}
		
		@Override
		public Matrix4 getLightMatrix(int i) 
		{
			return hardcodedMode ? hardcodedLightModel.getLightMatrix(i) : normalLightModel.getLightMatrix(i);
		}

		@Override
		public void setLightMatrix(int i, Matrix4 lightMatrix) 
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void overrideCameraPose(Matrix4 cameraPoseOverride) 
		{
			if (hardcodedMode)
			{
				hardcodedLightModel.overrideCameraPose(cameraPoseOverride);
			}
		}

		@Override
		public void removeCameraPoseOverride() 
		{
			if (hardcodedMode)
			{
				hardcodedLightModel.removeCameraPoseOverride();
			}
		}

		@Override
		public Vector3 getAmbientLightColor() 
		{
			return hardcodedMode ? hardcodedLightModel.getAmbientLightColor() : normalLightModel.getAmbientLightColor();
		}

		@Override
		public void setAmbientLightColor(Vector3 ambientLightColor) 
		{
			normalLightModel.setAmbientLightColor(ambientLightColor);
		}

		@Override
		public boolean getEnvironmentMappingEnabled() 
		{
			return hardcodedMode ? hardcodedLightModel.getEnvironmentMappingEnabled() : normalLightModel.getEnvironmentMappingEnabled();
		}

		@Override
		public void setEnvironmentMappingEnabled(boolean enabled) 
		{
			normalLightModel.setEnvironmentMappingEnabled(enabled);
		}

		@Override
		public boolean isLightVisualizationEnabled(int index) 
		{
			return hardcodedMode ? hardcodedLightModel.isLightVisualizationEnabled(index) : normalLightModel.isLightVisualizationEnabled(index);
		}

		@Override
		public boolean isLightWidgetEnabled(int index) 
		{
			return hardcodedMode ? hardcodedLightModel.isLightWidgetEnabled(index) : normalLightModel.isLightWidgetEnabled(index);
		}

		@Override
		public Vector3 getLightCenter(int i) 
		{
			return hardcodedMode ? hardcodedLightModel.getLightCenter(i) : normalLightModel.getLightCenter(i);
		}

		@Override
		public void setLightCenter(int i, Vector3 lightTargetPoint) 
		{
			normalLightModel.setLightCenter(i, lightTargetPoint);
		}

		@Override
		public Matrix4 getEnvironmentMapMatrix() 
		{
			return hardcodedMode ? hardcodedLightModel.getEnvironmentMapMatrix() : normalLightModel.getEnvironmentMapMatrix();
		}
	}
	
	public static void runProgram()
	{
		System.getenv();
    	System.setProperty("org.lwjgl.util.DEBUG", "true");
    	
    	// Check for and print supported image formats (some are not as easy as you would think)
    	checkSupportedImageFormats();

    	// Create a GLFW window for integration with LWJGL (part of the 'view' in this MVC arrangement)
    	try(GLFWWindow<OpenGLContext> window = 
    			GLFWWindowFactory.buildOpenGLWindow("IBRelight", 800, 800)
		    		.setResizable(true)
		    		.setMultisamples(4)
		    		.create())
		{
	    	OpenGLContext context = window.getContext();
	    	
	    	context.getState().enableDepthTest();
	    	
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
	    		program = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/relight/relight.frag"))
	    				.createProgram();
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        	throw new IllegalStateException("The shader program could not be initialized.", e);
	        }
	        
	        MetaLightModel metaLightModel = new MetaLightModel();
	        
	        TrackballLightController lightController = new TrackballLightController();
	        lightController.addAsWindowListener(window);
	    	metaLightModel.normalLightModel = lightController.getLightModel();
	        
	    	CameraModel fpCameraModel = new BasicCameraModel();
	    	
	        FirstPersonController fpController = new FirstPersonController(fpCameraModel);
	        fpController.addAsWindowListener(window);
	        
	        window.addMouseButtonPressListener((win, buttonIndex, mods) -> 
	        {
	        	fpController.setEnabled(false);
	    	});
	        
	        TrackballModel hardcodedLightTrackballModel = new TrackballModel();
	    	TrackballController hardcodedLightTrackballController = TrackballController.getBuilder()
	    			.setSensitivity(1.0f)
	    			.setPrimaryButtonIndex(0)
	    			.setSecondaryButtonIndex(1)
	    			.setModel(hardcodedLightTrackballModel)
	    			.create();
	    	hardcodedLightTrackballController.addAsWindowListener(window);
	        
	        // Hybrid FP + Trackball controls
	        ReadonlyCameraModel cameraModel = () -> fpCameraModel.getLookMatrix().times(
	        		(metaLightModel.hardcodedMode ? hardcodedLightTrackballModel : lightController.getCurrentCameraModel()).getLookMatrix());
	
	        // Create a new 'renderer' to be attached to the window and the GUI.
	        // This is the object that loads the ULF models and handles drawing them.  This object abstracts
	        // the underlying data and provides ways of triggering events via the trackball and the user
	        // interface later when it is passed to the ULFUserInterface object.
	        ImageBasedRendererList<OpenGLContext> model = new ImageBasedRendererList<OpenGLContext>(context, program);
	        model.setObjectModel(() -> Matrix4.IDENTITY);
	        model.setCameraModel(cameraModel);
	        model.setLightModel(metaLightModel);
	    	
	        HardcodedLightModel hardcodedLightController = 
	    			new HardcodedLightModel(
						() -> model.getSelectedItem().getActiveViewSet(),
						() -> model.getSelectedItem().getActiveGeometry(),
						hardcodedLightTrackballModel);
	    	metaLightModel.hardcodedLightModel = hardcodedLightController;
	        
	        window.addCharacterListener((win, c) -> 
	        {
	        	if (c == 'p')
	        	{
	        		System.out.println("reloading program...");
	        		
		        	try
		        	{
		        		// reload program
		        		Program<OpenGLContext> newProgram = context.getShaderProgramBuilder()
			    				.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
		        				.addShader(ShaderType.FRAGMENT, new File("shaders/relight/relight.frag"))
								.createProgram();
			        	
			        	if (model.getProgram() != null)
		        		{
		        			model.getProgram().close();
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
	        		//metaLightModel.hardcodedMode = !metaLightModel.hardcodedMode;
	        	}
	        	else if (c == ' ')
	        	{
	        		fpController.setEnabled(!fpController.getEnabled());
	        	}
	        });
	        
	        window.addMouseButtonPressListener((win, buttonIndex, mods) ->
	        {
	        	try
	        	{
		        	if (win == window && model.getSelectedItem() != null)
		        	{
		        		CursorPosition pos = window.getCursorPosition();
		        		WindowSize size = window.getWindowSize();
		        		double x = pos.x / size.width;
		        		double y = pos.y / size.height;

		        		System.out.println(model.getSelectedItem().getSceneViewportModel().getObjectAtCoordinates(x, y) + "\t" + 
		        				model.getSelectedItem().getSceneViewportModel().get3DPositionAtCoordinates(x, y));
		        	}
	        	}
	        	catch (Exception e)
	        	{
	        		e.printStackTrace();
	        	}
	        });
	
	    	// Create a new application to run our event loop and give it the GLFWWindow for polling
	    	// of events and the OpenGL context.  The ULFRendererList provides the renderable.
	        InteractiveApplication app = InteractiveGraphics.createApplication(window, context, model.getRenderable());
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
	        
	        IBRRequestQueue<OpenGLContext> requestQueue = new IBRRequestQueue<OpenGLContext>(context, model);
	        
	        app.addRefreshable(new Refreshable()
	        {
				@Override
				public void initialize() 
				{
				}

				@Override
				public void refresh() 
				{
					requestQueue.executeQueue();
				}

				@Override
				public void terminate() 
				{
				}
	        });
	        
	        // Create a user interface that examines the ULFRendererList for renderer settings and
	        // selecting between different loaded models.
	        IBRelightConfigFrame gui = new IBRelightConfigFrame(model, lightController.getLightModel(), (request) -> requestQueue.addRequest(request), window.isHighDPI());
	        gui.showGUI();        
	        //app.addPollable(gui); // Needed for Qt UI
	        
	        requestQueue.setLoadingMonitor(gui.getLoadingMonitor());
	        
	    	// Make everything visible and start the event loop
	    	window.show();
			app.run();
		}

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
