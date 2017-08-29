package tetzlaff.gl.interactive;

import tetzlaff.gl.Context;
import tetzlaff.interactive.EventPollable;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.interactive.Refreshable;

/**
 * An singleton factory object for binding together the given ContextType and InteractiveRenderable as
 * an object which implements InteractiveApplication.
 * 
 * @see Refreshable InteractiveApplication
 * @author Michael Tetzlaff
 */
public class InteractiveGraphics
{
    /**
     * Make a new InteractiveApplication object that binds together the given InteractiveRenderable and
     * ContextType inside a new anonymous Refreshable.  The resulting application will always
     * have the context set as current appropriately before the InteractiveRenderable is used.  It will also
`     * flush/swap the context after drawing the InteractiveRenderable.
     *
     * @param pollable The EventPollable that will be used by the constructed InteractiveApplication.
     * @param context A ContextType that needs to coordinated with the given InteractiveRenderable.
     * @param renderable A InteractiveRenderable object that needs to coordinate with the given ContextType.
     * @param <ContextType> Used to specify the specific Context that should be used.  Will be passed as generic
     *        parameter to Context.
     * @return A new  InteractiveApplication with the given InteractiveRenderable and ContextType bound together
     *            as its Refreshable.
     */
    public static <ContextType extends Context<ContextType>> InteractiveApplication createApplication(EventPollable pollable, ContextType context, InteractiveRenderable<ContextType> renderable)
    {
        return new InteractiveApplication(pollable, new Refreshable()
        {
            @Override
            public void initialize()
            {
                context.makeContextCurrent();
                renderable.initialize();
            }

            @Override
            public void refresh()
            {
                context.makeContextCurrent();
                renderable.update();
                context.getDefaultFramebuffer().clearColorBuffer(0, 0, 0, 0, 0);
                renderable.draw(context.getDefaultFramebuffer());
                context.flush();
                context.swapBuffers();
            }

            @Override
            public void terminate()
            {
                context.makeContextCurrent();
                renderable.close();
            }
        });
    }
}
