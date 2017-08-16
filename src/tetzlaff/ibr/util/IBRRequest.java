package tetzlaff.ibr.util;

import tetzlaff.gl.Context;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.LoadingMonitor;

public interface IBRRequest 
{
    <ContextType extends Context<ContextType>> void executeRequest(ContextType context, IBRRenderable<ContextType> renderable, LoadingMonitor callback) throws Exception;
}
