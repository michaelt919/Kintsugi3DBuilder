package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.*;
import tetzlaff.models.ExtendedCameraModel;

final class OrbitTool implements Tool
{
    private static final double ORBIT_SENSITIVITY = 1.0 * Math.PI; //todo: get from gui somehow
    private double orbitSensitivityAdjusted = 1.0;

    private Matrix4 oldOrbitMatrix;

    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;

    private static class Builder extends ToolBuilderBase<OrbitTool>
    {
        @Override
        public OrbitTool build()
        {
            return new OrbitTool(getCameraModel());
        }
    }

    static ToolBuilder<OrbitTool> getBuilder()
    {
        return new Builder();
    }

    private OrbitTool(ExtendedCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        if (buttonIndex == 0)
        {
            this.mouseStart = window.getCursorPosition();

            oldOrbitMatrix = cameraModel.getOrbit();
            WindowSize windowSize = window.getWindowSize();
            orbitSensitivityAdjusted = ORBIT_SENSITIVITY / Math.min(windowSize.width, windowSize.height);
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xPos, double yPos)
    {
        if (window.getMouseButtonState(0) == MouseButtonState.Pressed &&
            !Double.isNaN(mouseStart.x) && !Double.isNaN(mouseStart.y) &&
            (yPos != mouseStart.y || xPos != mouseStart.x))
        {
            Vector3 rotationVector = new Vector3(
                (float) (yPos - mouseStart.y),
                (float) (xPos - mouseStart.x),
                0.0f);

            cameraModel.setOrbit(
                Matrix4.rotateAxis(rotationVector.normalized(), rotationVector.length() * orbitSensitivityAdjusted)
                    .times(oldOrbitMatrix)
            );
        }
    }
}
