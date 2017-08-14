package tetzlaff.ibr;//Created by alexk on 7/24/2017.

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.rendering2.tools2.ToolBox;

public abstract class ControllableToolModel 
{
    protected abstract void requestGUIClose();

    public abstract ToolBox.ToolType getTool();
    public abstract void setTool(ToolBox.ToolType tool);

    final void loadEnvironmentMap(File environmentMap) throws IOException
    {
        IBRLoadingModel.getInstance().loadEnvironmentMap(environmentMap);
    }
    
    final void unloadEV()
    {
    	try
    	{
    		IBRLoadingModel.getInstance().loadEnvironmentMap(null);
    	}
    	catch(IOException e)
    	{
    		e.printStackTrace(); // Shouldn't ever get here.
    	}
    }
}
