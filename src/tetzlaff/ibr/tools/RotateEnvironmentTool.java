package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.EnvironmentModel;

final class RotateEnvironmentTool implements DragTool
{
    private static final double ROTATE_SENSITIVITY = Math.PI; //todo: get from gui somehow
    private double rotateSensitivityAdjusted = 1.0;

    private float oldEnvironmentRotation;

    private CursorPosition mouseStart;

    private final EnvironmentModel environmentModel;

    private static class Builder extends ToolBuilderBase<RotateEnvironmentTool>
    {
        @Override
        public RotateEnvironmentTool build()
        {
            return new RotateEnvironmentTool(getEnvironmentModel());
        }
    }

    static ToolBuilder<RotateEnvironmentTool> getBuilder()
    {
        return new Builder();
    }

    private RotateEnvironmentTool(EnvironmentModel environmentModel)
    {
        this.environmentModel = environmentModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, WindowSize windowSize)
    {
        this.mouseStart = cursorPosition;

        oldEnvironmentRotation = environmentModel.getEnvironmentRotation();
        rotateSensitivityAdjusted = ROTATE_SENSITIVITY / Math.min(windowSize.width, windowSize.height);
    }

    @Override
    public void cursorDragged(CursorPosition cursorPosition, WindowSize windowSize)
    {
        environmentModel.setEnvironmentRotation((float)(oldEnvironmentRotation + (cursorPosition.x - mouseStart.x) * rotateSensitivityAdjusted));
    }
}
