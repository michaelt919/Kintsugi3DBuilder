package tetzlaff.ibr.rendering2.tools2;//Created by alexk on 7/24/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.MouseButtonState;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.mvc.models.ControllableCameraModel;
import tetzlaff.mvc.models.ControllableEnvironmentMapModel;
import tetzlaff.mvc.models.ControllableLightModel;

public class DollyTool extends AbstractTool {
    DollyTool(ControllableCameraModel cameraModel, ControllableEnvironmentMapModel environmentMapModel, ControllableLightModel lightModel) {
        super(cameraModel, environmentMapModel, lightModel);
    }

    private final double dollySensitivity = Math.PI;
    private double dollySensitivityAdjusted;

    private double oldLogZoom;
    private double oldTwist;

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) {
        super.mouseButtonPressed(window, buttonIndex, mods);
        if(buttonIndex == MB1){
            WindowSize windowSize = window.getWindowSize();
            oldTwist = cameraModel.getTwist();
            oldLogZoom = Math.log(cameraModel.getZoom());
            dollySensitivityAdjusted = dollySensitivity / Math.min(windowSize.width, windowSize.height);
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xpos, double ypos) {

        if(window.getMouseButtonState(MB1) == MouseButtonState.Pressed && xpos != mouseStartX_MB1 && ypos != mouseStartY_MB1){

            cameraModel.setTwist(
                    oldTwist + Math.toDegrees ((xpos - mouseStartX_MB1)*dollySensitivityAdjusted)
            );

            cameraModel.setZoom((float)(Math.exp(oldLogZoom + dollySensitivityAdjusted * (ypos - mouseStartY_MB1))));



        }

    }
}
