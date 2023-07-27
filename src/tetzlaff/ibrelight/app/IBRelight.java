/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tetzlaff.ibrelight.javafx.MainApplication;
import tetzlaff.interactive.InitializationException;

import java.io.File;
import java.io.IOException;

public final class IBRelight
{
    private static final boolean GRAPHICS_WINDOW_ENABLED = false;
    public static final String APP_FOLDER_NAME = "Kintsugi3DBuilder";

    private IBRelight()
    {
    }

    public static void main(String... args) throws IOException, InitializationException
    {
        // Dynamically set the log directory based on the OS before instantiating a logger
        if (System.getProperty("Kintsugi3D.logDir") == null)
        {
            System.setProperty("Kintsugi3D.logDir", getUserAppDirectory() + "/logs");
        }

        Logger log = LoggerFactory.getLogger(IBRelight.class);

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
                }).start();
            });

            log.info("Starting JavaFX UI");
            MainApplication.launchWrapper("");
        }
    }

    public static File getUserAppDirectory()
    {
        String os = System.getProperty("os.name").toLowerCase();

        // Windows
        if (os.indexOf("win") >= 0)
        {
            return new File(new File(System.getenv("APPDATA")), APP_FOLDER_NAME);
        }

        // Mac OS
        if (os.indexOf("mac") >= 0)
        {
            return new File(new File(System.getProperty("user.home")), "Library/Application Support/" + APP_FOLDER_NAME);
        }

        // Linux and Unix
        if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") >= 0)
        {
            return new File(new File(System.getProperty("user.home")), "." + APP_FOLDER_NAME);
        }

        return new File(".");
    }
}
