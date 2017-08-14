package tetzlaff.ibr.rendering2.tools2;//Created by alexk on 8/8/2017.

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.ibr.ControllableToolModel;
import tetzlaff.mvc.models.ExtendedCameraModel;
import tetzlaff.mvc.models.impl.LightingModelBase;
import tetzlaff.mvc.models.impl.EnvironmentMapModelBase;

class CenterPointTool extends AbstractTool{
    private ControllableToolModel toolModel;
    public CenterPointTool(ExtendedCameraModel cameraModel, EnvironmentMapModelBase environmentMapModel, LightingModelBase lightModel, ControllableToolModel toolModel) {
        super(cameraModel, environmentMapModel, lightModel);
        this.toolModel = toolModel;
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) {
        super.mouseButtonPressed(window, buttonIndex, mods);
        if(buttonIndex == MB1){

            double trueX = mouseStartX_MB1 / window.getWindowSize().width;
            double trueY = mouseStartY_MB1 / window.getWindowSize().height;

            Vector3 newCenter = toolModel.getPoint(trueX, trueY);
            ControllableToolModel.SceneObjectType whatClicked = toolModel.getClickedObjectType(trueX, trueY);
//            System.out.println("You clicked: " + whatClicked + " at " + newCenter);

            if(whatClicked.equals(ControllableToolModel.SceneObjectType.OBJECT)){
                cameraModel.setCenter(newCenter);
                System.out.println("Set center to " + newCenter);

                toolModel.setTool(ToolBox.TOOL.ORBIT);
            }
        }
    }
}
