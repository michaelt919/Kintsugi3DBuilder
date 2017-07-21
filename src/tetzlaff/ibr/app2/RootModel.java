package tetzlaff.ibr.app2;//Created by alexk on 7/19/2017.

import tetzlaff.ibr.rendering2.CameraModel3;
import tetzlaff.ibr.rendering2.tools.ToolModel2;
import tetzlaff.ibr.rendering2.CameraModel2;
import tetzlaff.ibr.rendering2.LightModel2;

public class RootModel {
    private final CameraModel3 cameraModel3;
    private final LightModel2 lightModel2;
    private final ToolModel2 toolModel2;
    private final Error error = new Error("tried to distribute a model 3 times");
    private int gotCameraModel = 0;
    private int gotLightModel = 0;
    private int gotToolModel = 0;

    public RootModel(CameraModel3 cameraModel3, LightModel2 lightModel2, ToolModel2 toolModel2){
        this.cameraModel3 = cameraModel3;
        this.lightModel2 = lightModel2;
        this.toolModel2 = toolModel2;
    }

    public CameraModel3 getCameraModel3() {
        if(gotCameraModel++ > 2) throw error;
        return cameraModel3;
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
