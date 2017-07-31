package tetzlaff.ibr.app2;//Created by alexk on 7/19/2017.

import tetzlaff.ibr.rendering2.CameraModel3;
import tetzlaff.ibr.rendering2.EnvironmentMapModel3;
import tetzlaff.ibr.rendering2.LightModel3;
import tetzlaff.ibr.rendering2.ToolModel3;

public class RootModel {
    private final CameraModel3 cameraModel3;
    private final EnvironmentMapModel3 environmentMapModel3;
    private final LightModel3 lightModel3;
    private final ToolModel3 toolModel3;
    private final Error error = new Error("tried to distribute a model 3 times");
    private int gotCameraModel = 0;
    private int gotEnvironmentMapModel = 0;
    private int gotLightModel = 0;
    private int gotToolModel = 0;

    public RootModel(){
        cameraModel3 = new CameraModel3();
        toolModel3 = new ToolModel3();
        environmentMapModel3 = new EnvironmentMapModel3(toolModel3);
        lightModel3 = new LightModel3(environmentMapModel3);
    }

    public CameraModel3 getCameraModel3() {
        if(gotCameraModel++ > 2) throw error;
        return cameraModel3;
    }

    public LightModel3 getLightModel3() {
        if(gotLightModel++ > 2) throw error;
        return lightModel3;
    }

    public ToolModel3 getToolModel3() {
        if(gotToolModel++ > 2) throw error;
        return toolModel3;
    }

    public EnvironmentMapModel3 getEnvironmentMapModel3(){
        if(gotEnvironmentMapModel++ > 2) throw error;
        return environmentMapModel3;
    }
}
