package tetzlaff.ibrelight.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ExtendedObjectModel;

final class ObjectTwistTool implements DragTool
{
    private static final double ORBIT_SENSITIVITY = 1.0 * Math.PI; //todo: get from gui somehow
    private double orbitSensitivityAdjusted = 1.0;

    private Matrix4 oldOrbitMatrix;
    private Matrix4 cameraOrbitMatrix;

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

        oldOrbitMatrix = objectModel.getOrbit();
        cameraOrbitMatrix = cameraModel.getOrbit();
        orbitSensitivityAdjusted = ORBIT_SENSITIVITY / Math.min(windowSize.width, windowSize.height);
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, WindowSize windowSize)
    {
        if (cursorPosition.x != mouseStart.x)
        {
            objectModel.setOrbit(
                cameraOrbitMatrix.transpose()
                    .times(Matrix4.rotateZ((cursorPosition.x - mouseStart.x) * orbitSensitivityAdjusted))
                    .times(cameraOrbitMatrix)
                    .times(oldOrbitMatrix));
        }
    }
}
