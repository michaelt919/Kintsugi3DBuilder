package tetzlaff.mvc.models;

import tetzlaff.gl.vecmath.Matrix4;

public interface OverrideableLightModel extends LightModel
{
	void overrideCameraPose(Matrix4 cameraPoseOverride);
	void removeCameraPoseOverride();
}
