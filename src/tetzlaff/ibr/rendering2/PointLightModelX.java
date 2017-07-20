package tetzlaff.ibr.rendering2;

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.rendering2.CameraModelX;

public class PointLightModelX extends CameraModelX {

    private Vector3 color = new Vector3(0.0f, 0.0f, 0.0f);

    public Vector3 getColor() {
        return color;
    }

    public void setColor(Vector3 color) {
        this.color = color;
    }
}
