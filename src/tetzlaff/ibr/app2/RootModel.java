package tetzlaff.ibr.app2;//Created by alexk on 7/19/2017.

import tetzlaff.ibr.rendering2.CameraModel3;
import tetzlaff.ibr.rendering2.LightModel3;
import tetzlaff.ibr.rendering2.ToolModel3;

public class RootModel {
    private final CameraModel3 cameraModel3;
    private final LightModel3 lightModel2;
    private final ToolModel3 toolModel3;
    private final Error error = new Error("tried to distribute a model 3 times");
    private int gotCameraModel = 0;
    private int gotLightModel = 0;
    private int gotToolModel = 0;

    public RootModel(CameraModel3 cameraModel3, LightModel3 lightModel3, ToolModel3 toolModel3){
        this.cameraModel3 = cameraModel3;
        this.lightModel2 = lightModel3;
        this.toolModel3 = toolModel3;
    }

    public CameraModel3 getCameraModel3() {
        if(gotCameraModel++ > 2) throw error;
        return cameraModel3;
    }

    public LightModel3 getLightModel3() {
        if(gotLightModel++ > 2) throw error;
        return lightModel2;
    }

    public ToolModel3 getToolModel3() {
        if(gotToolModel++ > 2) throw error;
        return toolModel3;
    }
}
