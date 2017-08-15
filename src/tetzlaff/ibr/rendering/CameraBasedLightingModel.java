package tetzlaff.ibr.rendering;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.models.LightingModel;

public interface CameraBasedLightingModel extends LightingModel
{
	public void overrideCameraPose(Matrix4 cameraPoseOverride);
	public void removeCameraPoseOverride();
}
