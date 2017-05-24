package tetzlaff.ibr.rendering;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.mvc.models.LightModel;

public interface CameraBasedLightModel extends LightModel
{
	public void overrideCameraPose(Matrix4 cameraPoseOverride);
	public void removeCameraPoseOverride();
}
