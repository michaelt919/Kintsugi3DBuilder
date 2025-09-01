/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.app;

import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.builder.preferences.GlobalUserPreferencesManager;
import kintsugi3d.gl.interactive.InitializationException;
import org.lwjgl.system.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class Kintsugi3DBuilder
{
    private static final boolean GRAPHICS_WINDOW_ENABLED = false;

    private Kintsugi3DBuilder()
    {
    }

    public static void main(String... args) throws IOException, InitializationException
    {
        // TODO: temp fix to make file I/O work as expected in an internationalized context
        Locale.setDefault(Locale.US);

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

        if (System.getProperty("os.name").toLowerCase().startsWith("mac"))
        {
            // Mac OS requires glfw_async to inject GLFW calls into the "first thread".
            Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
        }

        MainApplication.setArgs(args);

        File stderrFileName = new File(ApplicationFolders.getLogFileDirectory().toAbsolutePath().toFile(),
            String.format("Kintsugi3DBuilder-%s-standard-error.log",
                DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss").format(LocalDateTime.now())));

        if (GRAPHICS_WINDOW_ENABLED)
        {
            try (FileOutputStream standardErr = new FileOutputStream(stderrFileName);
                PrintStream err = new PrintStream(standardErr, false, StandardCharsets.UTF_8))
            {
                System.setErr(err);

                log.info("Starting JavaFX UI");
                new Thread(() -> MainApplication.launchWrapper("")).start();

                log.info("Starting Render Window");
                Rendering.runProgram(args);
                // TODO System.exit() call for standalone graphics window?
            }
        }
        else
        {
            try (FileOutputStream standardErr = new FileOutputStream(stderrFileName);
                 PrintStream err = new PrintStream(standardErr, false, StandardCharsets.UTF_8))
            {
                System.setErr(err);

                MainApplication.addStartListener(stage ->
                {
                    log.info("Starting Render Window");
                    new Thread(() ->
                    {
                        try
                        {
                            Rendering.runProgram(stage, args);
                        }
                        catch (InitializationException e)
                        {
                            log.error("Error initializing render window:", e);
                        }
                    }, "OpenGL Rendering Thread").start();
                });

                log.info("Starting JavaFX UI");
                MainApplication.launchWrapper("");
            }

            System.exit(0);
        }
    }
}
