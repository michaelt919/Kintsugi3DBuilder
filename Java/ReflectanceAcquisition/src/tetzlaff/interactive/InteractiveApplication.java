package tetzlaff.interactive;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import tetzlaff.gl.exceptions.GLException;

public class InteractiveApplication
{
	private List<EventPollable> pollable;
	private Refreshable refreshable;
	private static MessageBox userMessageBox = null;
	
	public InteractiveApplication(EventPollable pollable, Refreshable refreshable) 
	{
		this.pollable = new ArrayList<EventPollable>();
		this.pollable.add(pollable);
		this.refreshable = refreshable;
	}
	
	public static void setMessageBox(MessageBox newMessageBox)
	{
		userMessageBox = newMessageBox;
	}
	
	public static MessageBox getMessageBox()
	{
		return userMessageBox;
	}

	public void addPollable(EventPollable pollable)
	{
		this.pollable.add(pollable);		
	}
	
	public void requestScreenshot(String fileFormat, File file)
	{
		refreshable.requestScreenshot(fileFormat, file);
	}
	
	public void run()
	{
		int pollingTime = 0;
		int refreshTime = 0;
		
		this.refreshable.initialize();

		Date startTimestamp = new Date();
		Date timestampA = startTimestamp;
		
		System.out.println("Main loop started.");
		boolean shouldTerminate = false;		
		while (!shouldTerminate)
		{
			this.refreshable.refresh();
			if(this.refreshable.hasDrawableError())
			{
				Exception drawableError = refreshable.getDrawableError();
				drawableError.printStackTrace();

				if(userMessageBox != null)
				{
					if(drawableError instanceof GLException || (drawableError.getCause() != null && drawableError.getCause() instanceof GLException))
					{
						userMessageBox.warning("GL Rendering Error", "An error occured with the rendering system. " +
								"Your GPU and/or video memory may be insufficient for rendering this model.\n\n[" +
								drawableError.getMessage() + "]");
					}
					else if(drawableError instanceof FileNotFoundException || (drawableError.getCause() != null && drawableError.getCause() instanceof GLException))
					{
						userMessageBox.warning("Resource Error", "An error occured while loading resources. " +
								"Check that all necessary files exist and that the proper paths were supplied.\n\n[" +
								drawableError.getMessage() + "]");
					}
					else
					{
						userMessageBox.warning("Application Error", "An error occured that prevents this model from being rendered." +
								"\n\n[" + drawableError.getMessage() + "]");
					}
				}
			}
			
			Date timestampB = new Date();
			refreshTime += timestampB.getTime() - timestampA.getTime();
			for (EventPollable poller : pollable)
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
				boolean shouldTerminate = false;
				for (EventPollable poller : app.pollable)
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
				app.refreshable.terminate();
			}
			activeApps.removeAll(appsToTerminate);
			
			for (InteractiveApplication app : activeApps)
			{
				app.refreshable.refresh();
			}

			for (InteractiveApplication app : activeApps)
			{
				for (EventPollable poller : app.pollable)
				{
					poller.pollEvents();
				}
			}
		}
	}
}
