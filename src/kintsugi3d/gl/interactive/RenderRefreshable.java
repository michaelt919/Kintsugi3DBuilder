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
import kintsugi3d.gl.core.DoubleFramebuffer;
import kintsugi3d.gl.core.DoubleFramebufferObject;
import kintsugi3d.gl.core.Resource;

import java.util.List;

public final class RenderRefreshable<ContextType extends Context<ContextType>> implements Refreshable
{
    private final ContextType context;
    private final InteractiveRenderable<ContextType> renderable;
    private final DoubleFramebuffer<ContextType> framebuffer;
    private final Iterable<Resource> managedResources;

    private boolean initialized = false;

    public static <ContextType extends Context<ContextType>> RenderRefreshable<ContextType> createWithManagedFrambufferObject(
        ContextType context, InteractiveRenderable<ContextType> renderable, DoubleFramebufferObject<ContextType> framebuffer)
    {
        return new RenderRefreshable<>(context, renderable, framebuffer, List.of(renderable, framebuffer));
    }

    public static <ContextType extends Context<ContextType>> RenderRefreshable<ContextType> createWithDefaultFrambufferObject(
        ContextType context, InteractiveRenderable<ContextType> renderable)
    {
        return new RenderRefreshable<>(context, renderable, context.getDefaultFramebuffer(), List.of(renderable));
    }

    private RenderRefreshable(ContextType context, InteractiveRenderable<ContextType> renderable,
                             DoubleFramebuffer<ContextType> framebuffer, Iterable<Resource> managedResources)
    {
        this.context = context;
        this.renderable = renderable;
        this.framebuffer = framebuffer;
        this.managedResources = managedResources;
    }

    @Override
    public boolean isInitialized()
    {
        return this.initialized;
    }

    @Override
    public void initialize() throws InitializationException
    {
        context.makeContextCurrent();
        renderable.initialize();
        this.initialized = true;
    }

    @Override
    public void refresh()
    {
        context.makeContextCurrent();
        renderable.update();
        framebuffer.clearColorBuffer(0, 0, 0, 0, 0);
        renderable.draw(framebuffer);
        context.flush();
        framebuffer.swapBuffers();
    }

    @Override
    public void terminate()
    {
        context.makeContextCurrent();

        for (Resource r : managedResources)
        {
            r.close();
        }
    }
}
