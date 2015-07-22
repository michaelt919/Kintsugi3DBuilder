package tetzlaff.gl.helpers;

import tetzlaff.gl.Context;
import tetzlaff.interactive.EventPollable;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.interactive.Refreshable;

public class InteractiveGraphics
{	
	public static InteractiveApplication createApplication(EventPollable pollable, Context context, Drawable drawable)
	{
		return new InteractiveApplication(pollable, new Refreshable()
		{
			@Override
			public void initialize() 
			{
				context.makeContextCurrent();
				drawable.initialize();
			}

			@Override
			public void refresh() 
			{
				context.makeContextCurrent();
				drawable.update();
				drawable.draw();
				context.flush();
				context.swapBuffers();
			}

			@Override
			public void terminate() 
			{
				context.makeContextCurrent();
				drawable.cleanup();
				context.destroy();
			}
		});
	}
}
