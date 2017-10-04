package tetzlaff.ibrelight.app.old;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Objects;
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
import tetzlaff.ibrelight.core.IBRRequestQueue;
import tetzlaff.ibrelight.core.LoadingModel;
import tetzlaff.ibrelight.core.SettingsModel;
import tetzlaff.ibrelight.rendering.CameraBasedLightingModel;
import tetzlaff.ibrelight.rendering.ImageBasedRendererList;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.interactive.Refreshable;
import tetzlaff.models.CameraModel;
import tetzlaff.models.LightWidgetModel;
import tetzlaff.models.LightingModel;
import tetzlaff.models.ReadonlyCameraModel;
import tetzlaff.models.impl.SimpleCameraModel;
import tetzlaff.mvc.old.controllers.impl.FirstPersonController;
import tetzlaff.mvc.old.controllers.impl.TrackballController;
import tetzlaff.mvc.old.controllers.impl.TrackballLightController;
import tetzlaff.mvc.old.models.TrackballModel;

/**
 * ULFProgram is a container for the main entry point of the Unstructured Light Field
 * rendering program.
 * 
 * @author Michael Tetzlaff
 */
public final class IBRelight
{
    private static final boolean DEBUG = true;

    private IBRelight()
    {
    }

    private static class MetaLightingModel implements CameraBasedLightingModel
    {
        boolean hardcodedMode = false;
        LightingModel normalLightingModel;
        HardcodedLightingModel hardcodedLightingModel;

        @Override
        public int getLightCount()
        {
            return hardcodedMode ? hardcodedLightingModel.getLightCount() : normalLightingModel.getLightCount();
        }

        @Override
        public Vector3 getLightColor(int i)
        {
            return hardcodedMode ? hardcodedLightingModel.getLightColor(i) : normalLightingModel.getLightColor(i);
        }

        @Override
        public void setLightColor(int i, Vector3 lightColor)
        {
            normalLightingModel.setLightColor(i, lightColor);
        }

        @Override
        public Matrix4 getLightMatrix(int i)
        {
            return hardcodedMode ? hardcodedLightingModel.getLightMatrix(i) : normalLightingModel.getLightMatrix(i);
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
                hardcodedLightingModel.overrideCameraPose(cameraPoseOverride);
            }
        }

        @Override
        public void removeCameraPoseOverride()
        {
            if (hardcodedMode)
            {
                hardcodedLightingModel.removeCameraPoseOverride();
            }
        }

        @Override
        public Vector3 getAmbientLightColor()
        {
            return hardcodedMode ? hardcodedLightingModel.getAmbientLightColor() : normalLightingModel.getAmbientLightColor();
        }

        @Override
        public void setAmbientLightColor(Vector3 ambientLightColor)
        {
            normalLightingModel.setAmbientLightColor(ambientLightColor);
        }

        @Override
        public boolean isEnvironmentMappingEnabled()
        {
            return hardcodedMode ? hardcodedLightingModel.isEnvironmentMappingEnabled() : normalLightingModel.isEnvironmentMappingEnabled();
        }

        @Override
        public void setEnvironmentMappingEnabled(boolean enabled)
        {
            normalLightingModel.setEnvironmentMappingEnabled(enabled);
        }

        @Override
        public boolean isLightVisualizationEnabled(int index)
        {
            return hardcodedMode ? hardcodedLightingModel.isLightVisualizationEnabled(index) : normalLightingModel.isLightVisualizationEnabled(index);
        }

        @Override
        public boolean areLightWidgetsEthereal()
        {
            return hardcodedMode ? hardcodedLightingModel.areLightWidgetsEthereal() : normalLightingModel.areLightWidgetsEthereal();
        }

        @Override
        public LightWidgetModel getLightWidgetModel(int index)
        {
            return hardcodedMode ? hardcodedLightingModel.getLightWidgetModel(index) : normalLightingModel.getLightWidgetModel(index);
        }

        @Override
        public Vector3 getLightCenter(int i)
        {
            return hardcodedMode ? hardcodedLightingModel.getLightCenter(i) : normalLightingModel.getLightCenter(i);
        }

        @Override
        public void setLightCenter(int i, Vector3 lightCenter)
        {
            normalLightingModel.setLightCenter(i, lightCenter);
        }

        @Override
        public void setLightWidgetsEthereal(boolean lightWidgetsEthereal)
        {
            normalLightingModel.setLightWidgetsEthereal(lightWidgetsEthereal);
        }

        @Override
        public Matrix4 getEnvironmentMapMatrix()
        {
            return hardcodedMode ? hardcodedLightingModel.getEnvironmentMapMatrix() : normalLightingModel.getEnvironmentMapMatrix();
        }

        @Override
        public void setEnvironmentMapMatrix(Matrix4 environmentMapMatrix)
        {
            throw new UnsupportedOperationException();
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

    //        org.lwjgl.opengl.GL11.glEnable(GL_DEBUG_OUTPUT);
    //        GLDebugMessageCallback debugCallback = new GLDebugMessageCallback()
    //        {
    //            @Override
    //            public void invoke(int source, int type, int id, int severity, int length, long message, long userParam)
    //            {
    //                if (severity == GL_DEBUG_SEVERITY_HIGH)
    //                {
    //                    System.err.println(org.lwjgl.system.MemoryUtil.memDecodeASCII(message));
    //                }
    //            }
    //
    //        };
    //        glDebugMessageCallback(debugCallback, 0L);

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

            MetaLightingModel metaLightingModel = new MetaLightingModel();

            TrackballLightController lightController = new TrackballLightController();
            lightController.addAsWindowListener(window);
            metaLightingModel.normalLightingModel = lightController.getLightingModel();

            CameraModel fpCameraModel = new SimpleCameraModel();

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
                    (metaLightingModel.hardcodedMode ? hardcodedLightTrackballModel : lightController.getCurrentCameraModel()).getLookMatrix());

            // Create a new 'renderer' to be attached to the window and the GUI.
            // This is the object that loads the ULF models and handles drawing them.  This object abstracts
            // the underlying data and provides ways of triggering events via the trackball and the user
            // interface later when it is passed to the ULFUserInterface object.
            ImageBasedRendererList<OpenGLContext> rendererList = new ImageBasedRendererList<>(context, program);
            rendererList.setObjectModel(() -> Matrix4.IDENTITY);
            rendererList.setCameraModel(cameraModel);
            rendererList.setLightingModel(metaLightingModel);

            LoadingModel loadingModel = new LoadingModel();
            loadingModel.setLoadingHandler(rendererList);

            HardcodedLightingModel hardcodedLightController =
                    new HardcodedLightingModel(
                        () -> rendererList.getSelectedItem().getActiveViewSet(),
                        () -> rendererList.getSelectedItem().getActiveGeometry(),
                        hardcodedLightTrackballModel);
            metaLightingModel.hardcodedLightingModel = hardcodedLightController;

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

            window.addMouseButtonPressListener((win, buttonIndex, mods) ->
            {
                try
                {
                    if (Objects.equals(win, window) && rendererList.getSelectedItem() != null)
                    {
                        CursorPosition pos = window.getCursorPosition();
                        WindowSize size = window.getWindowSize();
                        double x = pos.x / size.width;
                        double y = pos.y / size.height;

                        System.out.println(rendererList.getSelectedItem().getSceneViewportModel().getObjectAtCoordinates(x, y) + "\t" +
                                rendererList.getSelectedItem().getSceneViewportModel().get3DPositionAtCoordinates(x, y));
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
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
                    fpController.update();
                }

                @Override
                public void terminate()
                {
                }
            });

    //        // Fire up the Qt Interface
    //        // Prepare the Qt GUI system
    //        QApplication.initialize(args);
    //        QCoreApplication.setOrganizationName("UW Stout");
    //        QCoreApplication.setOrganizationDomain("uwstout.edu");
    //        QCoreApplication.setApplicationName("PhotoScan Helper");
    //
    //        // As far as I can tell, the OS X native menu bar doesn't work in Qt Jambi
    //        // The Java process owns the native menu bar and won't relinquish it to Qt
    //        QApplication.setAttribute(ApplicationAttribute.AA_DontUseNativeMenuBar);

            IBRRequestQueue<OpenGLContext> requestQueue = new IBRRequestQueue<>(rendererList);

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

            SettingsModel settingsModel = new IBRSettingsModelImpl();
            rendererList.setSettingsModel(settingsModel);

            // Create a user interface that examines the ULFRendererList for renderer settings and
            // selecting between different loaded models.
            IBRelightConfigFrame gui = new IBRelightConfigFrame(
                    rendererList,
                    lightController.getLightingModel(),
                    settingsModel,
                    loadingModel,
                    request -> requestQueue.addRequest(request),
                    window.isHighDPI());
            gui.showGUI();
            //app.addPollable(gui); // Needed for Qt UI

            loadingModel.setLoadingMonitor(gui.getLoadingMonitor());
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
    public static void main(String... args) throws FileNotFoundException
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
        Set<String> set = new HashSet<>();

        // Get list of all informal format names understood by the current set of registered readers
        String[] formatNames = ImageIO.getReaderFormatNames();

        for (String formatName : formatNames)
        {
            set.add(formatName.toLowerCase());
        }

        System.out.println("Supported image formats: " + set);
    }
}
