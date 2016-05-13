package tetzlaff.imagebasedmicrofacet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.LightController;
import tetzlaff.gl.helpers.Matrix3;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;
import tetzlaff.ulf.ViewSet;
import tetzlaff.window.KeyCodes;
import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;
import tetzlaff.window.listeners.CharacterListener;
import tetzlaff.window.listeners.KeyPressListener;
import tetzlaff.window.listeners.KeyReleaseListener;

public class HardcodedLightController implements LightController
{
	private static float OBJECT_SCALE = 11.1f; // 11.1cm radius
	
	private Trackball cameraTrackball;
	Supplier<ViewSet<?>> viewSetSupplier;
	private Vector3[] lightColors;
	private Vector3[] lightPositions;
	
	public HardcodedLightController(Supplier<ViewSet<?>> viewSetSupplier)
	{		
		this.viewSetSupplier = viewSetSupplier;
		this.cameraTrackball = new Trackball(1.0f, 0, 1, true);
	}
	
	public CameraController asCameraController()
	{
		return () -> cameraTrackball.getViewMatrix()
						.times(new Matrix4(new Matrix3(viewSetSupplier.get().getCameraPose(0))));
	}
	
	public void addAsWindowListener(Window window)
	{
		cameraTrackball.addAsWindowListener(window);
	}

	@Override
	public int getLightCount() 
	{
		return 3;
	}

	@Override
	public Vector3 getLightColor(int i) 
	{
		Vector3 cameraDisplacement = new Vector3(viewSetSupplier.get().getCameraPose(0).times(new Vector4(0.0f, 0.0f, 0.0f, 1.0f)));
		float cameraDistSquared = 77.0f * 77.0f / (OBJECT_SCALE * OBJECT_SCALE);//cameraDisplacement.dot(cameraDisplacement);
		
		if (i == 0)
		{
			return new Vector3(1.0f).times(cameraDistSquared);
		}
		else if (i == 1)
		{
			return new Vector3((float)Math.pow(2.0, 1.2)).times(cameraDistSquared);
		}
		else if (i == 2)
		{
			return new Vector3((float)Math.sqrt(2)).times(cameraDistSquared);
		}
		else
		{
			return new Vector3(0.0f);
		}
	}

	@Override
	public Matrix4 getLightMatrix(int i) 
	{
		if (i == 0)
		{
			Vector4 lightOffset = 
				new Vector4(15.0f / OBJECT_SCALE, 0.0f / OBJECT_SCALE, 5.0f / OBJECT_SCALE, 1.0f);
				//new Vector4(viewSetSupplier.get().getLightPosition(0), 1.0f);
			
			// Only take the rotation of the default camera pose.
			return Matrix4.lookAt(
					new Vector3(this.asCameraController().getViewMatrix().quickInverse(0.001f).times(lightOffset))
						.normalized().times(77.0f / OBJECT_SCALE)
					, 
					new Vector3(0, 0, 0), 
					new Vector3(0, 1, 0));
		}
		else
		{
			Vector3 lightPosition = new Vector3(0.0f);
			
			if (i == 1)
			{
				lightPosition = new Vector3(-80.0f, 44.0f, -74.0f).minus(new Vector3(0.0f, 0.0f, 0.0f)).times(1.0f / OBJECT_SCALE);
				//lightPosition = new Vector3(-10.0f, 5.0f, -2.5f); 
			}
			else if (i == 2)
			{
				lightPosition = new Vector3(81.0f, 36.0f, 12.0f).minus(new Vector3(0.0f, 0.0f, 0.0f)).times(1.0f / OBJECT_SCALE);
				//lightPosition = new Vector3(7.7f, 2.3f, -3.5f);
			}
			
			Matrix3 projectedCameraRotation = Matrix3.scale(1.0f, 0.0f, 1.0f)
					.times(new Matrix3(cameraTrackball.getViewMatrix()));
			Vector3 projectedRotatedXAxis = projectedCameraRotation.times(new Vector3(1.0f, 0.0f, 0.0f)).normalized();
			Vector3 projectedRotatedZAxis = projectedCameraRotation.times(new Vector3(0.0f, 0.0f, 1.0f)).normalized();
			projectedRotatedZAxis = projectedRotatedZAxis.minus(projectedRotatedXAxis.times(projectedRotatedXAxis.dot(projectedRotatedZAxis))).normalized();
			
			Matrix3 turntableRotation = new Matrix3(
					projectedRotatedXAxis,
					new Vector3(0.0f, 1.0f, 0.0f), 
					projectedRotatedZAxis);
			
			return Matrix4.lookAt(lightPosition, new Vector3(0.0f), new Vector3(0.0f, 1.0f, 0.0f))
					.times(new Matrix4(turntableRotation))
					.times(new Matrix4(new Matrix3(viewSetSupplier.get().getCameraPose(0))));
		}
	}
}
