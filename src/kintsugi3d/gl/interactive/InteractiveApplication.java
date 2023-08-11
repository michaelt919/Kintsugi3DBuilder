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

package kintsugi3d.gl.interactive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class InteractiveApplication
{
    private static final Logger log = LoggerFactory.getLogger(InteractiveApplication.class);
    private final List<EventPollable> pollables;
    private final List<Refreshable> refreshables;

    private double fpsCap = Double.POSITIVE_INFINITY;

    private static final boolean FPS_COUNTER = false;

    public InteractiveApplication(EventPollable pollable, Refreshable refreshable)
    {
        this.pollables = new ArrayList<>(16);
        this.pollables.add(pollable);
        this.refreshables = new ArrayList<>(16);
        this.refreshables.add(refreshable);
    }

    public void addPollable(EventPollable pollable)
    {
        this.pollables.add(pollable);
    }

    public void addRefreshable(Refreshable refreshable)
    {
        this.refreshables.add(refreshable);
    }

    public void run() throws InitializationException
    {
        for (Refreshable refreshable : this.refreshables)
        {
            refreshable.initialize();
        }
        Date startTimestamp = new Date();
        Date timestampA = startTimestamp;
        log.info("Main loop started.");
        boolean shouldTerminate = false;
        int refreshTime = 0;
        int pollingTime = 0;

        long lastSecond = startTimestamp.getTime();
        int frames = 0;

        while (!shouldTerminate)
        {
            int frameTime = 0;

            for (Refreshable refreshable : this.refreshables)
            {
                try
                {
                    refreshable.refresh();
                }
                catch(RuntimeException e)
                {
                    log.error("Runtime error occurred", e);
                }
            }
            Date timestampB = new Date();
            refreshTime += timestampB.getTime() - timestampA.getTime();
            frameTime += timestampB.getTime() - timestampA.getTime();
            for (EventPollable poller : pollables)
            {
                try
                {
                    poller.pollEvents();
                    shouldTerminate = shouldTerminate || poller.shouldTerminate();
                }
                catch(RuntimeException e)
                {
                    log.error("An error occurred while polling events", e);
                }
            }
            timestampA = new Date();
            pollingTime += timestampA.getTime() - timestampB.getTime();
            frameTime += timestampA.getTime() - timestampB.getTime();

            if (FPS_COUNTER)
            {
                if (timestampA.getTime() - lastSecond > 1000)
                {
                    log.info("FPS: " + frames);
                    lastSecond = timestampA.getTime();
                    frames = 0;
                }
                else
                {
                    frames++;
                }
            }

            // Sleep if necessary to not exceed the fps cap
            double minFrameTime = 1000.0 / fpsCap;
            if (frameTime < minFrameTime)
            {
                try
                {
                    Thread.sleep(Math.round(minFrameTime - frameTime));
                }
                catch (InterruptedException e)
                {
                    log.error("Interrupted while waiting for min frame delta", e);
                }
            }
        }

        log.info("Main loop terminated.");
        log.info("Total time elapsed: " + (timestampA.getTime() - startTimestamp.getTime()) + " milliseconds");
        log.info("Time spent polling for events: " + pollingTime + " milliseconds");
        log.info("Time spent on refreshes: " + refreshTime + " milliseconds");

        for (Refreshable refreshable : this.refreshables)
        {
            try
            {
                refreshable.terminate();
            }
            catch(RuntimeException e)
            {
                log.error("Error terminating refreshable:", e);
            }
        }

        System.exit(0);
    }

    public static void runSimultaneous(Iterable<InteractiveApplication> apps) throws InitializationException
    {
        Collection<InteractiveApplication> activeApps = new ArrayList<>(16);
        for (InteractiveApplication app : apps)
        {
            for (Refreshable refreshable : app.refreshables)
            {
                refreshable.initialize();
            }
            activeApps.add(app);
        }

        while(!activeApps.isEmpty())
        {
            Collection<InteractiveApplication> appsToTerminate = new ArrayList<>(16);
            for (InteractiveApplication app : activeApps)
            {
                boolean shouldTerminate = false;
                for (EventPollable poller : app.pollables)
                {
                    try
                    {
                        shouldTerminate = shouldTerminate || poller.shouldTerminate();
                    }
                    catch(RuntimeException e)
                    {
                        log.error("An error occurred:", e);
                    }
                }

                if (shouldTerminate)
                {
                    appsToTerminate.add(app);
                }
            }
            for (InteractiveApplication app : appsToTerminate)
            {
                for (Refreshable refreshable : app.refreshables)
                {
                    try
                    {
                        refreshable.terminate();
                    }
                    catch(RuntimeException e)
                    {
                        log.error("An error has occurred:", e);
                    }
                }
            }
            activeApps.removeAll(appsToTerminate);

            for (InteractiveApplication app : activeApps)
            {
                for (Refreshable refreshable : app.refreshables)
                {
                    try
                    {
                        refreshable.refresh();
                    }
                    catch(RuntimeException e)
                    {
                        log.error("An error has occurred:", e);
                    }
                }
            }

            for (InteractiveApplication app : activeApps)
            {
                for (EventPollable poller : app.pollables)
                {
                    try
                    {
                        poller.pollEvents();
                    }
                    catch(RuntimeException e)
                    {
                        log.error("An error has occurred:", e);
                    }
                }
            }
        }
    }

    public double getFPSCap()
    {
        return fpsCap;
    }

    public void setFPSCap(double fpsCap)
    {
        this.fpsCap = fpsCap;
    }
}
