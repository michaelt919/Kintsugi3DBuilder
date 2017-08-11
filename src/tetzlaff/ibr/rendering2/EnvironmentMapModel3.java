package tetzlaff.ibr.rendering2;//Created by alexk on 7/28/2017.

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.gui2.controllers.scene.environment_map.EVSetting;
import tetzlaff.mvc.models.ControllableEnvironmentMapModel;

import java.io.File;

public class EnvironmentMapModel3 extends ControllableEnvironmentMapModel {
    public EnvironmentMapModel3(ToolModel3 tool) {
        super(tool);
    }

    private ObservableValue<EVSetting> selected;
    public void setSelected(ObservableValue<EVSetting> selected){
        this.selected = selected;
        this.selected.addListener(settingChange);
    }
    private boolean nn(){
        return (selected != null && selected.getValue() != null);
    }

    @Override
    public Vector3 getAmbientLightColor() {
        if(nn()){

            if(selected.getValue().isEvUseColor()){
                Color color = selected.getValue().getEvColor();
                return new Vector3((float) color.getRed(),(float) color.getBlue(),(float) color.getGreen()).times((float) selected.getValue().getEvColorIntensity());
            }{

                if(selected.getValue().isEvUseImage()){
                    return new Vector3(1f);
                }else {
                    return Vector3.ZERO;
                }



            }


        }
        else return Vector3.ZERO;
    }

    private final ChangeListener<File> evFileChange = (ob, o, n)->{
        if(n != null){
            loadEV(n);
            if(nn()) selected.getValue().setFirstEVLoaded(true);
        }
    };

    private final ChangeListener<EVSetting> settingChange = (ob, o, n)->{
        if (n != null) {
            n.evImageFileProperty().addListener(evFileChange);
            evFileChange.changed(null, null, n.getEvImageFile());
        }
        if (o != null) {
            o.evImageFileProperty().removeListener(evFileChange);
        }
    };

    @Override
    public boolean getEnvironmentMappingEnabled() {
        if(nn()){
            return selected.getValue().isEvUseImage();
        }else return false;
    }

    @Override
    public Matrix4 getEnvironmentMapMatrix() {
        if(nn()){
            double azmuth = selected.getValue().getEvRotation();
            return Matrix4.rotateY(Math.toRadians(azmuth));
        }else {
            return Matrix4.IDENTITY;
        }
    }
}
