package tetzlaff.interactive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class InteractiveApplication
{
	private EventPollable pollable;
	private Refreshable refreshable;
	
	public InteractiveApplication(EventPollable pollable, Refreshable refreshable) 
	{
		this.pollable = pollable;
		this.refreshable = refreshable;
	}

	public void run()
	{
		int pollingTime = 0;
		int refreshTime = 0;
		this.refreshable.initialize();
		Date startTimestamp = new Date();
		Date timestampA = startTimestamp;
		System.out.println("Main loop started.");
		while (!this.pollable.shouldTerminate())
		{
			this.refreshable.refresh();
			Date timestampB = new Date();
			refreshTime += timestampB.getTime() - timestampA.getTime();
			this.pollable.pollEvents();
			timestampA = new Date();
			pollingTime += timestampA.getTime() - timestampB.getTime();
		}
		System.out.println("Main loop terminated.");
		System.out.println("Total time elapsed: " + (timestampA.getTime() - startTimestamp.getTime()) + " milliseconds");
		System.out.println("Time spent polling for events: " + pollingTime + " milliseconds");
		System.out.println("Time spent on refreshes: " + refreshTime + " milliseconds");
		this.refreshable.terminate();
	}
	
	public static void runSimultaneous(Iterable<InteractiveApplication> apps)
	{
		Collection<InteractiveApplication> activeApps = new ArrayList<InteractiveApplication>();
		for (InteractiveApplication app : apps)
		{
			app.refreshable.initialize();
			activeApps.add(app);
		}
		
		while(!activeApps.isEmpty())
		{
			Collection<InteractiveApplication> appsToTerminate = new ArrayList<InteractiveApplication>();
			for (InteractiveApplication app : activeApps)
			{
				if (app.pollable.shouldTerminate())
				{
					appsToTerminate.add(app);
				}
			}
			for (InteractiveApplication app : appsToTerminate)
			{
				app.refreshable.terminate();
			}
			activeApps.removeAll(appsToTerminate);
			
			for (InteractiveApplication app : activeApps)
			{
				app.refreshable.refresh();
			}

			for (InteractiveApplication app : activeApps)
			{
				app.pollable.pollEvents();
			}
		}
	}
}
