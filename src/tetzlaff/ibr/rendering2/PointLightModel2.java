package tetzlaff.ibr.rendering2;

import tetzlaff.gl.vecmath.Vector3;

public class PointLightModel2 extends CameraModel2 {

    private Vector3 color = new Vector3(0.0f, 0.0f, 0.0f);

    public Vector3 getColor() {
        return color;
    }

    public void setColor(Vector3 color) {
        this.color = color;
    }
}
