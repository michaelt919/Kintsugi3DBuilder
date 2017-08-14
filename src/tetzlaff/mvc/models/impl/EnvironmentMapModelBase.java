package tetzlaff.mvc.models.impl;//Created by alexk on 7/21/2017.

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.IBRLoadingModel;

public abstract class EnvironmentMapModelBase 
{
    public abstract Vector3 getAmbientLightColor() ;
    public abstract boolean getEnvironmentMappingEnabled();
    public abstract Matrix4 getEnvironmentMapMatrix();

    protected final void loadEnvironmentMap(File environmentMapFile) throws IOException
    {
        System.out.println("Loading environment map file " + environmentMapFile.getName());
        IBRLoadingModel.getInstance().loadEnvironmentMap(environmentMapFile);
    }

}
