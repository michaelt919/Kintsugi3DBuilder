package tetzlaff.ibr.rendering;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.models.LightingModel;

public interface CameraBasedLightingModel extends LightingModel
{
    void overrideCameraPose(Matrix4 cameraPoseOverride);
    void removeCameraPoseOverride();
}
