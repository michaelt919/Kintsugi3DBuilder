package tetzlaff.ibr.alexkautz_workspace.render.new_tool_setup_rename_this_later;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.mvc.models.ReadonlyLightModel;

/**
 * Created by alexk on 6/27/2017.
 */
public class LightModelX implements ReadonlyLightModel {

    @Override
    public int getLightCount() {
        return 0;
    }

    @Override
    public boolean isLightVisualizationEnabled(int i) {
        return false;
    }

    @Override
    public Vector3 getLightColor(int i) {
        return null;
    }

    @Override
    public Vector3 getAmbientLightColor() {
        return null;
    }

    @Override
    public boolean getEnvironmentMappingEnabled() {
        return false;
    }

    @Override
    public Matrix4 getLightMatrix(int i) {
        return null;
    }
}
