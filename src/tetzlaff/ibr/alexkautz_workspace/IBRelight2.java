package tetzlaff.ibr.alexkautz_workspace;

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
import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.ibr.alexkautz_workspace.mount_olympus.PassedParameters;
import tetzlaff.ibr.alexkautz_workspace.mount_olympus.RenderPerams;
import tetzlaff.ibr.alexkautz_workspace.render.TrackballLightController2;
import tetzlaff.ibr.alexkautz_workspace.render.new_tool_setup_rename_this_later.*;
import tetzlaff.ibr.rendering.ImageBasedRendererList;
import tetzlaff.ibr.util.IBRRequestQueue;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.interactive.Refreshable;
import tetzlaff.mvc.controllers.impl.FirstPersonController;
import tetzlaff.mvc.controllers.impl.TrackballController;
import tetzlaff.mvc.models.CameraModel;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.impl.BasicCameraModel;

/**
 * ULFProgram is a container for the main entry point of the Unstructured Light Field
 * rendering program.
 *
 * @author Michael Tetzlaff
 */
public class IBRelight2
{
	private static final boolean DEBUG = true;

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


			TrackballLightController2 lightController = new TrackballLightController2();
			lightController.addAsWindowListener(window);

			CameraModel fpCameraModel = new BasicCameraModel();

			FirstPersonController fpController = new FirstPersonController(fpCameraModel);
			fpController.addAsWindowListener(window);

			window.addMouseButtonPressListener((win, buttonIndex, mods) ->
			{
				fpController.setEnabled(false);
			});

			TrackballController trackballController = TrackballController.getBuilder().create();

			trackballController.addAsWindowListener(window);

			// Hybrid FP + Trackball controls
			ReadonlyCameraModel cameraModel = () -> fpCameraModel.getLookMatrix()
					.times((
							trackballController.getCameraModel().getLookMatrix()
					)) //TOASK does the fpCameraModel just return the Identity matrix?
					;


			//Here i create my own brand of camera and light models;
			LightModelX lightModelX = new LightModelX();
			CameraModelX cameraModelX = new CameraModelX();

			GlobalController globalController = new GlobalController();

            LookToolController.getBuilder() //I do not need to keep my instance of LookToolController in a value

                    .setGlobalControler(globalController)

                    .setCameraModelX(cameraModelX)
                    .setLightModelX(lightModelX)

                    .setPrimaryButtonIndex(0)
                    .setSecondaryButtonIndex(1)
                    .setTertiaryButtonIndex(3)
                    .setSensitivityScrollWheel(15.0f)
                    .setSensitivityOrbit(1.5f)

                    .setWindow(window)

                    .create();
			// Create a new 'renderer' to be attached to the window and the GUI.
			// This is the object that loads the ULF models and handles drawing them.  This object abstracts
			// the underlying data and provides ways of triggering events via the trackball and the user
			// interface later when it is passed to the ULFUserInterface object.
			//ImageBasedRendererList<OpenGLContext> model = new ImageBasedRendererList<OpenGLContext>(context, program, cameraModel, lightController.getLightModel());

            ImageBasedRendererList<OpenGLContext> model = new ImageBasedRendererList<OpenGLContext>(
                    context,
                    program,
                    cameraModelX,
                    lightController.getLightModel());


            window.addCharacterListener((win, c) -> {
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


			//IBRelightConfigFrame gui = new IBRelightConfigFrame(model, lightController.getCameraModel(), (request) -> requestQueue.addRequest(request), window.isHighDPI());
			PassedParameters.get().setRenderPerams(new RenderPerams(model,
					globalController
					));


			//gui.showGUI();
			//app.addPollable(gui); // Needed for Qt UI

			//requestQueue.setLoadingMonitor(gui.getLoadingMonitor());

			// Make everything visible and start the event loop

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

						System.out.println(model.getSelectedItem().getSceneViewportModel().getObjectAtCoordinates(x, y));
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			});

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
