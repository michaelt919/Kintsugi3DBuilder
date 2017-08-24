package tetzlaff.ibr.core;

import tetzlaff.gl.Context;

public interface IBRRequest 
{
    <ContextType extends Context<ContextType>> void executeRequest(ContextType context, IBRRenderable<ContextType> renderable, LoadingMonitor callback) throws Exception;
}
