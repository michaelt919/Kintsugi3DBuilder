package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.helpers.Drawable;

public interface ULFDrawable<ContextType extends Context<ContextType>> extends Drawable
{
	void setOnLoadCallback(ULFLoadingMonitor callback);

	ViewSet<ContextType> getActiveViewSet();
	
	float getGamma();
	float getWeightExponent();
	boolean isOcclusionEnabled();
	float getOcclusionBias();
	boolean isViewIndexCacheEnabled();
	boolean getHalfResolution();
	boolean getMultisampling();
	
	void setGamma(float gamma);
	void setWeightExponent(float weightExponent);
	void setOcclusionEnabled(boolean occlusionEnabled);
	void setOcclusionBias(float occlusionBias);

	void setViewIndexCacheEnabled(boolean viewIndexCacheEnabled);
	void setHalfResolution(boolean halfResEnabled);
	void setMultisampling(boolean multisamplingEnabled);
	
	void setProgram(Program<ContextType> program);
	void setIndexProgram(Program<ContextType> program);
	
	void requestResample(int width, int height, File targetVSETFile, File exportPath) throws IOException;
}
