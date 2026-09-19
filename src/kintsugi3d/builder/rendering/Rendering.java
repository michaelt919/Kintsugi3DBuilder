/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.rendering;

import kintsugi3d.builder.core.Global;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.interactive.SimpleRefreshable;

public final class Rendering
{
    private static volatile Context<?> context;
    private static volatile RenderableManager<?> renderableManager;
    private static volatile GraphicsRequestManager<?> requestQueue;

    private static final Object INITIALIZATION_LOCK = new Object();

    private Rendering()
    {
    }

    public static Context<?> getContext()
    {
        if (context != null)
        {
            //noinspection StaticVariableUsedBeforeInitialization
            return context;
        }
        else
        {
            throw new IllegalStateException("Rendering context has not been initialized.");
        }
    }

    public static RenderableManager<?> getRenderableManager()
    {
        if (renderableManager != null)
        {
            //noinspection StaticVariableUsedBeforeInitialization
            return renderableManager;
        }
        else
        {
            throw new IllegalStateException("Renderable instance manager has not been initialized.");
        }
    }

    public static GraphicsRequestQueue getRequestQueue()
    {
        if (requestQueue != null)
        {
            //noinspection StaticVariableUsedBeforeInitialization
            return requestQueue;
        }
        else
        {
            throw new IllegalStateException("Rendering queue has not been initialized.");
        }
    }

    public static <ContextType extends Context<ContextType>> void initialize(
        ContextType injectedContext, RenderableManager<ContextType> injectedRenderableManager)
    {
        //noinspection SynchronizationOnStaticField
        synchronized (INITIALIZATION_LOCK)
        {
            if (context == null && renderableManager == null)
            {
                // Start the request queue as soon as we have a graphics context.
                GraphicsRequestManager<ContextType> newRequestQueue = new GraphicsRequestManager<>(injectedContext);
                newRequestQueue.setRenderableManager(injectedRenderableManager);
                                newRequestQueue.setProgressMonitor(Global.io().getProgressMonitor());

                if (injectedRenderableManager.getOwningApp() != null)
                {
                    injectedRenderableManager.getOwningApp().addRefreshable((SimpleRefreshable) newRequestQueue::executeQueue);

                    context = injectedContext;
                    renderableManager = injectedRenderableManager;
                    requestQueue = newRequestQueue;
                }
                else
                {
                    throw new IllegalStateException("Instance manager has not been assigned to an interactive application.");
                }
            }
            else
            {
                throw new IllegalStateException("Rendering has already been initialized.");
            }
        }
    }

    public static void runLater(GraphicsRequest request)
    {
        getRequestQueue().addBackgroundGraphicsRequest(request);
    }

    public static void runLater(Runnable runnable)
    {
        getRequestQueue().addBackgroundGraphicsRequest(new GraphicsRequest()
        {
            @Override
            public <ContextType extends Context<ContextType>> void executeRequest(ContextType context)
            {
                runnable.run();
            }
        });
    }
}
