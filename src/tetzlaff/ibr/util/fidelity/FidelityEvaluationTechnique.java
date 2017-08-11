package tetzlaff.ibr.util.fidelity;

import java.io.File;
import java.io.IOException;
import java.util.List;

import tetzlaff.gl.Context;
import tetzlaff.ibr.rendering.IBRResources;
import tetzlaff.ibr.rendering2.IBRSettingsModel;

public interface FidelityEvaluationTechnique<ContextType extends Context<ContextType>> extends AutoCloseable
{
	boolean isGuaranteedMonotonic();
	boolean isGuaranteedInterpolating();
	void initialize(IBRResources<ContextType> resources, IBRSettingsModel settings, int size) throws IOException;
	void setMask(File maskFile) throws IOException;
	void updateActiveViewIndexList(List<Integer> activeViewIndexList);
	double evaluateBaselineError(int targetViewIndex, File debugFile);
	double evaluateError(int targetViewIndex, File debugFile);
	
	default double evaluateBaselineError(int targetViewIndex)
	{
		return evaluateBaselineError(targetViewIndex, null);
	}

	default double evaluateError(int targetViewIndex)
	{
		return evaluateError(targetViewIndex, null);
	}
}
