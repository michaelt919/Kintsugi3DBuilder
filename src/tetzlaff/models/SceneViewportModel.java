package tetzlaff.models;

import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;

public interface SceneViewportModel
{
    Object getObjectAtCoordinates(double x, double y);
    Vector3 get3DPositionAtCoordinates(double x, double y);
    Vector3 getViewingDirection(double x, double y);
    Vector3 getViewportCenter();
    Vector2 projectPoint(Vector3 point);
}
