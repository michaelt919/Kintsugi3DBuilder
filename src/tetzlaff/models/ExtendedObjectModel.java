package tetzlaff.models;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface ExtendedObjectModel extends ObjectModel, ReadonlyExtendedObjectModel
{
    void setOrbit(Matrix4 orbit);
    void setCenter(Vector3 center);
    void setRotationZ(float rotationZ);
    void setRotationY(float rotationY);
    void setRotationX(float rotationX);
}
