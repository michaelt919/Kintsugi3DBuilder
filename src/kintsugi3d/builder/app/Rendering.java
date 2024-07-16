/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.app;//Created by alexk on 7/19/2017.

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;

import javafx.application.Platform;
import javafx.stage.Stage;
import kintsugi3d.builder.core.*;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import kintsugi3d.builder.rendering.IBRInstanceManager;
import kintsugi3d.builder.state.*;
import kintsugi3d.builder.tools.DragToolType;
import kintsugi3d.builder.tools.KeyPressToolType;
import kintsugi3d.builder.tools.ToolBindingModel;
import kintsugi3d.builder.tools.ToolBindingModelImpl;
import kintsugi3d.builder.tools.ToolBox.Builder;
import kintsugi3d.gl.builders.framebuffer.DefaultFramebufferFactory;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.DoubleFramebufferObject;
import kintsugi3d.gl.glfw.CanvasWindow;
import kintsugi3d.gl.interactive.*;
import kintsugi3d.gl.opengl.OpenGLContext;
import kintsugi3d.gl.opengl.OpenGLContextFactory;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.window.*;
import kintsugi3d.util.CanvasInputController;
import kintsugi3d.util.KeyPress;
import kintsugi3d.util.MouseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public final class Rendering
{
    private static final Logger log = LoggerFactory.getLogger(Rendering.class);
    private Rendering()
    {
    }

    public static void runLater(GraphicsRequest request)
    {
        requestQueue.addBackgroundGraphicsRequest(request);
    }

    public static void runLater(Runnable runnable)
    {
        requestQueue.addBackgroundGraphicsRequest(new GraphicsRequest()
        {
            @Override
            public <ContextType extends Context<ContextType>> void executeRequest(ContextType context) throws Exception
            {
                runnable.run();
            }
        });
    }

    private static IBRRequestManager<OpenGLContext> requestQueue = null;

    public static IBRRequestManager<OpenGLContext> getRequestQueue()
    {
        return requestQueue;
    }

    public static void runProgram(String... args) throws InitializationException
    {
        runProgram(null, args);
    }

    public static void runProgram(Stage stage, String... args) throws InitializationException
    {
        System.getenv();
        System.setProperty("org.lwjgl.util.DEBUG", "true");

        // Check for and print supported image formats (some are not as easy as you would think)
        printSupportedImageFormats();
        try
        {
            if (stage == null)
            {
                CanvasWindow<OpenGLContext> window = OpenGLContextFactory.getInstance().buildWindow("Kintsugi 3D Builder", 800, 800)
                    .setResizable(true)
                    .setMultisamples(4)
                    .create();
                setup3DWindow(window);
                runProgram(stage, window.getCanvas(), args);
            }
            else
            {
                var framebufferCapture = new Object()
                {
                    DoubleFramebufferObject<OpenGLContext> fbo;
                };

                // Need to still specify a native window to create the context, even though we won't use it.
                CanvasWindow<OpenGLContext> nativeWindow = OpenGLContextFactory.getInstance().buildWindow("<ignore>", 1, 1)
                    .setDefaultFramebufferCreator(c -> framebufferCapture.fbo = DefaultFramebufferFactory.create(c, 800, 800))
                    .create();

                FramebufferCanvas<OpenGLContext> canvas = FramebufferCanvas.createUsingExistingFramebuffer(framebufferCapture.fbo);
                MultithreadModels.getInstance().getCanvasModel().setCanvas(canvas);
                runProgram(stage, canvas, args);
            }
        }
        finally
        {
            // The event loop has terminated so cleanup the windows and exit with a successful return code.
            CanvasWindow.closeAllWindows();
        }
    }

    private static void setup3DWindow(PollableWindow window)
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

        window.getCanvas().addWindowCloseListener(canvas ->
        {
            // Cancel the window closing and let the window synchronization system close the window later if the user confirms that they want to exit.
            window.cancelWindowClose();
            WindowSynchronization.getInstance().quit();
        });

//            window.addWindowFocusGainedListener(win -> WindowSynchronization.getInstance().focusGained(glfwSynchronization));
//            window.addWindowFocusLostListener(win -> WindowSynchronization.getInstance().focusLost(glfwSynchronization));

        window.show();
    }

    private static void runProgram(Stage stage, PollableCanvas3D<OpenGLContext> canvas, String... args) throws InitializationException
    {
        OpenGLContext context = canvas.getContext();

        // Start the request queue as soon as we have a graphics context.
        requestQueue = new IBRRequestManager<>(context);

        context.getState().enableDepthTest();

        ExtendedLightingModel lightingModel = MultithreadModels.getInstance().getLightingModel();
        EnvironmentModel environmentModel = MultithreadModels.getInstance().getEnvironmentModel();
        ExtendedCameraModel cameraModel = MultithreadModels.getInstance().getCameraModel();
        ExtendedObjectModel objectModel = MultithreadModels.getInstance().getObjectModel();
        SettingsModel settingsModel = MultithreadModels.getInstance().getSettingsModel();
        CameraViewListModel cameraViewListModel = MultithreadModels.getInstance().getCameraViewListModel();
        IOModel ioModel = MultithreadModels.getInstance().getLoadingModel();

        // Bind tools
        ToolBindingModel toolBindingModel = new ToolBindingModelImpl();

        toolBindingModel.setDragTool(new MouseMode(0, ModifierKeys.NONE), DragToolType.ORBIT);
        toolBindingModel.setDragTool(new MouseMode(1, ModifierKeys.NONE), DragToolType.PAN);
        toolBindingModel.setDragTool(new MouseMode(2, ModifierKeys.NONE), DragToolType.PAN);
        toolBindingModel.setDragTool(new MouseMode(0, ModifierKeysBuilder.begin().alt().end()), DragToolType.TWIST);
        toolBindingModel.setDragTool(new MouseMode(1, ModifierKeysBuilder.begin().alt().end()), DragToolType.DOLLY);
        toolBindingModel.setDragTool(new MouseMode(2, ModifierKeysBuilder.begin().alt().end()), DragToolType.DOLLY);
        toolBindingModel.setDragTool(new MouseMode(0, ModifierKeysBuilder.begin().shift().end()), DragToolType.ROTATE_ENVIRONMENT);
        toolBindingModel.setDragTool(new MouseMode(1, ModifierKeysBuilder.begin().shift().end()), DragToolType.FOCAL_LENGTH);
        toolBindingModel.setDragTool(new MouseMode(2, ModifierKeysBuilder.begin().shift().end()), DragToolType.FOCAL_LENGTH);
        toolBindingModel.setDragTool(new MouseMode(1, ModifierKeysBuilder.begin().control().shift().end()), DragToolType.LOOK_AT_POINT);
        toolBindingModel.setDragTool(new MouseMode(2, ModifierKeysBuilder.begin().control().shift().end()), DragToolType.LOOK_AT_POINT);
        toolBindingModel.setDragTool(new MouseMode(0, ModifierKeysBuilder.begin().control().end()), DragToolType.OBJECT_ROTATION);
        toolBindingModel.setDragTool(new MouseMode(1, ModifierKeysBuilder.begin().control().end()), DragToolType.OBJECT_CENTER);
        toolBindingModel.setDragTool(new MouseMode(2, ModifierKeysBuilder.begin().control().end()), DragToolType.OBJECT_CENTER);
        toolBindingModel.setDragTool(new MouseMode(0, ModifierKeysBuilder.begin().control().alt().end()), DragToolType.OBJECT_TWIST);

        toolBindingModel.setKeyPressTool(new KeyPress(Key.UP, ModifierKeys.NONE), KeyPressToolType.ENVIRONMENT_BRIGHTNESS_UP_LARGE);
        toolBindingModel.setKeyPressTool(new KeyPress(Key.DOWN, ModifierKeys.NONE), KeyPressToolType.ENVIRONMENT_BRIGHTNESS_DOWN_LARGE);
        toolBindingModel.setKeyPressTool(new KeyPress(Key.RIGHT, ModifierKeys.NONE), KeyPressToolType.ENVIRONMENT_BRIGHTNESS_UP_SMALL);
        toolBindingModel.setKeyPressTool(new KeyPress(Key.LEFT, ModifierKeys.NONE), KeyPressToolType.ENVIRONMENT_BRIGHTNESS_DOWN_SMALL);
        toolBindingModel.setKeyPressTool(new KeyPress(Key.UP, ModifierKeysBuilder.begin().shift().end()), KeyPressToolType.BACKGROUND_BRIGHTNESS_UP_LARGE);
        toolBindingModel.setKeyPressTool(new KeyPress(Key.DOWN, ModifierKeysBuilder.begin().shift().end()), KeyPressToolType.BACKGROUND_BRIGHTNESS_DOWN_LARGE);
        toolBindingModel.setKeyPressTool(new KeyPress(Key.RIGHT, ModifierKeysBuilder.begin().shift().end()), KeyPressToolType.BACKGROUND_BRIGHTNESS_UP_SMALL);
        toolBindingModel.setKeyPressTool(new KeyPress(Key.LEFT, ModifierKeysBuilder.begin().shift().end()), KeyPressToolType.BACKGROUND_BRIGHTNESS_DOWN_SMALL);
        toolBindingModel.setKeyPressTool(new KeyPress(Key.L, ModifierKeys.NONE), KeyPressToolType.TOGGLE_LIGHTS);
        toolBindingModel.setKeyPressTool(new KeyPress(Key.L, ModifierKeysBuilder.begin().control().end()), KeyPressToolType.TOGGLE_LIGHT_WIDGETS);

        IBRInstanceManager<OpenGLContext> instanceManager = new IBRInstanceManager<>(context);

        SceneViewportModel sceneViewportModel = MultithreadModels.getInstance().getSceneViewportModel();

        sceneViewportModel.setSceneViewport(new SceneViewport()
        {
            @Override
            public Object getObjectAtCoordinates(double x, double y)
            {
                if (instanceManager.getLoadedInstance() != null)
                {
                    return instanceManager.getLoadedInstance().getSceneViewportModel().getObjectAtCoordinates(x, y);
                }
                else
                {
                    return null;
                }
            }

            @Override
            public Vector3 get3DPositionAtCoordinates(double x, double y)
            {
                if (instanceManager.getLoadedInstance() != null)
                {
                    return instanceManager.getLoadedInstance().getSceneViewportModel().get3DPositionAtCoordinates(x, y);
                }
                else
                {
                    return Vector3.ZERO;
                }
            }

            @Override
            public Vector3 getViewingDirection(double x, double y)
            {
                if (instanceManager.getLoadedInstance() != null)
                {
                    return instanceManager.getLoadedInstance().getSceneViewportModel().getViewingDirection(x, y);
                }
                else
                {
                    return Vector3.ZERO;
                }
            }

            @Override
            public Vector3 getViewportCenter()
            {
                if (instanceManager.getLoadedInstance() != null)
                {
                    return instanceManager.getLoadedInstance().getSceneViewportModel().getViewportCenter();
                }
                else
                {
                    return Vector3.ZERO;
                }
            }

            @Override
            public Vector2 projectPoint(Vector3 point)
            {
                if (instanceManager.getLoadedInstance() != null)
                {
                    return instanceManager.getLoadedInstance().getSceneViewportModel().projectPoint(point);
                }
                else
                {
                    return Vector2.ZERO;
                }
            }

            @Override
            public float getLightWidgetScale()
            {
                if (instanceManager.getLoadedInstance() != null)
                {
                    return instanceManager.getLoadedInstance().getSceneViewportModel().getLightWidgetScale();
                }
                else
                {
                    return 1.0f;
                }
            }
        });

        CanvasInputController canvasInputController = Builder.create()
            .setCameraModel(cameraModel)
            .setEnvironmentModel(environmentModel)
            .setLightingModel(lightingModel)
            .setObjectModel(objectModel)
            .setSettingsModel(settingsModel)
            .setToolBindingModel(toolBindingModel)
            .setSceneViewportModel(sceneViewportModel)
            .build();

        canvasInputController.addAsCanvasListener(canvas);

        ioModel.setLoadingHandler(instanceManager);

        instanceManager.setObjectModel(objectModel);
        instanceManager.setCameraModel(cameraModel);
        instanceManager.setLightingModel(lightingModel);
        instanceManager.setCameraViewListModel(cameraViewListModel);
        instanceManager.setSettingsModel(settingsModel);

        canvas.addKeyPressListener((win, key, modifierKeys) ->
        {
            if (key == Key.F11)
            {
                log.info("Reloading program...");

                try
                {
                    // reload program
                    instanceManager.getLoadedInstance().reloadShaders();
                }
                catch (RuntimeException e)
                {
                    log.error("Error occurred while reloading application:", e);
                }
                catch(Error e)
                {
                    log.error("Error occurred while reloading application:", e);
                    //noinspection ProhibitedExceptionThrown
                    throw e;
                }
            }
        });

        // Create a new application to run our event loop and give it the WindowImpl for polling
        // of events and the OpenGL context.  The ULFRendererList provides the renderable.
        InteractiveApplication app = InteractiveGraphics.createApplication(canvas, context, instanceManager);
        app.setFPSCap(60.0); // TODO make this configurable

        requestQueue.setInstanceManager(instanceManager);
        requestQueue.setLoadingMonitor(new LoadingMonitor()
        {
            @Override
            public void startLoading()
            {
                ioModel.getLoadingMonitor().startLoading();
            }

            @Override
            public void setMaximum(double maximum)
            {
                ioModel.getLoadingMonitor().setMaximum(maximum);
            }

            @Override
            public void setProgress(double progress)
            {
                ioModel.getLoadingMonitor().setProgress(progress);
            }

            @Override
            public void loadingComplete()
            {
                ioModel.getLoadingMonitor().loadingComplete();
            }

            @Override
            public void loadingFailed(Throwable e)
            {
                ioModel.getLoadingMonitor().loadingFailed(e);
            }

            @Override
            public void loadingWarning(Throwable e)
            {
                ioModel.getLoadingMonitor().loadingWarning(e);
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

        // Keep the graphics thread paused while the window is minimized.
        if (stage != null)
        {
            app.addPollable(new EventPollable()
            {
                @Override
                public void pollEvents()
                {
                    while (stage.isIconified() && requestQueue.isEmpty())
                    {
                        Thread.onSpinWait();
                    }
                }

                @Override
                public boolean shouldTerminate()
                {
                    return false;
                }
            });
        }

        // Wake the graphics thread up when the window is un-minimized.
        if (stage != null)
        {
            Thread graphicsThread = Thread.currentThread();

            stage.iconifiedProperty().addListener((observable, wasIconified, isIconified) ->
            {
                if (wasIconified && !isIconified && requestQueue.isEmpty())
                {
                    graphicsThread.interrupt();
                }
            });
        }

        // Process CLI args after the main window has loaded.
        processArgs(args);

        try
        {
            app.run();
        }
        catch(RuntimeException|InitializationException|Error e)
        {
            Optional.ofNullable(ioModel.getLoadingMonitor()).ifPresent(loadingMonitor -> loadingMonitor.loadingFailed(e));
            throw e;
        }
    }

    private static void processArgs(String... args)
    {
        if (args.length == 0){WelcomeWindowController.getInstance().show();}

        // Load project if requested
        if (args.length >= 1)
        {
            if (args[0].endsWith(".vset"))
            {
                File vsetFile = new File(args[0]);
                new Thread(() -> MultithreadModels.getInstance().getLoadingModel().loadFromVSETFile(vsetFile.getPath(), vsetFile)).start();
            }
            else
            {
                // Using Platform.runLater since full IBR projects include stuff that's managed by JavaFX (cameras, lights, etc.)
                Platform.runLater(() ->
                {
                    try
                    {
                        File vsetFile = MultithreadModels.getInstance().getProjectModel().openProjectFile(new File(args[0]));
                        new Thread(() -> MultithreadModels.getInstance().getLoadingModel().loadFromVSETFile(vsetFile.getPath(), vsetFile)).start();
                    }
                    catch (IOException | ParserConfigurationException | SAXException e)
                    {
                        log.error("Error occurred processing arguments:", e);
                    }
                });
            }
        }

        // Execute command if requested, using reflection
        if (args.length >= 2)
        {
            try
            {
                Class<?> requestClass = Class.forName(args[1]);
                Method createMethod = requestClass.getDeclaredMethod("create", Kintsugi3DBuilderState.class, String[].class);
                if (ObservableIBRRequest.class.isAssignableFrom(createMethod.getReturnType())
                    && ((createMethod.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) == (Modifier.PUBLIC | Modifier.STATIC)))
                {
                    // Add request to the queue
                    requestQueue.addIBRRequest((ObservableIBRRequest) createMethod.invoke(null, MultithreadModels.getInstance(), args));

                    // Quit after the request finishes
                    // Use IBRRequest (rather than GraphicsRequest) so that it gets queued up after the actual request,
                    // once the project has finished loading
                    requestQueue.addBackgroundIBRRequest(new IBRRequest()
                    {
                        @Override
                        public <ContextType extends Context<ContextType>> void executeRequest(IBRInstance<ContextType> renderable)
                        {
                            WindowSynchronization.getInstance().quitWithoutConfirmation();
                        }
                    });
                }
            }
            catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
            {
                log.error("Reflection error occurred processing arguments:", e);
            }
        }
    }

    private static void printSupportedImageFormats()
    {
        // Get list of all informal format names understood by the current set of registered readers
        String[] formatNames = ImageIO.getReaderFormatNames();

        Collection<String> set = Arrays.stream(formatNames)
            .map(String::toLowerCase)
            .collect(Collectors.toCollection(() -> new HashSet<>(formatNames.length)));

        log.info("Supported image formats: " + set);
    }
}
