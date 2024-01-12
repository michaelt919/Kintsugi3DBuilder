/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.app;

import kintsugi3d.builder.preferences.GlobalUserPreferencesManager;
import kintsugi3d.builder.preferences.ReadOnlyDirectoryPreferencesModel;
import kintsugi3d.gl.exceptions.GLFWException;
import org.lwjgl.glfw.*;
import org.lwjgl.system.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.builder.javafx.MainApplication;
import kintsugi3d.gl.interactive.InitializationException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public final class Kintsugi3DBuilder
{
    private static final boolean GRAPHICS_WINDOW_ENABLED = false;

    private Kintsugi3DBuilder()
    {
    }

    public static void main(String... args) throws IOException, InitializationException
    {
        // Dynamically set the log directory based on the OS before instantiating a logger
        if (System.getProperty("Kintsugi3D.logDir") == null)
        {
            System.setProperty("Kintsugi3D.logDir", ApplicationFolders.getLogFileDirectory().toAbsolutePath().toString());
        }

        Logger log = LoggerFactory.getLogger(Kintsugi3DBuilder.class);
        log.debug("Logger initialized");

        // Log any exceptions that may have occurred while loading the log file directory from preferences
        List<Exception> startupExceptions = GlobalUserPreferencesManager.getInstance().getSerializerStartupExceptions();
        for (Exception e : startupExceptions)
        {
            log.error("A user preferences exception occurred during pre-logger application startup", e);
        }

        if (!startupExceptions.isEmpty())
        {
            log.warn("Exceptions occurred while attempting to read the user preferences file, and the default preferences were likely restored.");
        }

        // Log all directories that will be used for session
        log.info("Application log file directory: {}", ApplicationFolders.getLogFileDirectory());
        log.info("Application data directory: {}", ApplicationFolders.getUserAppDirectory());
        log.info("Application cache directory: {}", ApplicationFolders.getUserCacheDirectory());
        log.info("Application system data directory: {}", ApplicationFolders.getSystemAppDirectory());
        log.info("Application installation directory: {}", ApplicationFolders.getInstallationDirectory());
        log.info("Preview images root directory: {}", ApplicationFolders.getPreviewImagesRootDirectory());
        log.info("Fit cache root directory: {}", ApplicationFolders.getFitCacheRootDirectory());

        //allow render thread to modify user interface thread
        System.setProperty("glass.disableThreadChecks", "true");
        //TODO see com.sun.glass.ui.Application.java line 434


//        if (System.getProperty("os.name").toLowerCase().contains("mac") || GRAPHICS_WINDOW_ENABLED)
//        {
////            var startFlag = new Object()
////            {
////                boolean started;
////            };
////
////            MainApplication.addStartListener(stage ->
////            {
////                startFlag.started = true;
////            });
//
////            log.info("Starting JavaFX UI");
////            new Thread(() -> MainApplication.launchWrapper("")).start();
//
//            // Wait for JavaFX to start
////            while (!startFlag.started)
////            {
////                Thread.onSpinWait();
////            }
//
////            try
////            {
////                Thread.sleep(5000L);
////            }
////            catch (InterruptedException e)
////            {
////                throw new RuntimeException(e);
////            }
//
//            Configuration.GLFW_CHECK_THREAD0.set(false);
//
//            log.info("Starting Render Window");
//            Rendering.runProgram(args);
//
//            // TODO System.exit() call for standalone graphics window?
//        }
//        else
        {
            Configuration.GLFW_CHECK_THREAD0.set(false);

            MainApplication.addStartListener(stage ->
            {
                if ( glfwInit() != true )
                {
                    throw new GLFWException("Unable to initialize GLFW.");
                }
                glfwDefaultWindowHints();

                glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
                glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
                glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 1);
                glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

                glfwWindowHint(GLFW_VISIBLE, GL_FALSE);

                long handle = glfwCreateWindow(1, 1, "asdf", NULL, NULL);
                if ( handle == NULL )
                {
                    throw new GLFWException("Failed to create the GLFW window");
                }

                log.info("Starting Render Window");
                new Thread(() ->
                {
                    try
                    {
                        Rendering.runProgram(stage, handle, args);
                    }
                    catch (InitializationException e)
                    {
                        log.error("Error initializing render window:", e);
                    }
                }, "OpenGL Rendering Thread").start();
            });

            log.info("Starting JavaFX UI");
            MainApplication.launchWrapper("");
            System.exit(0);
        }
    }
}
