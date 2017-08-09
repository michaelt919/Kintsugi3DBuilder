package tetzlaff.ibr.rendering2.tools2;//Created by alexk on 8/8/2017.

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.ibr.rendering2.ToolModel3;
import tetzlaff.mvc.models.ControllableCameraModel;
import tetzlaff.mvc.models.ControllableEnvironmentMapModel;
import tetzlaff.mvc.models.ControllableLightModel;
import tetzlaff.mvc.models.ControllableToolModel;

public class CenterPointTool extends AbstractTool{
    private ControllableToolModel toolModel;
    public CenterPointTool(ControllableCameraModel cameraModel, ControllableEnvironmentMapModel environmentMapModel, ControllableLightModel lightModel, ControllableToolModel toolModel) {
        super(cameraModel, environmentMapModel, lightModel);
        this.toolModel = toolModel;
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) {
        super.mouseButtonPressed(window, buttonIndex, mods);
        if(buttonIndex == MB1){
            Vector3 newCenter = toolModel.getPoint((float) mouseStartX_MB1,(float) mouseStartY_MB1);
            System.out.println("You clicked: " + newCenter);
        }
    }
}
