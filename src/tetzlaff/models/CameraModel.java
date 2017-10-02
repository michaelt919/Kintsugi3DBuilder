package tetzlaff.models;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface CameraModel extends ReadonlyCameraModel
{
    void setLookMatrix(Matrix4 lookMatrix);
    void setTarget(Vector3 target);
    void setHorizontalFOV(float fov);
    void setOrthographic(boolean orthographic);
}
