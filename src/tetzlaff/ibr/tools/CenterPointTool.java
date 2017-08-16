package tetzlaff.ibr.tools;//Created by alexk on 8/8/2017.

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ReadonlyEnvironmentMapModel;
import tetzlaff.models.ReadonlyLightingModel;
import tetzlaff.models.SceneViewportModel;

class CenterPointTool extends AbstractTool
{
    private ToolSelectionModel toolModel;
    
    public CenterPointTool(ExtendedCameraModel cameraModel, ReadonlyEnvironmentMapModel environmentMapModel, ReadonlyLightingModel lightingModel, 
            ToolSelectionModel toolModel, SceneViewportModel sceneViewportModel)
    {
        super(cameraModel, environmentMapModel, lightingModel, sceneViewportModel);
        this.toolModel = toolModel;
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) 
    {
        super.mouseButtonPressed(window, buttonIndex, mods);
        if(buttonIndex == MB1)
        {
            double trueX = mouseStartX_MB1 / window.getWindowSize().width;
            double trueY = mouseStartY_MB1 / window.getWindowSize().height;

            Vector3 newCenter = getPoint(trueX, trueY);
            SceneObjectType whatClicked = getClickedObjectType(trueX, trueY);
//            System.out.println("You clicked: " + whatClicked + " at " + newCenter);

            if(whatClicked.equals(SceneObjectType.OBJECT))
            {
                cameraModel.setCenter(newCenter);
                System.out.println("Set center to " + newCenter);

                toolModel.setTool(ToolType.ORBIT);
            }
        }
    }
}
