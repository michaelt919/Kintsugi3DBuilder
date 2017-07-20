package tetzlaff.ibr.app2;//Created by alexk on 7/19/2017.

import tetzlaff.ibr.rendering2.tools.ToolModel2;
import tetzlaff.ibr.rendering2.CameraModel2;
import tetzlaff.ibr.rendering2.LightModel2;

public class RootModel {
    private final CameraModel2 cameraModel2;
    private final LightModel2 lightModel2;
    private final ToolModel2 toolModel2;
    private final Error error = new Error("tried to distribute a model 3 times");
    private int gotCameraModel = 0;
    private int gotLightModel = 0;
    private int gotToolModel = 0;

    public RootModel(CameraModel2 cameraModel2, LightModel2 lightModel2, ToolModel2 toolModel2){
        this.cameraModel2 = cameraModel2;
        this.lightModel2 = lightModel2;
        this.toolModel2 = toolModel2;
    }

    public CameraModel2 getCameraModel2() {
        if(gotCameraModel++ > 2) throw error;
        return cameraModel2;
    }

    public LightModel2 getLightModel2() {
        if(gotLightModel++ > 2) throw error;
        return lightModel2;
    }

    public ToolModel2 getToolModel2() {
        if(gotToolModel++ > 2) throw error;
        return toolModel2;
    }
}
