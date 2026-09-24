/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.app;

import javafx.stage.Stage;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.SynchronizedWindow;
import kintsugi3d.builder.core.WindowSynchronization;
import kintsugi3d.builder.io.IOModel;
import kintsugi3d.builder.rendering.*;
import kintsugi3d.builder.state.SelectableViewListModel;
import kintsugi3d.builder.state.scene.ActiveShaderModel;
import kintsugi3d.builder.state.scene.ManipulableLightingEnvironmentModel;
import kintsugi3d.builder.state.scene.ManipulableObjectPoseModel;
import kintsugi3d.builder.state.scene.ManipulableViewpointModel;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;
import kintsugi3d.builder.tools.*;
import kintsugi3d.gl.builders.framebuffer.DoubleFramebufferFactory;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.DoubleFramebufferObject;
import kintsugi3d.gl.glfw.CanvasWindow;
import kintsugi3d.gl.interactive.EventPollable;
import kintsugi3d.gl.interactive.InitializationException;
import kintsugi3d.gl.interactive.InteractiveApplication;
import kintsugi3d.gl.interactive.InteractiveGraphics;
import kintsugi3d.gl.opengl.OpenGLContext;
import kintsugi3d.gl.opengl.OpenGLContextFactory;
import kintsugi3d.gl.window.*;
import kintsugi3d.util.CanvasListener;
import kintsugi3d.util.KeyPress;
import kintsugi3d.util.MouseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public final class RenderingBootstrap
{
    private static final Logger LOG = LoggerFactory.getLogger(RenderingBootstrap.class);
    private static final int ICONIFIED_TIMEOUT_MILLIS = 5000;
    private static final int UNFOCUSED_TIMEOUT_MILLIS = 1000;

    // Allows us to pass the scene viewport to JavaFX before it is ready, using a temporary sentinel.
    private static final SceneViewportSafeWrapper SCENE_VIEWPORT_WRAPPER = new SceneViewportSafeWrapper();

    private RenderingBootstrap()
    {
    }

    static SceneViewport getSceneViewport()
    {
        return SCENE_VIEWPORT_WRAPPER;
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
                    .setDefaultFramebufferCreator(
                        c -> framebufferCapture.fbo = DoubleFramebufferFactory.create(c, 800, 800))
                    .create();

                FramebufferCanvas<OpenGLContext> canvas = FramebufferCanvas.createUsingExistingFramebuffer(framebufferCapture.fbo);
                JavaFXApplication.setCanvas(canvas);
                runProgram(stage, canvas, args);
            }
        }
        finally
        {
            // The event loop has terminated so cleanup the windows and exit with a successful return code.
            CanvasWindow.closeAllWindows();
        }
    }

    private static void setup3DWindow(Window window)
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
        context.getState().enableDepthTest();

        ManipulableLightingEnvironmentModel lightingModel = MultithreadState.getInstance().getLightingModel();
        ManipulableViewpointModel cameraModel = MultithreadState.getInstance().getCameraModel();
        ManipulableObjectPoseModel objectModel = MultithreadState.getInstance().getObjectModel();
        ActiveShaderModel activeShaderModel = MultithreadState.getInstance().getUserShaderModel();
        GeneralSettingsModel settingsModel = Global.state().getSettingsModel();
        SelectableViewListModel viewListModel = Global.state().getViewListModel();
        IOModel ioModel = Global.io();

        ToolBindingModel toolBindingModel = createToolBinding();

        ImageBasedRenderableManager<OpenGLContext> renderableManager = new ImageBasedRenderableManager<>(context);
        renderableManager.setObjectModel(objectModel);
        renderableManager.setCameraModel(cameraModel);
        renderableManager.setLightingModel(lightingModel);
        renderableManager.setUserShaderModel(activeShaderModel);
        renderableManager.setCameraViewListModel(viewListModel);
        renderableManager.setSettingsModel(settingsModel);

        // Replace the temporary sentinel with the actual scene viewport from the instance manager.
        SCENE_VIEWPORT_WRAPPER.setSceneViewport(renderableManager.getSceneViewport());

        // Create a new application to run our event loop and give it the WindowImpl for polling
        // of events and the OpenGL context.  The ULFRendererList provides the renderable.
        InteractiveApplication app = InteractiveGraphics.createApplication(canvas, context, renderableManager);
        app.setFPSCap(60.0); // TODO make this configurable

        app.addRefreshable(renderableManager.getRenderViews()); // i.e. views in carousel that also need to be in the refresh loop

        // Pass reference to instance manager to other components as needed.
        ioModel.setLoadingHandler(renderableManager);
        Rendering.initialize(context, renderableManager);

        // Allow frontend to react to IO events
        JavaFXApplication.getState().getProjectModel().registerIOListeners();

        CanvasListener canvasListener = ToolBox.Builder.create()
            .setCameraModel(cameraModel)
            .setLightingModel(lightingModel)
            .setObjectModel(objectModel)
            .setSettingsModel(settingsModel)
            .setToolBindingModel(toolBindingModel)
            .setSceneViewport(renderableManager.getSceneViewport())
            .build();

        canvasListener.addToCanvas(canvas);

        canvas.addKeyPressListener((win, key, modifierKeys) ->
        {
            if (key == Key.F11)
            {
                LOG.info("Reloading program...");

                try
                {
                    // reload program
                    renderableManager.getMainRenderable().reloadShaders();
                }
                catch (RuntimeException e)
                {
                    LOG.error("Error occurred while reloading application:", e);
                }
                catch(Error e)
                {
                    LOG.error("Error occurred while reloading application:", e);
                    //noinspection ProhibitedExceptionThrown
                    throw e;
                }
            }
        });

        GraphicsRequestQueue requestQueue = Rendering.getRequestQueue();

        // Used for wait / notify on rendering thread.
        Object waitForRenderingWork = new Object();

        // Keep the graphics thread paused while the window is minimized.
        if (stage != null)
        {
            app.addPollable(new EventPollable()
            {
                @Override
                public void pollEvents()
                {
                    synchronized (waitForRenderingWork)
                    {
                        while ((stage.isIconified() ||
                            javafx.stage.Window.getWindows().filtered(javafx.stage.Window::isFocused).isEmpty()) &&
                            requestQueue.isEmpty())
                        {
                            try
                            {
                                if (stage.isIconified())
                                {
                                    // Longer timeout (5 seconds) while minimized
                                    // Just wake it up periodically just in case we missed a condition for notifying the thread.
                                    waitForRenderingWork.wait(ICONIFIED_TIMEOUT_MILLIS);
                                }
                                else
                                {
                                    // Shorter timeout (1 second) while unfocused
                                    // This still reduces GPU load while still rendering regularly as a safeguard for edge cases
                                    // (such as refocusing using a window other than the main window).
                                    waitForRenderingWork.wait(UNFOCUSED_TIMEOUT_MILLIS);
                                }
                            }
                            catch (InterruptedException e)
                            {
                                LOG.warn("Wait interrupted", e);
                            }
                        }
                    }
                }

                @Override
                public boolean shouldTerminate()
                {
                    return false;
                }
            });
        }

        if (stage != null)
        {
            // Wake the graphics thread up when the window is un-minimized.
            stage.iconifiedProperty().addListener((observable, wasIconified, isIconified) ->
            {
                if (wasIconified && !isIconified && requestQueue.isEmpty())
                {
                    synchronized (waitForRenderingWork)
                    {
                        waitForRenderingWork.notifyAll();
                    }
                }
            });

            // Wake the graphics thread up when the window is refocused.
            stage.focusedProperty().addListener((observable, wasFocused, isFocused) ->
            {
                var focusedWindows = javafx.stage.Window.getWindows().filtered(javafx.stage.Window::isFocused);

                if (!wasFocused && isFocused && focusedWindows.size() == 1 && focusedWindows.get(0).equals(stage)
                    && requestQueue.isEmpty())
                {
                    synchronized (waitForRenderingWork)
                    {
                        waitForRenderingWork.notifyAll();
                    }
                }
            });

            // Wake the graphics thread up when work is added to the request queue.
            requestQueue.addRequestAddedListener(() ->
            {
                synchronized (waitForRenderingWork)
                {
                    waitForRenderingWork.notifyAll();
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
            Optional.ofNullable(ioModel.getProgressMonitor()).ifPresent(monitor -> monitor.fail(e));
            throw e;
        }
    }

    private static ToolBindingModel createToolBinding()
    {
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
        return toolBindingModel;
    }

    private static void processArgs(String... args)
    {
        // Load project if requested
        if (args.length >= 1)
        {
             Global.io().loadExistingProject(new File(args[0])); // Should initialize requestQueue
        }

        // Execute command if requested, using reflection
        if (args.length >= 2)
        {
            try
            {
                Class<?> requestClass = Class.forName(args[1]);
                Method createMethod = requestClass.getDeclaredMethod("create", String[].class);
                if (ProgressMonitoredGraphicsRequest.class.isAssignableFrom(createMethod.getReturnType())
                    && ((createMethod.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) == (Modifier.PUBLIC | Modifier.STATIC)))
                {
                    GraphicsRequestQueue requestQueue = Rendering.getRequestQueue();

                    // Add request to the queue
                    requestQueue.addGraphicsRequest((ProgressMonitoredGraphicsRequest) createMethod.invoke(null, (Object[]) args));

                    // Quit after the request finishes
                    // Use ProjectGraphicsRequest (rather than GraphicsRequest) so that it gets queued up after the actual request,
                    // once the project has finished loading
                    requestQueue.addBackgroundGraphicsRequest(new ImageBasedGraphicsRequest()
                    {
                        @Override
                        public <ContextType extends Context<ContextType>> void executeRequest(ImageBasedRenderable<ContextType> instance)
                        {
                            WindowSynchronization.getInstance().quitWithoutConfirmation();
                        }
                    });
                }
            }
            catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
            {
                LOG.error("Reflection error occurred processing arguments:", e);
            }
        }
    }

    private static void printSupportedImageFormats()
    {
        // Get list of all informal format names understood by the current set of registered readers
        String[] formatNames = ImageIO.getReaderFormatNames();

        Collection<String> set = Arrays.stream(formatNames)
            .map(s -> s.toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(() -> new HashSet<>(formatNames.length)));

        LOG.info("Supported image formats: {}", set);
    }
}
