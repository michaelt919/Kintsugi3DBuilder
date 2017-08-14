package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.MouseButtonState;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.mvc.models.ExtendedCameraModel;
import tetzlaff.mvc.models.SceneViewportModel;
import tetzlaff.mvc.models.impl.EnvironmentMapModelBase;
import tetzlaff.mvc.models.impl.LightingModelBase;

/*
not this work as intended at a zoom of 0.5
 */
class PanTool extends AbstractTool{
    PanTool(ExtendedCameraModel cameraModel, EnvironmentMapModelBase environmentMapModel, LightingModelBase lightingModel, SceneViewportModel sceneViewportModel) {
        super(cameraModel, environmentMapModel, lightingModel, sceneViewportModel);
    }
    private final double panSensitivity = 1.0;
    private double panSensitivityAdjusted = 1.0;


    private Vector3 oldCenter = Vector3.ZERO;
    private Matrix4 orbit;

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) {
        super.mouseButtonPressed(window, buttonIndex, mods);
        if(buttonIndex == MB1){
            oldCenter = cameraModel.getCenter();
            orbit = cameraModel.getOrbit();

            WindowSize windowSize = window.getWindowSize();
            panSensitivityAdjusted = panSensitivity / Math.min(windowSize.width, windowSize.height);
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xpos, double ypos) {
        if(window.getMouseButtonState(MB1).equals(MouseButtonState.Pressed) &&
                !Double.isNaN(mouseStartX_MB1) &&
                !Double.isNaN(mouseStartY_MB1) &&
                (xpos != mouseStartX_MB1 | ypos != mouseStartY_MB1)){

            Vector3 moveVector = new Vector3(
                                (float) (xpos - mouseStartX_MB1),
                                (float) (mouseStartY_MB1 - ypos),
                                0.0f
                        );

            moveVector = moveVector.times((float) panSensitivityAdjusted);

            Vector3 worldMoveVector = orbit.transpose().times(moveVector.asVector4(0f)).getXYZ();

            cameraModel.setCenter(oldCenter.minus(worldMoveVector));

        }
    }
}
