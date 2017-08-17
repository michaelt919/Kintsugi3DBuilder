package tetzlaff.interactive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class InteractiveApplication
{
    private final List<EventPollable> pollables;
    private final List<Refreshable> refreshables;

    public InteractiveApplication(EventPollable pollable, Refreshable refreshable)
    {
        this.pollables = new ArrayList<EventPollable>();
        this.pollables.add(pollable);
        this.refreshables = new ArrayList<Refreshable>();
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

    public void run()
    {
        int pollingTime = 0;
        int refreshTime = 0;
        for (Refreshable refreshable : this.refreshables)
        {
            refreshable.initialize();
        }
        Date startTimestamp = new Date();
        Date timestampA = startTimestamp;
        System.out.println("Main loop started.");
        boolean shouldTerminate = false;
        while (!shouldTerminate)
        {
            for (Refreshable refreshable : this.refreshables)
            {
                refreshable.refresh();
            }
            Date timestampB = new Date();
            refreshTime += timestampB.getTime() - timestampA.getTime();
            for (EventPollable poller : pollables)
            {
                poller.pollEvents();
                shouldTerminate = shouldTerminate || poller.shouldTerminate();
            }
            timestampA = new Date();
            pollingTime += timestampA.getTime() - timestampB.getTime();

        }
        System.out.println("Main loop terminated.");
        System.out.println("Total time elapsed: " + (timestampA.getTime() - startTimestamp.getTime()) + " milliseconds");
        System.out.println("Time spent polling for events: " + pollingTime + " milliseconds");
        System.out.println("Time spent on refreshes: " + refreshTime + " milliseconds");
        for (Refreshable refreshable : this.refreshables)
        {
            refreshable.terminate();
        }
    }

    public static void runSimultaneous(Iterable<InteractiveApplication> apps)
    {
        Collection<InteractiveApplication> activeApps = new ArrayList<InteractiveApplication>();
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
            Collection<InteractiveApplication> appsToTerminate = new ArrayList<InteractiveApplication>();
            for (InteractiveApplication app : activeApps)
            {
                boolean shouldTerminate = false;
                for (EventPollable poller : app.pollables)
                {
                    shouldTerminate = shouldTerminate || poller.shouldTerminate();
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
                    refreshable.terminate();
                }
            }
            activeApps.removeAll(appsToTerminate);

            for (InteractiveApplication app : activeApps)
            {
                for (Refreshable refreshable : app.refreshables)
                {
                    refreshable.refresh();
                }
            }

            for (InteractiveApplication app : activeApps)
            {
                for (EventPollable poller : app.pollables)
                {
                    poller.pollEvents();
                }
            }
        }
    }
}
