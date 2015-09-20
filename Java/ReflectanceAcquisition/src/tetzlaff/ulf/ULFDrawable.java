package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.helpers.Drawable;

public interface ULFDrawable extends Drawable
{
	void setOnLoadCallback(ULFLoadingMonitor callback);
	
	float getGamma();
	float getWeightExponent();
	boolean isOcclusionEnabled();
	float getOcclusionBias();
	boolean getHalfResolution();
	
	void setGamma(float gamma);
	void setWeightExponent(float weightExponent);
	void setOcclusionEnabled(boolean occlusionEnabled);
	void setOcclusionBias(float occlusionBias);
	
	void setVisualizeCameras(boolean camerasEnabled);
	void setHalfResolution(boolean halfResEnabled);
	void setMultisampling(boolean multisamplingEnabled);
	
	void requestResample(int width, int height, File targetVSETFile, File exportPath) throws IOException;

}
