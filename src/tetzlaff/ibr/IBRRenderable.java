package tetzlaff.ibr;

import java.io.File;
import java.util.List;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.interactive.InteractiveRenderable;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.Matrix4;

public interface IBRRenderable<ContextType extends Context<ContextType>> extends InteractiveRenderable
{
	void setOnLoadCallback(IBRLoadingMonitor callback);

	ViewSet getActiveViewSet();
	VertexGeometry getActiveProxy();
	
	IBRSettings settings();
	
	boolean getHalfResolution();
	boolean getMultisampling();

	void setHalfResolution(boolean halfResEnabled);
	void setMultisampling(boolean multisamplingEnabled);
	
	void setProgram(Program<ContextType> program);
	void reloadHelperShaders();

	void setEnvironment(File environmentFile);
	
	void requestResample(int width, int height, File targetVSETFile, File exportPath);
	void requestFidelity(File exportPath, File targetVSETFile);
	void requestBTF(int width, int height, File exportPath);

	void setTransformationMatrices(List<Matrix4> matrices);
	void setReferenceScene(VertexGeometry scene);
}
