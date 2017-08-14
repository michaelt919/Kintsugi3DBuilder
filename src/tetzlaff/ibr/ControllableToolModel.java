package tetzlaff.ibr;//Created by alexk on 7/24/2017.

import java.io.File;

import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.rendering2.tools2.ToolBox;

public abstract class ControllableToolModel 
{
    protected abstract void requestGUIClose();

    public abstract ToolBox.TOOL getTool();
    public abstract void setTool(ToolBox.TOOL tool);

    private IBRRenderableListModel<?> model;
    
    public final void setModel(IBRRenderableListModel<?> model) 
    {
        this.model = model;
    }

    final void loadEnvironmentMap(File environmentMap)
    {
        model.getSelectedItem().setEnvironment(environmentMap);
    }

    public final IBRRenderable<?> getIBRRenderable()
    {
        return model.getSelectedItem();
    }
    
    final void unloadEV()
    {
        model.getSelectedItem().setEnvironment(null);
    }

    public Vector3 getPoint(double x, double y)
    {
        return model.getSelectedItem().getSceneViewportModel().get3DPositionAtCoordinates(x, y);
    }

    public enum SceneObjectType
    {
        OBJECT, LIGHT, BACKGROUND, OTHER
    }

    public SceneObjectType getClickedObjectType(double x, double y)
    {
        Object thing = model.getSelectedItem().getSceneViewportModel().getObjectAtCoordinates(x, y);
        if(thing != null && thing instanceof String)
        {
            if(thing.equals("IBRObject"))
            {
                return SceneObjectType.OBJECT;
            }
            else if(((String) thing).startsWith("Light"))
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
