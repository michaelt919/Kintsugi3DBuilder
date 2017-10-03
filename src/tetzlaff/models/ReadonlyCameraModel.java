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

    default float getHorizontalFOV()
    {
        return (float)(360 / Math.PI /* convert and multiply by 2) */ * Math.atan(0.36 /* "35 mm" film (actual 36mm horizontal), 50mm lens */));
    }

    default boolean isOrthographic()
    {
        return false;
    }
}
