package tetzlaff.mvc.models.impl;//Created by alexk on 7/21/2017.

import java.io.File;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.ControllableToolModel;

public abstract class EnvironmentMapModelBase 
{
    public abstract Vector3 getAmbientLightColor() ;
    public abstract boolean getEnvironmentMappingEnabled();
    public abstract Matrix4 getEnvironmentMapMatrix();

    private final ControllableToolModel tool;
    public EnvironmentMapModelBase(ControllableToolModel tool) {
        this.tool = tool;
    }

    protected final void loadEnvironmentMap(File environmentMapFile){
        System.out.println("Loading environment map file " + environmentMapFile.getName());
        tool.loadEnvironmentMap(environmentMapFile);
    }

}
