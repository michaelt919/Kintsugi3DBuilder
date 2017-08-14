package tetzlaff.ibr.rendering2.tools2;//Created by alexk on 8/8/2017.

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.ibr.ControllableToolModel;
import tetzlaff.mvc.models.ExtendedCameraModel;
import tetzlaff.mvc.models.SceneViewportModel;
import tetzlaff.mvc.models.impl.EnvironmentMapModelBase;
import tetzlaff.mvc.models.impl.LightingModelBase;

class CenterPointTool extends AbstractTool{
    private ControllableToolModel toolModel;
    public CenterPointTool(ExtendedCameraModel cameraModel, EnvironmentMapModelBase environmentMapModel, LightingModelBase lightModel, 
    		ControllableToolModel toolModel, SceneViewportModel sceneViewportModel) 
    {
        super(cameraModel, environmentMapModel, lightModel, sceneViewportModel);
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

                toolModel.setTool(ToolBox.ToolType.ORBIT);
            }
        }
    }
}
