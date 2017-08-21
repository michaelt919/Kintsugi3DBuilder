package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.window.*;
import tetzlaff.models.ExtendedCameraModel;

final class DollyTool implements Tool
{
    private static final double DOLLY_SENSITIVITY = Math.PI;
    private double dollySensitivityAdjusted;

    private float oldLog10Distance;
    private float oldTwist;

    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;

    private static class Builder extends ToolBuilderBase<DollyTool>
    {
        @Override
        public DollyTool build()
        {
            return new DollyTool(getCameraModel());
        }
    }

    static ToolBuilder<DollyTool> getBuilder()
    {
        return new Builder();
    }

    private DollyTool(ExtendedCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        if (buttonIndex == 0)
        {
            this.mouseStart = window.getCursorPosition();

            WindowSize windowSize = window.getWindowSize();
            oldTwist = cameraModel.getTwist();
            oldLog10Distance = cameraModel.getLog10Distance();
            dollySensitivityAdjusted = DOLLY_SENSITIVITY / Math.min(windowSize.width, windowSize.height);
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xPos, double yPos)
    {
        if (window.getMouseButtonState(0) == MouseButtonState.Pressed && xPos != this.mouseStart.x && yPos != this.mouseStart.y)
        {
            cameraModel.setTwist(oldTwist + (float) Math.toDegrees((xPos - this.mouseStart.x) * dollySensitivityAdjusted));
            cameraModel.setLog10Distance((float) (oldLog10Distance + 0.5 * dollySensitivityAdjusted * (this.mouseStart.y - yPos)));
        }
    }
}
