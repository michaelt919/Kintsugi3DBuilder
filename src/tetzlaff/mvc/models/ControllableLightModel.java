package tetzlaff.mvc.models;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Vector3;

public abstract class ControllableLightModel implements ReadonlyLightModel {
    public abstract void setLightColor(int i, Vector3 color);

    private ControllableEnvironmentMapModel ev;

    public ControllableLightModel(ControllableEnvironmentMapModel ev) {
        this.ev = ev;
    }

    @Override
    public final Vector3 getAmbientLightColor() {
        return ev.getAmbientLightColor();
    }


    public abstract ControllableSubLightModel getLight(int i);


}
