package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.listeners.CursorPositionListener;
import tetzlaff.gl.window.listeners.KeyPressListener;
import tetzlaff.gl.window.listeners.MouseButtonPressListener;
import tetzlaff.gl.window.listeners.ScrollListener;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ReadonlyEnvironmentMapModel;
import tetzlaff.models.ReadonlyLightingModel;
import tetzlaff.models.SceneViewportModel;

class AbstractTool implements CursorPositionListener, MouseButtonPressListener, ScrollListener, KeyPressListener
{
    protected double mouseStartX_MB1;
    protected double mouseStartY_MB1;

    protected static final int MB1 = 0;//mouse button
    protected static final int MB2 = 1;
    protected static final int MB3 = 2;

    protected final ExtendedCameraModel cameraModel;
    protected final ReadonlyEnvironmentMapModel environmentMapModel;
    protected final ReadonlyLightingModel lightingModel;
    protected final SceneViewportModel sceneViewportModel;

    AbstractTool(ExtendedCameraModel cameraModel, ReadonlyEnvironmentMapModel environmentMapModel, ReadonlyLightingModel lightingModel, SceneViewportModel sceneViewportModel)
    {
        this.cameraModel = cameraModel;
        this.environmentMapModel = environmentMapModel;
        this.lightingModel = lightingModel;
        this.sceneViewportModel = sceneViewportModel;
    }

    @Override
    public void scroll(Window<?> window, double xoffset, double yoffset)
    {
    }

    @Override
    public void cursorMoved(Window<?> window, double xpos, double ypos)
    {
    }

    @Override
    public void keyPressed(Window<?> window, int keycode, ModifierKeys mods)
    {
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        System.out.println("MB: " + buttonIndex);
        if (buttonIndex == MB1)
        {
            CursorPosition pos = window.getCursorPosition();
            mouseStartX_MB1 = pos.x;
            mouseStartY_MB1 = pos.y;
        }
    }

    protected Vector3 getPoint(double x, double y)
    {
        return sceneViewportModel.get3DPositionAtCoordinates(x, y);
    }

    protected enum SceneObjectType
    {
        OBJECT,
        LIGHT,
        BACKGROUND,
        OTHER
    }

    protected SceneObjectType getClickedObjectType(double x, double y)
    {
        Object thing = sceneViewportModel.getObjectAtCoordinates(x, y);
        if (thing != null && thing instanceof String)
        {
            if (thing.equals("IBRObject"))
            {
                return SceneObjectType.OBJECT;
            }
            else if (((String) thing).startsWith("Light"))
            {
                return SceneObjectType.LIGHT;
            }
        }
        else
        {
            return SceneObjectType.BACKGROUND;
        }
        return SceneObjectType.OTHER;
    }
}