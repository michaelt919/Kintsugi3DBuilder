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

package kintsugi3d.builder.core;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.rendering.ProjectInstanceManager;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.interactive.GraphicsRequest;
import kintsugi3d.gl.interactive.ObservableGraphicsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

public class GraphicsRequestManager<ContextType extends Context<ContextType>> implements GraphicsRequestQueue<ContextType>
{
    private static final Logger LOG = LoggerFactory.getLogger(GraphicsRequestManager.class);
    private final ContextType context;
    private final Queue<Runnable> requestList;
    private final Collection<Runnable> requestAddedListeners = new ArrayList<>(1);
    private ProjectInstanceManager<ContextType> instanceManager;
    private ProgressMonitor progressMonitor;

    public GraphicsRequestManager(ContextType context)
    {
        this.context = context;
        this.requestList = new LinkedList<>();
    }

    public boolean isEmpty()
    {
        return requestList.isEmpty();
    }

    public void setInstanceManager(ProjectInstanceManager<ContextType> instanceManager)
    {
        this.instanceManager = instanceManager;
    }

    public void setProgressMonitor(ProgressMonitor progressMonitor)
    {
        this.progressMonitor = progressMonitor;
    }

    private static void handleCancellation()
    {
        Platform.runLater(() ->
        {
            Alert alert = new Alert(AlertType.INFORMATION, "The operation was cancelled. Processing has stopped.");
            alert.setTitle("Cancelled");
            alert.setHeaderText("Cancelled");
            alert.show();
        });
    }

    public void addRequestAddedListener(Runnable listener)
    {
        requestAddedListeners.add(listener);
    }

    @Override
    public void addBackgroundGraphicsRequest(ProjectGraphicsRequest request)
    {
        if (instanceManager.getLoadedInstance() == null)
        {
            // Instance is currently null, wait for a load and then call this function again (recursive-ish)
            instanceManager.addInstanceLoadCallback(instance -> addBackgroundGraphicsRequest(request));
        }
        else
        {
            synchronized (requestList)
            {
                this.requestList.add(() ->
                {
                    // Check again for null, just in case
                    if (instanceManager.getLoadedInstance() == null)
                    {
                        // Instance is currently null, wait for a load and then call this function again (recursive-ish)
                        instanceManager.addInstanceLoadCallback(instance -> addBackgroundGraphicsRequest(request));
                    }
                    else
                    {
                        // Suppress warning about catching and not rethrowing AssertionError.
                        // The request should effectively be regarded a "sandbox" where a critical logic error should not result in the application terminating.
                        //noinspection ErrorNotRethrown
                        try
                        {
                            request.executeRequest(instanceManager.getLoadedInstance());
                        }
                        catch (UserCancellationException e)
                        {
                            LOG.error("Operation was cancelled while executing request", e);
                        }
                        catch (Exception | AssertionError e)
                        {
                            LOG.error("Error occurred while executing request", e);
                        }
                    }
                });
            }

            // Notify listeners
            for (Runnable r :  requestAddedListeners)
            {
                r.run();
            }
        }
    }

    @Override
    public void addGraphicsRequest(ObservableProjectGraphicsRequest request)
    {
        if (this.progressMonitor.isConflictingProcess())
        {
            return;
        }

        if (instanceManager.getLoadedInstance() == null)
        {
            // Instance is currently null, wait for a load and then call this function again (recursive-ish)
            instanceManager.addInstanceLoadCallback(instance -> addGraphicsRequest(request));
        }
        else
        {
            synchronized (requestList)
            {
                this.requestList.add(() ->
                {
                    if (progressMonitor != null)
                    {
                        progressMonitor.start();
                    }

                    // Check again for null, just in case
                    if (instanceManager.getLoadedInstance() == null)
                    {
                        // Instance is currently null, wait for a load and then call this function again (recursive-ish)
                        instanceManager.addInstanceLoadCallback(instance -> addGraphicsRequest(request));
                    }
                    else
                    {
                        // Suppress warning about catching and not rethrowing AssertionError.
                        // The request should effectively be regarded a "sandbox" where a critical logic error should not result in the application terminating.
                        //noinspection ErrorNotRethrown
                        try
                        {
                            request.executeRequest(instanceManager.getLoadedInstance(), progressMonitor);
                        }
                        catch (UserCancellationException e)
                        {
                            LOG.error("Operation was cancelled while executing request", e);
                            handleCancellation();
                        }
                        catch (Exception | AssertionError e)
                        {
                            ExceptionHandling.error("Error occured while excecuting request", e);
                        }
                    }

                    if (progressMonitor != null)
                    {
                        progressMonitor.complete();
                    }
                });
            }
        }

        // Notify listeners
        for (Runnable r :  requestAddedListeners)
        {
            r.run();
        }
    }

    /**
     * Add a graphics request without a loading monitor
     *
     * @param request
     */
    @Override
    public void addBackgroundGraphicsRequest(GraphicsRequest request)
    {
        synchronized (requestList)
        {
            this.requestList.add(() ->
            {

                // Suppress warning about catching and not rethrowing AssertionError.
                // The request should effectively be regarded a "sandbox" where a critical logic error should not result in the application terminating.
                // noinspection ErrorNotRethrown
                try
                {
                    request.executeRequest(context);
                }
                catch (UserCancellationException e)
                {
                    LOG.error("Operation was cancelled while executing request", e);
                }
                catch (Exception | AssertionError e)
                {
                    LOG.error("Error occurred while executing request", e);
                }
            });
        }

        // Notify listeners
        for (Runnable r :  requestAddedListeners)
        {
            r.run();
        }
    }

    @Override
    public void addGraphicsRequest(ObservableGraphicsRequest request)
    {
        synchronized (requestList)
        {
            this.requestList.add(() ->
            {
                if (progressMonitor != null)
                {
                    if (this.progressMonitor.isConflictingProcess())
                    {
                        return;
                    }
                    progressMonitor.start();
                }

                // Suppress warning about catching and not rethrowing AssertionError.
                // The request should effectively be regarded a "sandbox" where a critical logic error should not result in the application terminating.
                // noinspection ErrorNotRethrown
                try
                {
                    request.executeRequest(context, progressMonitor);
                }
                catch (UserCancellationException e)
                {
                    LOG.error("Operation was cancelled while executing request", e);
                    handleCancellation();
                }
                catch (Exception | AssertionError e)
                {
                    LOG.error("Error occurred while executing request", e);
                    Platform.runLater(() ->
                        new Alert(AlertType.ERROR, "An error occurred processing request. Processing has stopped.\nCheck the log for more info.").show());
                }

                if (progressMonitor != null)
                {
                    progressMonitor.complete();
                }
            });
        }

        // Notify listeners
        for (Runnable r :  requestAddedListeners)
        {
            r.run();
        }
    }

    public void executeQueue()
    {
        context.makeContextCurrent();

        synchronized (requestList)
        {
            while (!requestList.isEmpty())
            {
                if (requestList.peek() != null)
                {
                    requestList.peek().run(); // Peek first to ensure that isEmpty() returns false when called from other threads.
                }

                requestList.poll();       // Once the task is done, remove the request from the queue.
            }
        }
    }
}
