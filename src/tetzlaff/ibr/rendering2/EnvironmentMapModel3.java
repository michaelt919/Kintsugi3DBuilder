package tetzlaff.ibr.rendering2;//Created by alexk on 7/28/2017.

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.mvc.models.ControllableEnvironmentMapModel;

public class EnvironmentMapModel3 extends ControllableEnvironmentMapModel {
    public EnvironmentMapModel3(ToolModel3 tool) {
        super(tool);
    }
    @Override
    public Vector3 getAmbientLightColor() {
        return new Vector3(0.0f,0.0f,0.0f);
    }
}
