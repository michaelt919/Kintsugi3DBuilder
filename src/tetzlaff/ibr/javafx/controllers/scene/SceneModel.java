package tetzlaff.ibr.javafx.controllers.scene;

import java.util.ArrayList;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.collections.ObservableList;
import tetzlaff.ibr.javafx.controllers.scene.camera.CameraSetting;
import tetzlaff.ibr.javafx.controllers.scene.environment.EnvironmentSetting;
import tetzlaff.ibr.javafx.controllers.scene.lights.LightGroupSetting;
import tetzlaff.ibr.javafx.controllers.scene.object.ObjectPoseSetting;

public class SceneModel
{
    private final ObservableList<CameraSetting> cameraList = new ObservableListWrapper<>(new ArrayList<>(16));
    private final ObservableList<EnvironmentSetting> environmentList = new ObservableListWrapper<>(new ArrayList<>(16));
    private final ObservableList<LightGroupSetting> lightGroupList = new ObservableListWrapper<>(new ArrayList<>(16));
    private final ObservableList<ObjectPoseSetting> objectPoseList = new ObservableListWrapper<>(new ArrayList<>(16));

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<CameraSetting> getCameraList()
    {
        return this.cameraList;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<EnvironmentSetting> getEnvironmentList()
    {
        return this.environmentList;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<LightGroupSetting> getLightGroupList()
    {
        return this.lightGroupList;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<ObjectPoseSetting> getObjectPoseList()
    {
        return this.objectPoseList;
    }
}
