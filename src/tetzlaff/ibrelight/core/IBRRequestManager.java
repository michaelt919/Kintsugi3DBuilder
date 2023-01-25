/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.core;

import java.util.LinkedList;
import java.util.Queue;

import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.rendering.IBRInstanceManager;
import tetzlaff.interactive.GraphicsRequest;

public class IBRRequestManager<ContextType extends Context<ContextType>> implements IBRRequestQueue<ContextType>
{
    private final ContextType context;
    private final Queue<Runnable> requestList;
    private IBRInstanceManager<ContextType> instanceManager;
    private LoadingMonitor loadingMonitor;

    public IBRRequestManager(ContextType context)
    {
        this.context = context;
        this.requestList = new LinkedList<>();
    }

    public boolean isEmpty()
    {
        return requestList.isEmpty();
    }

    public void setInstanceManager(IBRInstanceManager<ContextType> instanceManager)
    {
        this.instanceManager = instanceManager;
    }

    public void setLoadingMonitor(LoadingMonitor loadingMonitor)
    {
        this.loadingMonitor = loadingMonitor;
    }

    @Override
    public void addIBRRequest(IBRRequest<ContextType> request)
    {
        this.requestList.add(() ->
        {
            if (instanceManager.getLoadedInstance() == null)
            {
                // Instance is currently null, wait for a load
                instanceManager.addInstanceLoadCallback(instance ->
                {
                    // Suppress warning about catching and not rethrowing AssertionError.
                    // The request should effectively be regarded a "sandbox" where a critical logic error should not result in the application terminating.
                    //noinspection ErrorNotRethrown
                    try
                    {
                        request.executeRequest(instance, loadingMonitor);
                    }
                    catch(Exception | AssertionError e)
                    {
                        e.printStackTrace();
                    }
                });
            }
            else
            {
                // Instance is not currently null, execute now.
                instanceManager.addInstanceLoadCallback(instance ->
                {
                    // Suppress warning about catching and not rethrowing AssertionError.
                    // The request should effectively be regarded a "sandbox" where a critical logic error should not result in the application terminating.
                    //noinspection ErrorNotRethrown
                    try
                    {
                        request.executeRequest(instanceManager.getLoadedInstance(), loadingMonitor);
                    }
                    catch(Exception | AssertionError e)
                    {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    @Override
    public void addGraphicsRequest(GraphicsRequest<ContextType> request)
    {
        this.requestList.add(() ->
        {
            // Suppress warning about catching and not rethrowing AssertionError.
            // The request should effectively be regarded a "sandbox" where a critical logic error should not result in the application terminating.
            //noinspection ErrorNotRethrown
            try
            {
                request.executeRequest(context, loadingMonitor);
            }
            catch(Exception | AssertionError e)
            {
                e.printStackTrace();
            }
        });
    }

    public void executeQueue()
    {
        //if (model != null && model.getLoadedInstance() != null)
        {
            context.makeContextCurrent();

            while (!requestList.isEmpty())
            {
                if (loadingMonitor != null)
                {
                    loadingMonitor.startLoading();
                }

                requestList.peek().run(); // Peek first to ensure that isEmpty() returns false when called from other threads.
                requestList.poll();       // Once the task is done, remove the request from the queue.

                if (loadingMonitor != null)
                {
                    loadingMonitor.loadingComplete();
                }
            }
        }
    }
}
