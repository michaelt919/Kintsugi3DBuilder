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
