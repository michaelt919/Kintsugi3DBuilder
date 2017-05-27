package tetzlaff.ibr.util;

import tetzlaff.gl.Context;
import tetzlaff.ibr.IBRLoadingMonitor;
import tetzlaff.ibr.rendering.IBRImplementation;

public interface IBRRequest 
{
	<ContextType extends Context<ContextType>> void executeRequest(ContextType context, IBRImplementation<ContextType> renderer, IBRLoadingMonitor callback) throws Exception;
}
