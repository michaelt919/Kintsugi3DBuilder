package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.ExtendedCameraModel;

final class FocalLengthTool implements DragTool
{
    private static final double FOCAL_LENGTH_SENSITIVITY = Math.PI;
    private double focalLengthSensitivityAdjusted;

    private double oldLog10FocalLength;

    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;

    private static class Builder extends ToolBuilderBase<FocalLengthTool>
    {
        @Override
        public FocalLengthTool build()
        {
            return new FocalLengthTool(getCameraModel());
        }
    }

    static ToolBuilder<FocalLengthTool> getBuilder()
    {
        return new Builder();
    }

    private FocalLengthTool(ExtendedCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, WindowSize windowSize)
    {
        this.mouseStart = cursorPosition;
        oldLog10FocalLength = Math.log10(cameraModel.getFocalLength());
        focalLengthSensitivityAdjusted = FOCAL_LENGTH_SENSITIVITY / windowSize.width;
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, WindowSize windowSize)
    {
        cameraModel.setFocalLength((float)Math.pow(10,
            oldLog10FocalLength
                + (float) Math.toDegrees((Math.abs(cursorPosition.x - windowSize.width * 0.5)
                            - Math.abs(this.mouseStart.x - windowSize.width * 0.5))
                * focalLengthSensitivityAdjusted)));
    }
}
