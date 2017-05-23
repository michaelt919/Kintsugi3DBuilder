package tetzlaff.ibr.rendering;

import java.util.function.Supplier;

import tetzlaff.gl.vecmath.Matrix3;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibr.ViewSet;
import tetzlaff.mvc.controllers.LightController;
import tetzlaff.mvc.controllers.TrackballController;
import tetzlaff.mvc.models.BasicCameraModel;
import tetzlaff.mvc.models.OverrideableLightModel;
import tetzlaff.util.VertexGeometry;
import tetzlaff.window.Window;

public class HardcodedLightController implements LightController
{
	private TrackballController cameraTrackball;

	public HardcodedLightController(Supplier<ViewSet> viewSetSupplier, Supplier<VertexGeometry> proxySupplier)
	{		
		this.viewSetSupplier = viewSetSupplier;
		this.proxySupplier = proxySupplier;
		this.cameraTrackball = new TrackballController(1.0f, 0, 1, true);
	}
	
	public BasicCameraModel asCameraController()
	{
		return () -> cameraTrackball.getLookMatrix()
			;//	.times(new Matrix4(new Matrix3(viewSetSupplier.get().getCameraPose(0))));
	}
	
	public void addAsWindowListener(Window window)
	{
		cameraTrackball.addAsWindowListener(window);
	}
}
