package tetzlaff.models;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

@FunctionalInterface
public interface ReadonlyCameraModel 
{
    Matrix4 getLookMatrix();

    default Vector3 getTarget()
    {
        return Vector3.ZERO;
    }
    default float getHorizontalFOV() { return (float)Math.PI / 2; }
}
