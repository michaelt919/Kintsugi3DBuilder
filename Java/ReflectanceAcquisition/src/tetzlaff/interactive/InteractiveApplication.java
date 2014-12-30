package tetzlaff.interactive;

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
}
