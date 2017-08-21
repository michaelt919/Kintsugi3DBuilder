package tetzlaff.models;

import tetzlaff.gl.vecmath.Matrix4;

@FunctionalInterface
public interface ReadonlyObjectModel 
{
    Matrix4 getTransformationMatrix();
}
