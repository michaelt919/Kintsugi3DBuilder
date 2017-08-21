package tetzlaff.ibr.tools;//Created by alexk on 8/8/2017.

import tetzlaff.gl.vecmath.Vector3;
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

            double windowX = mousePos.x / window.getWindowSize().width;
            double windowY = mousePos.y / window.getWindowSize().height;

            Vector3 newCenter = sceneViewportModel.get3DPositionAtCoordinates(windowX, windowY);
            Object clickedObject = sceneViewportModel.getObjectAtCoordinates(windowX, windowY);
            if (clickedObject instanceof String && "IBRObject".equals(clickedObject))
            {
                cameraModel.setCenter(newCenter);
                System.out.println("Set center to " + newCenter);

                toolSelectionModel.setTool(ToolType.ORBIT);
            }
        }
    }
}
