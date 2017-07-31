package tetzlaff.mvc.models;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Vector3;

import java.io.File;

public abstract class ControllableEnvironmentMapModel {
    public abstract Vector3 getAmbientLightColor() ;

    private final ControllableToolModel tool;
    public ControllableEnvironmentMapModel(ControllableToolModel tool) {
        this.tool = tool;
    }

    protected final void loadEV(File ev){
        System.out.println("Loading EV file " + ev.getName());
        tool.loadEV(ev);
    }
    protected final void unloadEV(){
        System.out.println("Unloading EV file.");
        tool.unloadEV();
    }

}
