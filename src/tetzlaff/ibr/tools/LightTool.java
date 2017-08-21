package tetzlaff.ibr.tools;

import tetzlaff.gl.vecmath.DoubleVector2;
import tetzlaff.gl.window.*;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ReadonlyEnvironmentMapModel;
import tetzlaff.models.ReadonlyLightingModel;
import tetzlaff.models.SceneViewportModel;

final class LightTool implements Tool
{
    private CursorPosition mouseStart;

    private final ExtendedCameraModel cameraModel;
    private final ReadonlyEnvironmentMapModel environmentMapModel;
    private final ReadonlyLightingModel lightingModel;
    private final SceneViewportModel sceneViewportModel;

    private static class Builder extends ToolBuilderBase<LightTool>
    {
        @Override
        public LightTool build()
        {
            return new LightTool(getCameraModel(), getEnvironmentMapModel(), getLightingModel(), getSceneViewportModel());
        }
    }

    static ToolBuilder<LightTool> getBuilder()
    {
        return new Builder();
    }

    private LightTool(ExtendedCameraModel cameraModel, ReadonlyEnvironmentMapModel environmentMapModel, ReadonlyLightingModel lightingModel,
        SceneViewportModel sceneViewportModel)
    {
        this.cameraModel = cameraModel;
        this.environmentMapModel = environmentMapModel;
        this.lightingModel = lightingModel;
        this.sceneViewportModel = sceneViewportModel;
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        if (buttonIndex == 0)
        {
            this.mouseStart = window.getCursorPosition();
            WindowSize windowSize = window.getWindowSize();
        }
    }

    private void updateAzimuth(DoubleVector2 windowPosition)
    {

    }

    private void updateInclination(DoubleVector2 windowPosition)
    {

    }

    private void updateDistance(DoubleVector2 windowPosition)
    {

    }


    @Override
    public void cursorMoved(Window<?> window, double xPos, double yPos)
    {
        if (window.getMouseButtonState(0) == MouseButtonState.Pressed && xPos != mouseStart.x && yPos != mouseStart.y)
        {
        }
    }
}
