package tetzlaff.ibrelight.core;

import java.util.LinkedList;
import java.util.Queue;

import tetzlaff.gl.Context;

public class IBRRequestQueue<ContextType extends Context<ContextType>> 
{
    private final Queue<Runnable> requestList;
    private final IBRRenderableListModel<ContextType> model;
    private LoadingMonitor loadingMonitor;

    public IBRRequestQueue(IBRRenderableListModel<ContextType> model)
    {
        this.requestList = new LinkedList<>();
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
        if (model.getSelectedItem() != null)
        {
            model.getSelectedItem().getResources().context.makeContextCurrent();

            while (!requestList.isEmpty())
            {
                if (loadingMonitor != null)
                {
                    loadingMonitor.startLoading();
                }

                requestList.poll().run();

                if (loadingMonitor != null)
                {
                    loadingMonitor.loadingComplete();
                }
            }
        }
    }
}
