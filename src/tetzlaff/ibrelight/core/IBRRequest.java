package tetzlaff.ibrelight.core;

import tetzlaff.gl.Context;

public interface IBRRequest 
{
    <ContextType extends Context<ContextType>> void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback) throws Exception;
}
