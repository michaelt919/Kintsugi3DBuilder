package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.MouseButtonState;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ReadonlyEnvironmentMapModel;
import tetzlaff.models.ReadonlyLightingModel;
import tetzlaff.models.SceneViewportModel;

class OrbitTool extends AbstractTool 
{
    private final double orbitSensitivity = 1.0 * Math.PI; //todo: get from gui somehow
    private double orbitSensitivityAdjusted = 1.0;

    private Matrix4 oldOrbitMatrix;

    OrbitTool(ExtendedCameraModel cameraModel, ReadonlyEnvironmentMapModel environmentMapModel, ReadonlyLightingModel lightingModel, SceneViewportModel sceneViewportModel) 
    {
        super(cameraModel, environmentMapModel, lightingModel, sceneViewportModel);
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) 
    {
        super.mouseButtonPressed(window, buttonIndex, mods);
        if(buttonIndex == MB1) 
        {
            oldOrbitMatrix = cameraModel.getOrbit();
            WindowSize windowSize = window.getWindowSize();
            orbitSensitivityAdjusted = orbitSensitivity / Math.min(windowSize.width, windowSize.height);
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xpos, double ypos) 
    {
        if(window.getMouseButtonState(MB1) == MouseButtonState.Pressed &&
            !Double.isNaN(mouseStartX_MB1) && !Double.isNaN(mouseStartY_MB1) &&
            (ypos != mouseStartY_MB1 || xpos != mouseStartX_MB1))
        {
                Vector3 rotationVector = new Vector3(
                        (float) (ypos - mouseStartY_MB1),
                        (float) (xpos - mouseStartX_MB1),
                        0.0f);

            cameraModel.setOrbit(
                Matrix4.rotateAxis(rotationVector.normalized(), rotationVector.length() * orbitSensitivityAdjusted)
                	.times(oldOrbitMatrix)
            );
        }
    }
}
