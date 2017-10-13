package tetzlaff.ibrelight.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.ExtendedCameraModel;

final class OrbitTool implements DragTool
{
    private static final double ORBIT_SENSITIVITY = 1.0 * Math.PI; //todo: get from gui somehow
    private double orbitSensitivityAdjusted = 1.0;

    private float oldTwist;
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
    public void mouseButtonPressed(CursorPosition cursorPosition, WindowSize windowSize)
    {
        this.mouseStart = cursorPosition;

        oldOrbitMatrix = cameraModel.getOrbit();
        oldTwist = cameraModel.getTwist();
        orbitSensitivityAdjusted = ORBIT_SENSITIVITY / Math.min(windowSize.width, windowSize.height);
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, WindowSize windowSize)
    {
        if (cursorPosition.y != mouseStart.y || cursorPosition.x != mouseStart.x)
        {
            Vector3 rotationVector = new Vector3(
                (float) (cursorPosition.y - mouseStart.y),
                (float) (cursorPosition.x - mouseStart.x),
                0.0f);

            cameraModel.setOrbit(
                Matrix4.rotateAxis(rotationVector.normalized(), rotationVector.length() * orbitSensitivityAdjusted)
                    .times(oldOrbitMatrix));

            cameraModel.setTwist(oldTwist);
        }
    }
}
