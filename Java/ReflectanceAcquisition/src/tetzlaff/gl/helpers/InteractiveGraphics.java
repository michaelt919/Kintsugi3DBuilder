package tetzlaff.gl.helpers;

import java.io.File;

import tetzlaff.gl.Context;
import tetzlaff.gl.Framebuffer;
import tetzlaff.interactive.EventPollable;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.interactive.Refreshable;
import tetzlaff.window.Window;

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
			private long fpsLastCheck = 0;
			
			private Exception drawableError = null;
			
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
				if(!drawable.draw())
				{
					clearDefaultBuffer();
				}
				context.flush();
				if(screenshotRequested)
				{
					drawable.saveToFile(fileFormat, file);
					screenshotRequested = false;
				}
				context.swapBuffers();
				
				if(drawable.hasInitializeError())
				{
					drawableError = drawable.getInitializeError();
				}
				
				if(((System.nanoTime()/1000000) - fpsLastCheck) > 1000 && pollable instanceof Window)
				{
					int FPS = drawable.getCurFPS();
					if(FPS >= 0) {
						Window myWin = (Window)pollable;
						myWin.setWindowTitle(String.format("Unstructured Light Field Renderer, FPS: %3d [%3d, %3d]",
								FPS, drawable.getMinFPS(), drawable.getMaxFPS()));
					}
					fpsLastCheck += 1000;
				}
			}
			
			private void clearDefaultBuffer()
			{
		    	Framebuffer<ContextType> framebuffer = context.getDefaultFramebuffer();
	    		framebuffer.clearColorBuffer(0, 0.30f, 0.30f, 0.30f, 1.0f);
			}

			@Override
			public void terminate() 
			{
				context.makeContextCurrent();
				drawable.cleanup();
				context.destroy();
			}

			@Override
			public void requestDebugDump(String fileFormat, File file)
			{
				this.fileFormat = fileFormat;
				this.file = file;
				screenshotRequested = true;
			}
			
			@Override
			public boolean hasError()
			{
				return (drawableError!=null);
			}

			@Override
			public Exception getError()
			{
				Exception tempRef = drawableError;
				drawableError = null;
				return tempRef;
			}
		});
	}
}
