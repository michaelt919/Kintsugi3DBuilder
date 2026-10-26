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

package kintsugi3d.app.test;

import kintsugi3d.app.RenderingBootstrap;
import kintsugi3d.builder.rendering.Rendering;
import kintsugi3d.gl.interactive.InitializationException;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public final class MainThreadRenderingTestRunner
{
    private static final Logger LOG = LoggerFactory.getLogger(MainThreadRenderingTestRunner.class);

    private MainThreadRenderingTestRunner()
    {
    }

    public static void main(String[] args) throws InitializationException
    {
        SmokeTest.setRenderThreadRequired(false);
        SmokeTest.preRenderThreadInitialize();

        var listener = new SummaryGeneratingListener();

        // Start thread to run JUnit tests
        new Thread(() ->
        {
            //noinspection OverlyBroadCatchBlock
            try
            {
                LOG.debug("Started JUnit Test Thread.");

                var request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectClass(SmokeTest.class))
                    .build();

                LOG.debug("Trying to open JUnit launcher session.");

                try (var session = LauncherFactory.openSession())
                {
                    LOG.debug("JUnit launcher session opened.");

                    var launcher = session.getLauncher();
                    launcher.registerTestExecutionListeners(listener);

                    LOG.debug("Trying to start JUnit tests.");

                    launcher.execute(request);

                    LOG.debug("JUnit tests complete.");
                }

                Rendering.requestTerminate();
            }
            catch (RuntimeException e)
            {
                LOG.error("Error running JUnit tests.", e);
            }
        },
        "JUnit Test Thread").start();

        // Start rendering on the main thread.
        RenderingBootstrap.runProgram(null, false);

        LOG.debug("Rendering loop terminated.");

        var summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out, true, StandardCharsets.UTF_8));
        summary.printFailuresTo(new PrintWriter(System.out, true, StandardCharsets.UTF_8));

        if (summary.getTotalFailureCount() > 0)
        {
            System.exit(1);
        }
    }
}