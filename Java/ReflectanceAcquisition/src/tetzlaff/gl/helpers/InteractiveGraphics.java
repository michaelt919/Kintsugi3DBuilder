package tetzlaff.gl.helpers;

import java.io.File;

import tetzlaff.gl.Context;
import tetzlaff.interactive.EventPollable;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.interactive.Refreshable;

/**
 * An singleton factory object for binding together the given ContextType and Drawable as
 * an object which implements InteractiveApplication.
 * 
 * @see Refreshable InteractiveApplication
 * @author Michael Tetzlaff
 */
public class InteractiveGraphics
{	
	/**
	 * Make a new InteractiveApplication object that binds together the given Drawable and
	 * ContextType inside a new anonymous Refreshable.  The resulting application will always
	 * have the context set as current appropriately before the Drawable is used.  It will also
`	 * flush/swap the context after drawing the Drawable.
	 * 
	 * @param pollable The EventPollable that will be used by the constructed InteractiveApplication.
	 * @param context A ContextType that needs to coordinated with the given Drawable.
	 * @param drawable A Drawable object that needs to coordinate with the given ContextType.
	 * @param <ContextType> Used to specify the specific Context that should be used.  Will be passed as generic
	 *        parameter to Context.
	 * @return A new  InteractiveApplication with the given Drawable and ContextType bound together
	 * 		   as its Refreshable.
	 */
	public static <ContextType extends Context<ContextType>> InteractiveApplication createApplication(EventPollable pollable, ContextType context, Drawable drawable)
	{
		return new InteractiveApplication(pollable, new Refreshable()
		{
			private volatile boolean screenshotRequested = false;
			private String fileFormat;
			private File file;
			
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
				if(screenshotRequested)
				{
					drawable.saveToFile(fileFormat, file);
					screenshotRequested = false;
				}
				context.swapBuffers();
			}

			@Override
			public void terminate() 
			{
				context.makeContextCurrent();
				drawable.cleanup();
				context.destroy();
			}

			@Override
			public void requestScreenshot(String fileFormat, File file)
			{
				this.fileFormat = fileFormat;
				this.file = file;
				screenshotRequested = true;
			}
		});
	}
}
