package tetzlaff.ibr.javafx.models;//Created by alexk on 7/25/2017.

import javafx.beans.value.ObservableValue;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.javafx.controllers.scene.lights.LightGroupSetting;
import tetzlaff.mvc.models.LightInstanceModel;
import tetzlaff.mvc.models.impl.LightingModelBase;

public class JavaFXLightingModel extends LightingModelBase {

    private ObservableValue<LightGroupSetting> lightGroupSettingObservableValue;
    private LightGroupSetting backup = new LightGroupSetting("backup");

    private JavaFXLightInstanceModel[] subLightModelImps = new JavaFXLightInstanceModel[LightGroupSetting.LIGHT_LIMIT];

    public JavaFXLightingModel(JavaFXEnvironmentMapModel ev) {
        super(ev);
        for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++) {
            subLightModelImps[i] = new JavaFXLightInstanceModel();

            subLightModelImps[i].setSubLightSettingObservableValue(backup.lightListProperty().valueAt(i));

        }
    }

    public void setLightGroupSettingObservableValue(ObservableValue<LightGroupSetting> lightGroupSettingObservableValue){
        this.lightGroupSettingObservableValue = lightGroupSettingObservableValue;

        this.lightGroupSettingObservableValue.addListener((observable, oldValue, newValue) -> {
            if(newValue != null){
                for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++) {
                    subLightModelImps[i].setSubLightSettingObservableValue(newValue.lightListProperty().valueAt(i));
                }
            } else {
                System.out.println("Binding Backup");
                for (int i = 0; i < LightGroupSetting.LIGHT_LIMIT; i++) {
                    subLightModelImps[i].setSubLightSettingObservableValue(backup.lightListProperty().valueAt(i));
                }
            }
        });
    }

    private LightGroupSetting lightGroup(){
        if (lightGroupSettingObservableValue == null || lightGroupSettingObservableValue.getValue() == null) {
//            System.out.println("Using LightGroup Backup");
            return backup;
        }else {
//            System.out.println("Need Value");
            return lightGroupSettingObservableValue.getValue();
        }
    }

    private JavaFXLightInstanceModel lightModel(int index){
        return subLightModelImps[index];
    }

    @Override
    public int getLightCount() {
        //        System.out.println("Counted " + count + "Lights");
//        return LightGroupSetting.LIGHT_LIMIT; //TODO ERROR HERE
//        System.out.println("Count: " + count);
        return lightGroup().getNLights();
    }

    @Override
    public boolean isLightVisualizationEnabled(int i) {
        return true;
    }
	
	@Override
	public boolean isLightWidgetEnabled(int i) 
	{
		return true;
	}

    @Override
    public void setLightColor(int i, Vector3 color) {
        lightModel(i).setColor(color);
    }

    @Override
    public Vector3 getLightColor(int i) {
//        System.out.println("Get Color ");
        return lightModel(i).getColor();
    }




    @Override
    public Matrix4 getLightMatrix(int i) {
//        System.out.println("get light matrix " + i);

        Matrix4 out = lightModel(i).getLookMatrix();

//        for (int j = 0; j < 4; j++) {
//            System.out.print("[");
//            for (int k = 0; k < 4; k++) {
//                System.out.print("\t" + out.get(j,k));
//            }
//            System.out.print("]\n");
//        }

        return out;
    }


    @Override
    public LightInstanceModel getLightInstanceModel(int i) {
        return subLightModelImps[i];
    }

	@Override
	public Vector3 getLightCenter(int i) 
	{
	    return getLightInstanceModel(i).getCenter();
	}
}
