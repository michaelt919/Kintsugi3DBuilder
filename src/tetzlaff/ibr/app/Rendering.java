package tetzlaff.ibr.app;//Created by alexk on 7/19/2017.

import java.io.File;
import java.io.IOException;
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
import tetzlaff.ibr.IBRLoadingModel;
import tetzlaff.ibr.ReadonlyIBRLoadOptionsModel;
import tetzlaff.ibr.ReadonlyIBRSettingsModel;
import tetzlaff.ibr.javafx.models.JavaFXModels;
import tetzlaff.ibr.rendering.ImageBasedRendererList;
import tetzlaff.ibr.tools.ToolBox;
import tetzlaff.ibr.tools.ToolSelectionModel;
import tetzlaff.ibr.util.IBRRequestQueue;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.interactive.Refreshable;
import tetzlaff.mvc.controllers.impl.FirstPersonController;
import tetzlaff.mvc.controllers.impl.TrackballController;
import tetzlaff.mvc.models.CameraModel;
import tetzlaff.mvc.models.ExtendedCameraModel;
import tetzlaff.mvc.models.SceneViewportModel;
import tetzlaff.mvc.models.impl.BasicCameraModel;
import tetzlaff.mvc.models.impl.EnvironmentMapModelBase;
import tetzlaff.mvc.models.impl.LightingModelBase;

public class Rendering
{
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



			CameraModel fpCameraModel = new BasicCameraModel();

			FirstPersonController fpController = new FirstPersonController(fpCameraModel);
			fpController.addAsWindowListener(window);

			window.addMouseButtonPressListener((win, buttonIndex, mods) ->
			{
				fpController.setEnabled(false);
			});

			TrackballController trackballController = TrackballController.getBuilder().create();

			trackballController.addAsWindowListener(window);

			//Here I create my own brand of camera and light models;
			// TODO NEWUI refactor against interfaces not implementations
			LightingModelBase lightingModel = JavaFXModels.getInstance().getLightingModel();
			EnvironmentMapModelBase environmentMapModel = JavaFXModels.getInstance().getEnvironmentMapModel();
			ExtendedCameraModel cameraModel = JavaFXModels.getInstance().getCameraModel();
			
			ReadonlyIBRSettingsModel settingsModel = JavaFXModels.getInstance().getSettingsModel();
			ReadonlyIBRLoadOptionsModel loadOptionsModel = JavaFXModels.getInstance().getLoadOptionsModel();
			
			ToolSelectionModel toolModel = JavaFXModels.getInstance().getToolModel();

			ImageBasedRendererList<OpenGLContext> rendererList = new ImageBasedRendererList<OpenGLContext>(context, program);

			ToolBox.ToolBoxBuilder.create()
					.setCameraModel(cameraModel)
					.setEnvironmentMapModel(environmentMapModel)
					.setLightingModel(lightingModel)
					.setToolModel(toolModel)
					.setSceneViewportModel(new SceneViewportModel()
					{
						@Override
						public Object getObjectAtCoordinates(double x, double y) 
						{
							return rendererList.getSelectedItem().getSceneViewportModel().getObjectAtCoordinates(x, y);
						}

						@Override
						public Vector3 get3DPositionAtCoordinates(double x, double y) 
						{
							return rendererList.getSelectedItem().getSceneViewportModel().get3DPositionAtCoordinates(x, y);
						}
					})
					.setWindow(window)
					.build();
			
			IBRLoadingModel.getInstance().setLoadingHandler(rendererList);
			IBRLoadingModel.getInstance().setLoadOptionsModel(loadOptionsModel);
            
            rendererList.setObjectModel(() -> Matrix4.IDENTITY);
            rendererList.setCameraModel(cameraModel);
            rendererList.setLightingModel(lightingModel);
            rendererList.setSettingsModel(settingsModel);

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

						if (rendererList.getProgram() != null)
						{
							rendererList.getProgram().close();
						}
						rendererList.setProgram(newProgram);

						rendererList.getSelectedItem().reloadHelperShaders();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
				else if (c == 'l')
				{
					//metaLightingModel.hardcodedMode = !metaLightingModel.hardcodedMode;
				}
				else if (c == ' ')
				{
					fpController.setEnabled(!fpController.getEnabled());
				}
			});

			// Create a new application to run our event loop and give it the GLFWWindow for polling
			// of events and the OpenGL context.  The ULFRendererList provides the renderable.
			InteractiveApplication app = InteractiveGraphics.createApplication(window, context, rendererList.getRenderable());
			app.addRefreshable(new Refreshable()
			{
				@Override
				public void initialize()
				{
				}

				@Override
				public void refresh()
				{
					//fpController.update();
				}

				@Override
				public void terminate()
				{
				}
			});

			IBRRequestQueue<OpenGLContext> requestQueue = new IBRRequestQueue<OpenGLContext>(context, rendererList);

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

			window.show();
			app.run();

		}

		// The event loop has terminated so cleanup the windows and exit with a successful return code.
		GLFWWindow.closeAllWindows();
	}

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
}
