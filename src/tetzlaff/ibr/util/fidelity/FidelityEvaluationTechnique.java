package tetzlaff.ibr.util.fidelity;

import tetzlaff.gl.Context;
import tetzlaff.ibr.IBRSettings;
import tetzlaff.ibr.rendering.IBRResources;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface FidelityEvaluationTechnique<ContextType extends Context<ContextType>> extends AutoCloseable
{
	boolean isGuaranteedMonotonic();
	void initialize(IBRResources<ContextType> resources, IBRSettings settings, int size) throws IOException;
	void updateActiveViewIndexList(List<Integer> viewIndexList);
	double evaluateError(int targetViewIndex, File debugFile);

	default double evaluateError(int targetViewIndex)
	{
		return evaluateError(targetViewIndex, null);
	}
}
