package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.ExtendedCameraModel;

/*
not this work as intended at a zoom of 0.5
 */
final class PanTool implements DragTool
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
    public void mouseButtonPressed(CursorPosition cursorPosition, WindowSize windowSize)
    {
        this.mouseStart = cursorPosition;

        oldCenter = cameraModel.getTarget();
        orbit = cameraModel.getOrbit();

        panSensitivityAdjusted = PAN_SENSITIVITY / Math.min(windowSize.width, windowSize.height);
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, WindowSize windowSize)
    {
        Vector3 moveVector = new Vector3(
                (float) (cursorPosition.x - mouseStart.x),
                (float) (mouseStart.y - cursorPosition.y),
                0.0f);

        moveVector = moveVector.times((float) panSensitivityAdjusted);
        Vector3 worldMoveVector = orbit.transpose().times(moveVector.asVector4(0f)).getXYZ();
        cameraModel.setTarget(oldCenter.minus(worldMoveVector));
    }
}
