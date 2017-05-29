package tetzlaff.ibr;

import java.io.File;
import java.util.List;

import tetzlaff.gl.Context;
import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.Program;
import tetzlaff.gl.interactive.InteractiveRenderable;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.ibr.rendering.IBRResources;

public interface IBRRenderable<ContextType extends Context<ContextType>> extends InteractiveRenderable<ContextType>
{
	void setOnLoadCallback(LoadingMonitor callback);

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

	void setTransformationMatrices(List<Matrix4> matrices);
	void setReferenceScene(VertexGeometry scene);
	
	IBRResources<ContextType> getResources();
	
	void draw(Framebuffer<ContextType> framebuffer, Matrix4 view, Matrix4 projection);
	
	@Override
	default void draw(Framebuffer<ContextType> framebuffer) 
	{
		draw(framebuffer, null, null);
	}
}
