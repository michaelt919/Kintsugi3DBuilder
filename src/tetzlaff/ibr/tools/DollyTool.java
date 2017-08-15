package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.MouseButtonState;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ReadonlyEnvironmentMapModel;
import tetzlaff.models.ReadonlyLightingModel;
import tetzlaff.models.SceneViewportModel;

class DollyTool extends AbstractTool 
{
    private final double dollySensitivity = Math.PI;
    private double dollySensitivityAdjusted;

    private float oldLog10Distance;
    private float oldTwist;
    
    DollyTool(ExtendedCameraModel cameraModel, ReadonlyEnvironmentMapModel environmentMapModel, ReadonlyLightingModel lightingModel, SceneViewportModel sceneViewportModel) 
    {
        super(cameraModel, environmentMapModel, lightingModel, sceneViewportModel);
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) 
    {
        super.mouseButtonPressed(window, buttonIndex, mods);
        if(buttonIndex == MB1)
        {
            WindowSize windowSize = window.getWindowSize();
            oldTwist = cameraModel.getTwist();
            oldLog10Distance = cameraModel.getLog10Distance();
            dollySensitivityAdjusted = dollySensitivity / Math.min(windowSize.width, windowSize.height);
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xpos, double ypos)
    {
        if(window.getMouseButtonState(MB1) == MouseButtonState.Pressed && xpos != mouseStartX_MB1 && ypos != mouseStartY_MB1)
        {
            cameraModel.setTwist(oldTwist + (float)Math.toDegrees ((xpos - mouseStartX_MB1)*dollySensitivityAdjusted));
            cameraModel.setLog10Distance((float) (oldLog10Distance + 0.5* dollySensitivityAdjusted*(mouseStartY_MB1 - ypos)));
        }

    }
}
