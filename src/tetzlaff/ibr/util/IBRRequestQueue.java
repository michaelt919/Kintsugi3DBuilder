package tetzlaff.ibr.util;

import java.util.LinkedList;
import java.util.Queue;

import tetzlaff.gl.Context;
import tetzlaff.ibr.IBRRenderableListModel;
import tetzlaff.ibr.LoadingMonitor;

public class IBRRequestQueue<ContextType extends Context<ContextType>> 
{
    private Queue<IBRRequest> requestList;
    private ContextType context;
    private IBRRenderableListModel<ContextType> model;
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
