package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ExtendedObjectModel;

final class ObjectTwistTool implements DragTool
{
    private static final double TWIST_SENSITIVITY = Math.PI;
    private double twistSensitivityAdjusted;

    private float oldTwist;

    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;
    private final ExtendedObjectModel objectModel;

    private static class Builder extends ToolBuilderBase<ObjectTwistTool>
    {
        @Override
        public ObjectTwistTool build()
        {
            return new ObjectTwistTool(getCameraModel(), getObjectModel());
        }
    }

    static ToolBuilder<ObjectTwistTool> getBuilder()
    {
        return new Builder();
    }

    private ObjectTwistTool(ExtendedCameraModel cameraModel, ExtendedObjectModel objectModel)
    {
        this.cameraModel = cameraModel;
        this.objectModel = objectModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, WindowSize windowSize)
    {
        this.mouseStart = cursorPosition;
        oldTwist = objectModel.getRotationZ();
        twistSensitivityAdjusted = TWIST_SENSITIVITY / windowSize.width;
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, WindowSize windowSize)
    {
        objectModel.setRotationZ(oldTwist + (float) Math.toDegrees((cursorPosition.x - this.mouseStart.x) * twistSensitivityAdjusted));
    }
}
