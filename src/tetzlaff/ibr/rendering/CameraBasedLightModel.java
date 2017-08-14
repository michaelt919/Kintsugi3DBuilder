package tetzlaff.ibr.rendering;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.mvc.models.LightingModel;

public interface CameraBasedLightModel extends LightingModel
{
	public void overrideCameraPose(Matrix4 cameraPoseOverride);
	public void removeCameraPoseOverride();
}
