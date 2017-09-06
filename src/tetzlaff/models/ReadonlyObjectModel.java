package tetzlaff.models;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

@FunctionalInterface
public interface ReadonlyObjectModel 
{
    Matrix4 getTransformationMatrix();

    default Vector3 getCenter()
    {
        return Vector3.ZERO;
    }
}
