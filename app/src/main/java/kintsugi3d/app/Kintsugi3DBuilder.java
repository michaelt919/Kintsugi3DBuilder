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

import javafx.application.Application;
import kintsugi3d.builder.preferences.GlobalUserPreferencesManager;
import kintsugi3d.builder.util.ApplicationFolders;
import kintsugi3d.gl.interactive.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        Locale.setDefault(Locale.ROOT);

        // Dynamically set the log directory based on the OS before instantiating a logger
        ApplicationFolders.makeLogDirectorySystemProperty();

        Logger log = LoggerFactory.getLogger(Kintsugi3DBuilder.class);
        log.debug("Logger initialized");

        GlobalUserPreferencesManager.getInstance().logStartupExceptions();
        ApplicationFolders.logApplicationDirectories();

        // Set system properties, etc. that need to be set before anything touches LWJGL
        // (which JavaFX does indirectly by loading Kintsugi's matrix math classes)
        RenderingBootstrap.preInitialize();

        JavaFXApplication.setArgs(args);

        File stderrFileName = new File(ApplicationFolders.getLogFileDirectory().toAbsolutePath().toFile(),
            String.format("Kintsugi3DBuilder-%s-standard-error.log",
                DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss").format(LocalDateTime.now())));

        if (GRAPHICS_WINDOW_ENABLED)
        {
            try (FileOutputStream standardErr = new FileOutputStream(stderrFileName);
                PrintStream err = new PrintStream(new TeeErrorStream(standardErr), false, StandardCharsets.UTF_8))
            {
                System.setErr(err);

                log.info("Starting JavaFX UI");
                new Thread(() -> Application.launch("")).start();

                log.info("Starting Render Window");
                RenderingBootstrap.runProgram(args);
                // TODO System.exit() call for standalone graphics window?
            }
        }
        else
        {
            try (FileOutputStream standardErr = new FileOutputStream(stderrFileName);
                 PrintStream err = new PrintStream(new TeeErrorStream(standardErr), false, StandardCharsets.UTF_8))
            {
                System.setErr(err);

                JavaFXApplication.addStartListener(stage -> RenderingBootstrap.startRenderThread(stage, args));

                log.info("Starting JavaFX UI");
                JavaFXApplication.launchWrapper("");
            }

            System.exit(0);
        }
    }

    private static final class TeeErrorStream extends OutputStream
    {
        private final PrintStream original;
        private final OutputStream redirect;

        private TeeErrorStream(OutputStream redirect)
        {
            this.original = System.err;
            this.redirect = redirect;
        }

        @Override
        public void write(int b) throws IOException
        {
            original.write(b);
            redirect.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
            original.write(b, off, len);
            redirect.write(b, off, len);
        }

        @Override
        public void flush() throws IOException
        {
            original.flush();
            redirect.flush();
        }

        @Override
        public void close() throws IOException
        {
            System.setErr(this.original);
            redirect.close();
        }
    }
}
