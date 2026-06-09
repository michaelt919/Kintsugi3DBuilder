/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.interactive;

import kintsugi3d.gl.core.Context;

/**
 * An singleton factory object for binding together the given ContextType and InteractiveRenderable as
 * an object which implements InteractiveApplication.
 * 
 * @see Refreshable InteractiveApplication
 * @author Michael Tetzlaff
 */
public final class InteractiveGraphics
{
    private InteractiveGraphics()
    {
    }

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
    public static <ContextType extends Context<ContextType>> InteractiveApplication createApplication(
        EventPollable pollable, ContextType context, InteractiveRenderable<ContextType> renderable)
    {
        if (renderable.getOwningApp() != null)
        {
            throw new IllegalStateException("Renderable is already owned by another application!");
        }

        InteractiveApplication app = new InteractiveApplication(pollable,
            RenderRefreshable.createWithDefaultFrambufferObject(context, renderable));

        renderable.setOwningApp(app);

        return app;
    }

}
