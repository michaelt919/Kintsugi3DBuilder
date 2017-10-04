package tetzlaff.ibrelight.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.ExtendedCameraModel;

final class TwistTool implements DragTool
{
    private static final double TWIST_SENSITIVITY = Math.PI;
    private double twistSensitivityAdjusted;

    private float oldTwist;

    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;

    private static class Builder extends ToolBuilderBase<TwistTool>
    {
        @Override
        public TwistTool build()
        {
            return new TwistTool(getCameraModel());
        }
    }

    static ToolBuilder<TwistTool> getBuilder()
    {
        return new Builder();
    }

    private TwistTool(ExtendedCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, WindowSize windowSize)
    {
        this.mouseStart = cursorPosition;
        oldTwist = cameraModel.getTwist();
        twistSensitivityAdjusted = TWIST_SENSITIVITY / windowSize.width;
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, WindowSize windowSize)
    {
        cameraModel.setTwist(oldTwist + (float) Math.toDegrees((cursorPosition.x - this.mouseStart.x) * twistSensitivityAdjusted));
    }
}
