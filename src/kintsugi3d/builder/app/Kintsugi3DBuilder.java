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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.builder.javafx.MainApplication;
import kintsugi3d.gl.interactive.InitializationException;

import java.io.File;
import java.io.IOException;

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
            System.setProperty("Kintsugi3D.logDir", ApplicationFolders.getUserAppDirectory() + "/logs");
        }

        Logger log = LoggerFactory.getLogger(Kintsugi3DBuilder.class);

        //allow render thread to modify user interface thread
        System.setProperty("glass.disableThreadChecks", "true");
        //TODO see com.sun.glass.ui.Application.java line 434

        if (GRAPHICS_WINDOW_ENABLED)
        {
            log.info("Starting JavaFX UI");
            new Thread(() -> MainApplication.launchWrapper("")).start();

            log.info("Starting Render Window");
            Rendering.runProgram(args);
        }
        else
        {
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
    }
}
