package tetzlaff.gl.helpers;


public interface OverrideableLightController extends LightController
{
	void overrideCameraPose(Matrix4 cameraPoseOverride);
	void removeCameraPoseOverride();
}
