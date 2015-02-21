package tetzlaff.ulf;

import java.io.IOException;

import tetzlaff.gl.helpers.Drawable;

public interface ULFDrawable extends Drawable
{
	void setOnLoadCallback(ULFLoadingMonitor callback);
	
	float getGamma();
	float getWeightExponent();
	boolean isOcclusionEnabled();
	float getOcclusionBias();
	
	void setGamma(float gamma);
	void setWeightExponent(float weightExponent);
	void setOcclusionEnabled(boolean occlusionEnabled);
	void setOcclusionBias(float occlusionBias);
	
	void requestResample(int size, String targetVSETFile, String exportPath) throws IOException;
}
