package tetzlaff.mvc.models;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Vector3;

public interface ControllableLightModel extends ReadonlyLightModel {
    public void setLightColor(int i, Vector3 color);
}
