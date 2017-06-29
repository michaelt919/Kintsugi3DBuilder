package tetzlaff.ibr.alexkautz_workspace.render.new_tool_setup_rename_this_later;

import tetzlaff.gl.vecmath.Vector3;

public class PointLightModelX extends CameraModelX {

    private Vector3 color = new Vector3(0.0f, 0.0f, 0.0f);

    public Vector3 getColor() {
        return color;
    }

    public void setColor(Vector3 color) {
        this.color = color;
    }
}
