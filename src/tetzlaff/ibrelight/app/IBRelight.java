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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javafx.application.Platform;
import org.xml.sax.SAXException;
import tetzlaff.ibrelight.javafx.MainApplication;
import tetzlaff.ibrelight.javafx.MultithreadModels;
import tetzlaff.interactive.InitializationException;

import javax.xml.parsers.ParserConfigurationException;

public final class IBRelight
{
    private static final boolean DEBUG = true;
    private static final boolean GRAPHICS_WINDOW_ENABLED = true;

    private IBRelight()
    {
    }

    public static void main(String... args) throws IOException, InitializationException
    {
        if (!DEBUG)
        {
            PrintStream out = new PrintStream("out.log");
            PrintStream err = new PrintStream("err.log");
            System.setOut(out);
            System.setErr(err);
        }

        //allow render thread to modify user interface thread
        System.setProperty("glass.disableThreadChecks", "true");
        //TODO see com.sun.glass.ui.Application.java line 434

        if (GRAPHICS_WINDOW_ENABLED)
        {
            System.out.println("Starting JavaFX UI");
            new Thread(() -> MainApplication.launchWrapper("")).start();

            System.out.println("Starting Render Window");
            Rendering.runProgram(args);
        }
        else
        {
            MainApplication.addStartListener(stage ->
            {
                System.out.println("Starting Render Window");
                new Thread(() ->
                {
                    try
                    {
                        Rendering.runProgram(stage, args);
                    }
                    catch (InitializationException e)
                    {
                        e.printStackTrace();
                    }
                }).start();
            });

            System.out.println("Starting JavaFX UI");
            MainApplication.launchWrapper("");
        }
    }
}
