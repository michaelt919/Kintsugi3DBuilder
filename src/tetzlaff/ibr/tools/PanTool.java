package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.*;
import tetzlaff.models.ExtendedCameraModel;

/*
not this work as intended at a zoom of 0.5
 */
class PanTool implements Tool
{
    private static final double PAN_SENSITIVITY = 1.0;
    private double panSensitivityAdjusted = 1.0;

    private Vector3 oldCenter = Vector3.ZERO;
    private Matrix4 orbit;

    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;

    private static class Builder extends ToolBuilderBase<PanTool>
    {
        @Override
        public PanTool build()
        {
            return new PanTool(getCameraModel());
        }
    }

    static ToolBuilder<PanTool> getBuilder()
    {
        return new Builder();
    }

    private PanTool(ExtendedCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        if (buttonIndex == 0)
        {
            this.mouseStart = window.getCursorPosition();

            oldCenter = cameraModel.getCenter();
            orbit = cameraModel.getOrbit();

            WindowSize windowSize = window.getWindowSize();
            panSensitivityAdjusted = PAN_SENSITIVITY / Math.min(windowSize.width, windowSize.height);
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xPos, double yPos)
    {
        if (window.getMouseButtonState(0) == MouseButtonState.Pressed &&
            !Double.isNaN(mouseStart.x) &&
            !Double.isNaN(mouseStart.y) &&
            (xPos != mouseStart.x || yPos != mouseStart.y))
        {

            Vector3 moveVector = new Vector3(
                (float) (xPos - mouseStart.x),
                (float) (mouseStart.y - yPos),
                0.0f);

            moveVector = moveVector.times((float) panSensitivityAdjusted);
            Vector3 worldMoveVector = orbit.transpose().times(moveVector.asVector4(0f)).getXYZ();
            cameraModel.setCenter(oldCenter.minus(worldMoveVector));
        }
    }
}
