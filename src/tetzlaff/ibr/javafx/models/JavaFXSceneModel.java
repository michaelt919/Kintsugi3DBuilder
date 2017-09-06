package tetzlaff.ibr.javafx.models;

import java.util.ArrayList;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.collections.ObservableList;
import tetzlaff.ibr.javafx.controllers.scene.camera.CameraSetting;
import tetzlaff.ibr.javafx.controllers.scene.environment_map.EnvironmentSetting;
import tetzlaff.ibr.javafx.controllers.scene.lights.LightGroupSetting;
import tetzlaff.ibr.javafx.controllers.scene.object.ObjectPoseSetting;

public class JavaFXSceneModel
{
    private final ObservableList<CameraSetting> cameraList = new ObservableListWrapper<>(new ArrayList<>(16));
    private final ObservableList<EnvironmentSetting> environmentList = new ObservableListWrapper<>(new ArrayList<>(16));
    private final ObservableList<LightGroupSetting> lightGroupList = new ObservableListWrapper<>(new ArrayList<>(16));
    private final ObservableList<ObjectPoseSetting> objectPoseList = new ObservableListWrapper<>(new ArrayList<>(16));

    public ObservableList<CameraSetting> getCameraList()
    {
        return this.cameraList;
    }

    public ObservableList<EnvironmentSetting> getEnvironmentList()
    {
        return this.environmentList;
    }

    public ObservableList<LightGroupSetting> getLightGroupList()
    {
        return this.lightGroupList;
    }

    public ObservableList<ObjectPoseSetting> getObjectPoseList()
    {
        return this.objectPoseList;
    }
}
