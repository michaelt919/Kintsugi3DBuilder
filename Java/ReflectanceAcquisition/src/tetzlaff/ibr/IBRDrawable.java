package tetzlaff.ibr;

import java.io.File;
import java.io.IOException;
import java.util.List;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.VertexMesh;

public interface IBRDrawable<ContextType extends Context<ContextType>> extends Drawable
{
	void setOnLoadCallback(IBRLoadingMonitor callback);

	ViewSet<ContextType> getActiveViewSet();
	VertexMesh getActiveProxy();
	
	IBRSettings settings();
	
	boolean getHalfResolution();
	boolean getMultisampling();

	void setHalfResolution(boolean halfResEnabled);
	void setMultisampling(boolean multisamplingEnabled);
	
	void setProgram(Program<ContextType> program);
	void reloadHelperShaders();

	Texture2D<ContextType> getEnvironmentTexture();
	void setEnvironment(File environmentFile) throws IOException;
	
	void requestResample(int width, int height, File targetVSETFile, File exportPath) throws IOException;
	void requestFidelity(File exportPath, File targetVSETFile) throws IOException;
	void requestBTF(int width, int height, File exportPath) throws IOException;

	void setTransformationMatrices(List<Matrix4> matrices);

	VertexMesh getReferenceScene();
	void setReferenceScene(VertexMesh scene);
}
