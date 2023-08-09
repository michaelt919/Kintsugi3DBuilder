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

package kintsugi3d.builder.core;

import java.util.LinkedList;
import java.util.Queue;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.core.Context;
import kintsugi3d.builder.rendering.IBRInstanceManager;
import kintsugi3d.gl.interactive.GraphicsRequest;

public class IBRRequestManager<ContextType extends Context<ContextType>> implements IBRRequestQueue<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(IBRRequestManager.class);
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
        if (instanceManager.getLoadedInstance() == null)
        {
            // Instance is currently null, wait for a load and then call this function again (recursive-ish)
            instanceManager.addInstanceLoadCallback(instance -> addIBRRequest(request));
        }
        else
        {
            this.requestList.add(() ->
            {
                // Check again for null, just in case
                if (instanceManager.getLoadedInstance() == null)
                {
                    // Instance is currently null, wait for a load and then call this function again (recursive-ish)
                    instanceManager.addInstanceLoadCallback(instance -> addIBRRequest(request));
                }
                else
                {
                    // Suppress warning about catching and not rethrowing AssertionError.
                    // The request should effectively be regarded a "sandbox" where a critical logic error should not result in the application terminating.
                    //noinspection ErrorNotRethrown
                    try
                    {
                        request.executeRequest(instanceManager.getLoadedInstance(), loadingMonitor);
                    }
                    catch (Exception | AssertionError e)
                    {
                        log.error("Error occurred while executing request:", e);
                        Platform.runLater(() ->
                        {
                            new Alert(Alert.AlertType.ERROR, "An error occurred processing request. Processing has stopped.\nCheck the log for more info.").show();
                        });
                    }
                }
            });
        }
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
                log.error("Error occurred while executing request:", e);
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
