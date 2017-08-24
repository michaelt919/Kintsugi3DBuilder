package tetzlaff.ibr.core;

import java.util.LinkedList;
import java.util.Queue;

import tetzlaff.gl.Context;

public class IBRRequestQueue<ContextType extends Context<ContextType>> 
{
    private final Queue<IBRRequest> requestList;
    private final ContextType context;
    private final IBRRenderableListModel<ContextType> model;
    private LoadingMonitor loadingMonitor;

    public IBRRequestQueue(ContextType context, IBRRenderableListModel<ContextType> model)
    {
        this.requestList = new LinkedList<IBRRequest>();
        this.context = context;
        this.model = model;
    }

    public void setLoadingMonitor(LoadingMonitor loadingMonitor)
    {
        this.loadingMonitor = loadingMonitor;
    }

    public void addRequest(IBRRequest request)
    {
        this.requestList.add(request);
    }

    public void executeQueue()
    {
        context.makeContextCurrent();

        while(!requestList.isEmpty())
        {
            if (loadingMonitor != null)
            {
                loadingMonitor.startLoading();
            }

            try
            {
                requestList.poll().executeRequest(context, model.getSelectedItem(), loadingMonitor);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            if (loadingMonitor != null)
            {
                loadingMonitor.loadingComplete();
            }
        }
    }
}
