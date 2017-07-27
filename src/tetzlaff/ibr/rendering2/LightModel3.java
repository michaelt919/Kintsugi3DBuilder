package tetzlaff.ibr.rendering2;//Created by alexk on 7/25/2017.

import javafx.beans.value.ObservableValue;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.gui2.controllers.scene.lights.LightGroupSetting;
import tetzlaff.mvc.models.ControllableLightModel;

public class LightModel3 implements ControllableLightModel {

    private ObservableValue<LightGroupSetting> lightGroupSettingObservableValue;
    //private LightGroupSetting backup =

    @Override
    public int getLightCount() {
        return 0;
    }

    @Override
    public boolean isLightVisualizationEnabled(int i) {
        return false;
    }

    @Override
    public void setLightColor(int i, Vector3 color) {

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
