package tetzlaff.ibr.app2;//Created by alexk on 7/19/2017.

import tetzlaff.ibr.rendering2.CameraModelImp;
import tetzlaff.ibr.rendering2.EnvironmentMapModelImp;
import tetzlaff.ibr.rendering2.LightModelImp;
import tetzlaff.ibr.rendering2.ToolModelImp;

public class RootModel {
    private final CameraModelImp cameraModel;
    private final EnvironmentMapModelImp environmentMapModel;
    private final LightModelImp lightModel;
    private final ToolModelImp toolModel;
    private final Error error = new Error("tried to distribute a model 3 times");
    private int gotCameraModel = 0;
    private int gotEnvironmentMapModel = 0;
    private int gotLightModel = 0;
    private int gotToolModel = 0;

    public RootModel(){
        cameraModel = new CameraModelImp();
        toolModel = new ToolModelImp();
        environmentMapModel = new EnvironmentMapModelImp(toolModel);
        lightModel = new LightModelImp(environmentMapModel);
    }

    public CameraModelImp getCameraModel() {
        if(gotCameraModel++ >= 2){
            System.out.println("{camera model}");
            throw error;
        }
        return cameraModel;
    }

    public LightModelImp getLightModel() {
        if(gotLightModel++ >= 2){
            System.out.println("{light model}");
            throw error;
        }
        return lightModel;
    }

    public ToolModelImp getToolModel() {
        if(gotToolModel++ >= 2){
            System.out.println("{tool model}");
            throw error;
        }
        return toolModel;
    }

    public EnvironmentMapModelImp getEnvironmentMapModel(){
        if(gotEnvironmentMapModel++ >= 2){
            System.out.println("{environment model}");
            throw error;
        }
        return environmentMapModel;
    }
}
