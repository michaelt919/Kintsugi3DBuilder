package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ExtendedObjectModel;

final class ObjectCenterTool implements DragTool
{
    private static final double PAN_SENSITIVITY = 1.0;
    private double panSensitivityAdjusted = 1.0;

    private Vector3 oldCenter = Vector3.ZERO;
    private Matrix4 orbit;

    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;
    private final ExtendedObjectModel objectModel;

    private static class Builder extends ToolBuilderBase<ObjectCenterTool>
    {
        @Override
        public ObjectCenterTool build()
        {
            return new ObjectCenterTool(getCameraModel(), getObjectModel());
        }
    }

    static ToolBuilder<ObjectCenterTool> getBuilder()
    {
        return new Builder();
    }

    private ObjectCenterTool(ExtendedCameraModel cameraModel, ExtendedObjectModel objectModel)
    {
        this.cameraModel = cameraModel;
        this.objectModel = objectModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, WindowSize windowSize)
    {
        this.mouseStart = cursorPosition;

        oldCenter = objectModel.getCenter();
        orbit = cameraModel.getOrbit().times(objectModel.getOrbit());

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
        Vector3 worldMoveVector = orbit.transpose().times(moveVector.asDirection()).getXYZ();
        objectModel.setCenter(oldCenter.minus(worldMoveVector));
    }
}
