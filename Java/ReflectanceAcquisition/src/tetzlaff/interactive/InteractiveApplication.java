package tetzlaff.interactive;

import java.util.ArrayList;
import java.util.Collection;

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
		this.refreshable.initialize();
		while (!this.pollable.shouldTerminate())
		{
			this.refreshable.refresh();
			this.pollable.pollEvents();
		}
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
