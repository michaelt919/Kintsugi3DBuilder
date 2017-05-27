package tetzlaff.ibr.util;

import java.util.LinkedList;
import java.util.Queue;

import tetzlaff.gl.Context;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.rendering.IBRImplementation;

public class IBRRequestQueue<ContextType extends Context<ContextType>> 
{
	private Queue<IBRRequest> requestList;
	private IBRRenderable<ContextType> renderable;
	
	public IBRRequestQueue(IBRRenderable<ContextType> renderable, )
	{
		requestList = new LinkedList<IBRRequest>();
	}
	
	public void addRequest(IBRRequest request)
	{
		this.requestList.add(request);
	}
	
	public void executeQueue()
	{
		while(!requestList.isEmpty())
		{
			try
			{
				requestList.poll().executeRequest(renderable.getContext(), renderable);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
