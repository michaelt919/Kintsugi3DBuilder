package tetzlaff.ibrelight.tools;//Created by alexk on 8/8/2017.

import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.SceneViewport;
import tetzlaff.models.SceneViewportModel;

final class LookAtPointTool implements DragTool
{
    private final ExtendedCameraModel cameraModel;
    private final SceneViewportModel sceneViewportModel;

    private static class Builder extends ToolBuilderBase<LookAtPointTool>
    {
        @Override
        public LookAtPointTool build()
        {
            return new LookAtPointTool(getCameraModel(), getSceneViewportModel());
        }
    }

    static ToolBuilder<LookAtPointTool> getBuilder()
    {
        return new Builder();
    }

    private LookAtPointTool(ExtendedCameraModel cameraModel, SceneViewportModel sceneViewportModel)
    {
        this.cameraModel = cameraModel;
        this.sceneViewportModel = sceneViewportModel;
    }

    @Override
    public void mouseButtonPressed(CursorPosition cursorPosition, WindowSize windowSize)
    {
        double normalizedX = cursorPosition.x / windowSize.width;
        double normalizedY = cursorPosition.y / windowSize.height;

        SceneViewport sceneViewport = sceneViewportModel.getSceneViewport();
        Object clickedObject = sceneViewport.getObjectAtCoordinates(normalizedX, normalizedY);
        if (clickedObject instanceof String && "IBRObject".equals(clickedObject))
        {
            cameraModel.setTarget(sceneViewport.get3DPositionAtCoordinates(normalizedX, normalizedY));
        }
    }
}
