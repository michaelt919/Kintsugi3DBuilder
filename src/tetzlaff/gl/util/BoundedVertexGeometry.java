package tetzlaff.gl.util;

import tetzlaff.gl.vecmath.Vector3;

public interface BoundedVertexGeometry extends VertexGeometry
{
    Vector3 getCentroid();
    float getBoundingRadius();
    Vector3 getBoundingBoxCenter();
    Vector3 getBoundingBoxSize();
}
