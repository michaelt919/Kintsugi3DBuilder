/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.core;

import java.util.LinkedList;
import java.util.Queue;

import tetzlaff.gl.core.Context;

public class IBRRequestQueue<ContextType extends Context<ContextType>> 
{
    private final Queue<Runnable> requestList;
    private IBRRenderableListModel<ContextType> model;
    private LoadingMonitor loadingMonitor;

    public IBRRequestQueue()
    {
        this.requestList = new LinkedList<>();
    }

    public boolean isEmpty()
    {
        return requestList.isEmpty();
    }

    public void setModel(IBRRenderableListModel<ContextType> model)
    {
        this.model = model;
    }

    public void setLoadingMonitor(LoadingMonitor loadingMonitor)
    {
        this.loadingMonitor = loadingMonitor;
    }

    public void addRequest(Runnable request)
    {
        this.requestList.add(request);
    }

    public void addRequest(IBRRequest request)
    {
        this.requestList.add(() ->
        {
            try
            {
                request.executeRequest(model.getSelectedItem(), loadingMonitor);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    public void executeQueue()
    {
        if (model != null && model.getSelectedItem() != null)
        {
            model.getSelectedItem().getResources().context.makeContextCurrent();

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
