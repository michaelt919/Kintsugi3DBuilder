package tetzlaff.ibr.tools;//Created by alexk on 8/8/2017.

import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.SceneViewportModel;

final class CenterPointTool implements Tool
{

    private final ToolSelectionModel toolSelectionModel;
    private final ExtendedCameraModel cameraModel;
    private final SceneViewportModel sceneViewportModel;

    private static class Builder extends ToolBuilderBase<CenterPointTool>
    {
        @Override
        public CenterPointTool build()
        {
            return new CenterPointTool(getCameraModel(), getToolSelectionModel(), getSceneViewportModel());
        }
    }

    static ToolBuilder<CenterPointTool> getBuilder()
    {
        return new Builder();
    }

    private CenterPointTool(ExtendedCameraModel cameraModel, ToolSelectionModel toolSelectionModel, SceneViewportModel sceneViewportModel)
    {
        this.toolSelectionModel = toolSelectionModel;
        this.cameraModel = cameraModel;
        this.sceneViewportModel = sceneViewportModel;
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        if (buttonIndex == 0)
        {
            CursorPosition mousePos = window.getCursorPosition();

            double normalizedX = mousePos.x / window.getWindowSize().width;
            double normalizedY = mousePos.y / window.getWindowSize().height;

            Object clickedObject = sceneViewportModel.getObjectAtCoordinates(normalizedX, normalizedY);
            if (clickedObject instanceof String && "IBRObject".equals(clickedObject))
            {
                cameraModel.setCenter(sceneViewportModel.get3DPositionAtCoordinates(normalizedX, normalizedY));
                toolSelectionModel.setTool(ToolType.ORBIT);
            }
        }
    }
}
