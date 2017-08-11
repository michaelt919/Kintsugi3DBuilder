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
import tetzlaff.ibr.rendering2.IBRSettingsModel;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.ReadonlyLightModel;
import tetzlaff.mvc.models.ReadonlyObjectModel;
import tetzlaff.mvc.models.SceneViewportModel;

public interface IBRRenderable<ContextType extends Context<ContextType>> extends InteractiveRenderable<ContextType>
{
	void setOnLoadCallback(LoadingMonitor callback);

	ViewSet getActiveViewSet();
	VertexGeometry getActiveGeometry();
	
	SceneViewportModel getSceneViewportModel();
	
	IBRSettingsModel getSettingsModel();
	void setSettingsModel(IBRSettingsModel ibrSettingsModel);
	
	boolean getHalfResolution();
	boolean getMultisampling();

	void setHalfResolution(boolean halfResEnabled);
	void setMultisampling(boolean multisamplingEnabled);
	
	void setProgram(Program<ContextType> program);
	void reloadHelperShaders();

	void setBackplate(File backplateFile);
	void setEnvironment(File environmentFile);

	void setObjectModel(ReadonlyObjectModel objectModel);
	void setCameraModel(ReadonlyCameraModel cameraModel);
	void setLightModel(ReadonlyLightModel lightModel);
	
	void setMultiTransformationModel(List<Matrix4> multiTransformationModel);
	void setReferenceScene(VertexGeometry scene);
	
	IBRResources<ContextType> getResources();
	
	void draw(Framebuffer<ContextType> framebuffer, Matrix4 view, Matrix4 projection);
	
	@Override
	default void draw(Framebuffer<ContextType> framebuffer) 
	{
		draw(framebuffer, null, null);
	}
}
