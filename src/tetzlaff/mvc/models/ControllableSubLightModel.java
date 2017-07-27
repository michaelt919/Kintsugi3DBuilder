package tetzlaff.mvc.models;//Created by alexk on 7/25/2017.

import tetzlaff.gl.vecmath.Vector3;

public interface ControllableSubLightModel extends ControllableCameraModel {
    public Vector3 getColor();
    public void setColor(Vector3 color);
}
