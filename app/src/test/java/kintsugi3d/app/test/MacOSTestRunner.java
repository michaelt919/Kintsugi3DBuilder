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

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public final class MacOSTestRunner
{
    private MacOSTestRunner()
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
            var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(SmokeTest.class))
                .build();

            try (var session = LauncherFactory.openSession())
            {
                var launcher = session.getLauncher();
                launcher.registerTestExecutionListeners(listener);
                launcher.execute(request);
            }

            Rendering.requestTerminate();
        },
        "JUnit Test Thread").start();

        // Start rendering on the main thread.
        RenderingBootstrap.runProgram(null, false);

        var summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out, true, StandardCharsets.UTF_8));
        summary.printFailuresTo(new PrintWriter(System.out, true, StandardCharsets.UTF_8));

        if (summary.getTotalFailureCount() > 0)
        {
            System.exit(1);
        }
    }
}