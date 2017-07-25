package tetzlaff.ibr.rendering2;//Created by alexk on 7/25/2017.

import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.gui2.controllers.scene.lights.LightGroupSetting;
import tetzlaff.ibr.gui2.controllers.scene.lights.SubLightSetting;
import tetzlaff.mvc.models.ControllableLightModel;

import javax.swing.plaf.PanelUI;

public class LightModel3 implements ControllableLightModel {

    private ObservableValue<LightGroupSetting> lightGroupSettingObservableValue;
    private LightGroupSetting backup = new LightGroupSetting("backup");

    private SubLightModel3[] subLightModel3s = new SubLightModel3[LightGroupSetting.LIGHT_LIMIT];

    public LightModel3() {
        for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++) {
            subLightModel3s[i] = new SubLightModel3();

            subLightModel3s[i].setSubLightSettingObservableValue(backup.lightListProperty().valueAt(i));

        }
    }

    public void setLightGroupSettingObservableValue(ObservableValue<LightGroupSetting> lightGroupSettingObservableValue){
        this.lightGroupSettingObservableValue = lightGroupSettingObservableValue;

        this.lightGroupSettingObservableValue.addListener((observable, oldValue, newValue) -> {
            if(newValue != null){
                for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++) {
                    subLightModel3s[i].setSubLightSettingObservableValue(newValue.lightListProperty().valueAt(i));
                }
            } else {
                for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++) {
                    subLightModel3s[i].setSubLightSettingObservableValue(backup.lightListProperty().valueAt(i));
                }
            }
        });
    }

    private LightGroupSetting lightGroup(){
        if (lightGroupSettingObservableValue != null || lightGroupSettingObservableValue.getValue() == null) {
            return backup;
        }else {
            return lightGroupSettingObservableValue.getValue();
        }
    }

    private SubLightModel3 lightModel(int index){
        return subLightModel3s[index];
    }

    @Override
    public int getLightCount() {
        return lightGroup().getNLights();
    }

    @Override
    public boolean isLightVisualizationEnabled(int i) {
        return true;
    }

    @Override
    public void setLightColor(int i, Vector3 color) {
        lightModel(i).setColor(color);
    }

    @Override
    public Vector3 getLightColor(int i) {
        return lightModel(i).getColor();
    }

    @Override
    public Vector3 getAmbientLightColor() {
        return new Vector3(0.0f, 0.0f, 0.0f);
    }

    @Override
    public boolean getEnvironmentMappingEnabled() {
        return false;
    }

    @Override
    public Matrix4 getLightMatrix(int i) {
        return lightModel(i).getLookMatrix();
    }
}
