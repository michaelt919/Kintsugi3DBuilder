package tetzlaff.ibr.tools;//Created by alexk on 8/8/2017.

import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.SceneViewportModel;

final class CenterPointTool implements DragTool
{

    private final ToolBindingModel toolBindingModel;
    private final ExtendedCameraModel cameraModel;
    private final SceneViewportModel sceneViewportModel;

    private static class Builder extends ToolBuilderBase<CenterPointTool>
    {
        @Override
        public CenterPointTool build()
        {
            return new CenterPointTool(getCameraModel(), getToolBindingModel(), getSceneViewportModel());
        }
    }

    static ToolBuilder<CenterPointTool> getBuilder()
    {
        return new Builder();
    }

    private CenterPointTool(ExtendedCameraModel cameraModel, ToolBindingModel toolBindingModel, SceneViewportModel sceneViewportModel)
    {
        this.toolBindingModel = toolBindingModel;
        this.cameraModel = cameraModel;
        this.sceneViewportModel = sceneViewportModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, WindowSize windowSize)
    {
        double normalizedX = cursorPosition.x / windowSize.width;
        double normalizedY = cursorPosition.y / windowSize.height;

        Object clickedObject = sceneViewportModel.getObjectAtCoordinates(normalizedX, normalizedY);
        if (clickedObject instanceof String && "IBRObject".equals(clickedObject))
        {
            cameraModel.setCenter(sceneViewportModel.get3DPositionAtCoordinates(normalizedX, normalizedY));
        }
    }
}
