package tetzlaff.ibr.gui2.controllers.scene.lights;//Created by alexk on 7/16/2017.

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.Arrays;

public class LightGroupSetting {
    private final static int L =4; //max number of lights
    private final StringProperty name = new SimpleStringProperty();
    private final BooleanProperty groupLocked = new SimpleBooleanProperty();
    private final ArrayList<LightSetting> lights = new ArrayList<>(L);



    public LightSetting getLight(int index){
        assert index < lights.size() : "try'd to get a light that doesn't exist yet!\n index:" + index
                + " nLights:" + lights.size() + " groopName:" + name.getValue();
        return lights.get(index);
    }

    public void addLight(int duplicateFromIndex){
        //If this is the first light added, we will create it with default settings
        //Otherwise, we will duplicate the previous light.
        //we will also prevent creating too many lights here.
        LightSetting newLight;
        if(lights.size() == 0){
            newLight = new LightSetting(
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    90.0,
                    1.0,
                    0.0,
                    false,
                    ("L1"),
                    LightType.PointLight,
                    groupLocked
            );
        }
        else if(lights.size() < L && duplicateFromIndex < lights.size()){
            newLight = lights.get(duplicateFromIndex).duplicate();
        }
        else if(lights.size() < L){
            newLight = lights.get(lights.size()-1).duplicate();
        }
        else return;//do nothing (since nLights=L)

        assert newLight != null;

        lights.add(newLight);
    }

    public void removeLight(int removeFromIndex){
        if(lights.size() == 0) return;
        else if (removeFromIndex < lights.size()){
           lights.remove(removeFromIndex);
        } else {
            lights.remove(lights.size()-1);
        }
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public boolean isGroupLocked() {
        return groupLocked.get();
    }

    public BooleanProperty groupLockedProperty() {
        return groupLocked;
    }

    public void setGroupLocked(boolean groupLocked) {
        this.groupLocked.set(groupLocked);
    }
}
