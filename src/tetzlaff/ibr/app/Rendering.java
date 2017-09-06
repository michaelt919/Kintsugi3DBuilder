package tetzlaff.ibr.app;//Created by alexk on 7/19/2017.

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

import tetzlaff.gl.Program;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.glfw.GLFWWindow;
import tetzlaff.gl.glfw.GLFWWindowFactory;
import tetzlaff.gl.interactive.InteractiveGraphics;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.Key;
import tetzlaff.ibr.core.IBRRequestQueue;
import tetzlaff.ibr.core.LoadingModel;
import tetzlaff.ibr.core.LoadingMonitor;
import tetzlaff.ibr.core.SettingsModel;
import tetzlaff.ibr.javafx.models.JavaFXModelAccess;
import tetzlaff.ibr.rendering.ImageBasedRendererList;
import tetzlaff.ibr.tools.ToolBindingModel;
import tetzlaff.ibr.tools.ToolBox.Builder;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.interactive.Refreshable;
import tetzlaff.models.*;
import tetzlaff.models.impl.BasicCameraModel;
import tetzlaff.mvc.old.controllers.impl.FirstPersonController;
import tetzlaff.util.WindowBasedController;

public final class Rendering
{
    private Rendering()
    {
    }

    private static volatile IBRRequestQueue<?> requestQueue;

    public static IBRRequestQueue<?> getRequestQueue()
    {
        if (requestQueue == null)
        {
            System.out.println("Waiting for requestQueue to be initialized...");
        }
        while (requestQueue == null)
        {
        }
        System.out.println("requestQueue initialized; continuing.");
        return requestQueue;
    }

    public static void runProgram()
    {
        System.getenv();
        System.setProperty("org.lwjgl.util.DEBUG", "true");

        // Check for and print supported image formats (some are not as easy as you would think)
        printSupportedImageFormats();

        // Create a GLFW window for integration with LWJGL (part of the 'view' in this MVC arrangement)
        try(GLFWWindow<OpenGLContext> window =
                    GLFWWindowFactory.buildOpenGLWindow("IBRelight", 800, 800)
                            .setResizable(true)
                            .setMultisamples(4)
                            .create())
        {
            SynchronizedWindow glfwSynchronization = new SynchronizedWindow()
            {
                @Override
                public boolean isFocused()
                {
                    return window.isFocused();
                }

                @Override
                public void focus()
                {
                    // TODO uncomment this if it becomes possible to upgrade to new version of LWJGL that supports window focus through updated GLFW.
                    //new Thread(window::focus).start();
                }

                @Override
                public void quit()
                {
                    window.requestWindowClose();
                }
            };

            WindowSynchronization.getInstance().addListener(glfwSynchronization);

            window.addWindowCloseListener(win -> WindowSynchronization.getInstance().quit());
//            window.addWindowFocusGainedListener(win -> WindowSynchronization.getInstance().focusGained(glfwSynchronization));
//            window.addWindowFocusLostListener(win -> WindowSynchronization.getInstance().focusLost(glfwSynchronization));


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
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
                throw new IllegalStateException("The shader program could not be initialized.", e);
            }

            CameraModel fpCameraModel = new BasicCameraModel();

            FirstPersonController fpController = new FirstPersonController(fpCameraModel);
            fpController.addAsWindowListener(window);

            window.addMouseButtonPressListener((win, buttonIndex, mods) -> fpController.setEnabled(false));

            ExtendedLightingModel lightingModel = JavaFXModelAccess.getInstance().getLightingModel();
            EnvironmentMapModel environmentMapModel = JavaFXModelAccess.getInstance().getEnvironmentMapModel();
            ExtendedCameraModel cameraModel = JavaFXModelAccess.getInstance().getCameraModel();
            ExtendedObjectModel objectModel = JavaFXModelAccess.getInstance().getObjectModel();
            SettingsModel settingsModel = JavaFXModelAccess.getInstance().getSettingsModel();
            LoadingModel loadingModel = JavaFXModelAccess.getInstance().getLoadingModel();
            ToolBindingModel toolModel = JavaFXModelAccess.getInstance().getToolModel();

            ImageBasedRendererList<OpenGLContext> rendererList = new ImageBasedRendererList<>(context, program);

            WindowBasedController windowBasedController = Builder.create()
                .setCameraModel(cameraModel)
                .setEnvironmentMapModel(environmentMapModel)
                .setLightingModel(lightingModel)
                .setObjectModel(objectModel)
                .setSettingsModel(settingsModel)
                .setToolModel(toolModel)
                .setSceneViewportModel(new SceneViewportModel()
                {
                    @Override
                    public Object getObjectAtCoordinates(double x, double y)
                    {
                        if (rendererList.getSelectedItem() != null)
                        {
                            return rendererList.getSelectedItem().getSceneViewportModel().getObjectAtCoordinates(x, y);
                        }
                        else
                        {
                            return null;
                        }
                    }

                    @Override
                    public Vector3 get3DPositionAtCoordinates(double x, double y)
                    {
                        if (rendererList.getSelectedItem() != null)
                        {
                            return rendererList.getSelectedItem().getSceneViewportModel().get3DPositionAtCoordinates(x, y);
                        }
                        else
                        {
                            return Vector3.ZERO;
                        }
                    }

                    @Override
                    public Vector3 getViewingDirection(double x, double y)
                    {
                        if (rendererList.getSelectedItem() != null)
                        {
                            return rendererList.getSelectedItem().getSceneViewportModel().getViewingDirection(x, y);
                        }
                        else
                        {
                            return Vector3.ZERO;
                        }
                    }

                    @Override
                    public Vector3 getViewportCenter()
                    {
                        if (rendererList.getSelectedItem() != null)
                        {
                            return rendererList.getSelectedItem().getSceneViewportModel().getViewportCenter();
                        }
                        else
                        {
                            return Vector3.ZERO;
                        }
                    }

                    @Override
                    public Vector2 projectPoint(Vector3 point)
                    {
                        if (rendererList.getSelectedItem() != null)
                        {
                            return rendererList.getSelectedItem().getSceneViewportModel().projectPoint(point);
                        }
                        else
                        {
                            return Vector2.ZERO;
                        }
                    }

                    @Override
                    public float getLightWidgetScale()
                    {
                        if (rendererList.getSelectedItem() != null)
                        {
                            return rendererList.getSelectedItem().getSceneViewportModel().getLightWidgetScale();
                        }
                        else
                        {
                            return 1.0f;
                        }
                    }
                })
                .build();

            windowBasedController.addAsWindowListener(window);

            loadingModel.setLoadingHandler(rendererList);
            
            rendererList.setObjectModel(() -> Matrix4.IDENTITY);
            rendererList.setCameraModel(cameraModel);
            rendererList.setLightingModel(lightingModel);
            rendererList.setObjectModel(objectModel);
            rendererList.setSettingsModel(settingsModel);

            window.addKeyPressListener((win, key, modifierKeys) ->
            {
                if (key == Key.F10)
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
                    catch (FileNotFoundException|RuntimeException e)
                    {
                        e.printStackTrace();
                    }
                }
                //else if (key == Key.L)
                //{
                    //metaLightingModel.hardcodedMode = !metaLightingModel.hardcodedMode;
                //}
                else if (key == Key.SPACE)
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

            requestQueue = new IBRRequestQueue<>(rendererList);
            requestQueue.setLoadingMonitor(new LoadingMonitor()
            {
                @Override
                public void startLoading()
                {
                    loadingModel.getLoadingMonitor().startLoading();
                }

                @Override
                public void setMaximum(double maximum)
                {
                    loadingModel.getLoadingMonitor().setMaximum(maximum);
                }

                @Override
                public void setProgress(double progress)
                {
                    loadingModel.getLoadingMonitor().setProgress(progress);
                }

                @Override
                public void loadingComplete()
                {
                    loadingModel.getLoadingMonitor().loadingComplete();
                }
            });

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

    private static void printSupportedImageFormats()
    {
        // Get list of all informal format names understood by the current set of registered readers
        String[] formatNames = ImageIO.getReaderFormatNames();

        Collection<String> set = Arrays.stream(formatNames)
            .map(String::toLowerCase)
            .collect(Collectors.toCollection(() -> new HashSet<>(formatNames.length)));

        System.out.println("Supported image formats: " + set);
    }
}
