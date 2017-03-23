package tetzlaff.ibr;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.VertexMesh;

public interface IBRDrawable<ContextType extends Context<ContextType>> extends Drawable
{
	void setOnLoadCallback(IBRLoadingMonitor callback);

	ViewSet<ContextType> getActiveViewSet();
	VertexMesh getActiveProxy();
	
	float getGamma();
	float getWeightExponent();
	boolean isOcclusionEnabled();
	float getOcclusionBias();
	
	boolean isIBREnabled();
	boolean isRelightingEnabled();
	boolean areTexturesEnabled();
	boolean isPBRGeometricAttenuationEnabled();
	boolean isFresnelEnabled();
	
	boolean isViewIndexCacheEnabled();
	boolean getHalfResolution();
	boolean getMultisampling();
	
	void setGamma(float gamma);
	void setWeightExponent(float weightExponent);
	void setOcclusionEnabled(boolean occlusionEnabled);
	void setOcclusionBias(float occlusionBias);
	
	void setIBREnabled(boolean ibrEnabled);
	void setRelightingEnabled(boolean relightingEnabled);
	void setTexturesEnabled(boolean texturesEnabled);
	void setPBRGeometricAttenuationEnabled(boolean pbrGeometricAttenuationEnabled);
	void setFresnelEnabled(boolean fresnelEnabled);

	void setViewIndexCacheEnabled(boolean viewIndexCacheEnabled);
	void setHalfResolution(boolean halfResEnabled);
	void setMultisampling(boolean multisamplingEnabled);
	
	void setProgram(Program<ContextType> program);
	void setIndexProgram(Program<ContextType> program);
	void reloadHelperShaders();

	Texture2D<ContextType> getEnvironmentTexture();
	void setEnvironment(File environmentFile) throws IOException;
	
	void requestResample(int width, int height, File targetVSETFile, File exportPath) throws IOException;
	void requestFidelity(File exportPath) throws IOException;
	void requestBTF(int width, int height, File exportPath) throws IOException;

}
