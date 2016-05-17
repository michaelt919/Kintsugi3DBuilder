package tetzlaff.imagebasedmicrofacet;

import tetzlaff.gl.helpers.LightController;
import tetzlaff.gl.helpers.Matrix4;

public interface OverrideableLightController extends LightController
{
	void overrideCameraPose(Matrix4 cameraPoseOverride);
	void removeCameraPoseOverride();
}
