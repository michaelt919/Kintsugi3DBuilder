package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.Vector4;

public interface ULFDrawable extends Drawable
{
	void setOnLoadCallback(ULFLoadingMonitor callback);
	
	float getGamma();
	float getWeightExponent();
	boolean isOcclusionEnabled();
	float getOcclusionBias();
	boolean getHalfResolution();
	Vector4 getBackgroundColor();
	boolean isKNeighborsEnabled();
	int getKNeighborCount();
	
	void setGamma(float gamma);
	void setWeightExponent(float weightExponent);
	void setOcclusionEnabled(boolean occlusionEnabled);
	void setOcclusionBias(float occlusionBias);
	
	void setVisualizeCameras(boolean camerasEnabled);
	void setHalfResolution(boolean halfResEnabled);
	void setMultisampling(boolean multisamplingEnabled);
	void setBackgroundColor(Vector4 RGBA);
	void setKNeighborsEnabled(boolean kNeighborsEnabled);
	void setKNeighborCount(int kNeighborCount);
	
	void requestResample(int width, int height, File targetVSETFile, File exportPath) throws IOException;

}
