package tetzlaff.ibr.rendering2;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.mvc.models.ReadonlyLightModel;

import java.util.ArrayList;

public class LightModelX implements ReadonlyLightModel {

    public LightModelX(int startingNumLights) {
        lights = new ArrayList<PointLightModelX>();
        for (int i = 0; i < startingNumLights; i++) addLight();

        ambientLightColor = new Vector3(0f,0f,0f);
    }

    private final ArrayList<PointLightModelX> lights;
    private Vector3 ambientLightColor;

    public void addLight(){
        PointLightModelX newLight = new PointLightModelX();
        lights.add(newLight);
    }

    @Override
    public int getLightCount() {
        return lights.size();
    }

    @Override
    public boolean isLightVisualizationEnabled(int i) {
        return true;
    }

    @Override
    public Vector3 getLightColor(int i) {
        return lights.get(i).getColor();
    }

    public void setLightColor(int i, Vector3 color){
        lights.get(i).setColor(color);
    }

    @Override
    public Vector3 getAmbientLightColor() {
        return ambientLightColor;
    }

    public void setAmbientLightColor(Vector3 ambientLightColor){
        this.ambientLightColor = ambientLightColor;
    }

    @Override
    public boolean getEnvironmentMappingEnabled() {
        return false;
    }

    @Override
    public Matrix4 getLightMatrix(int i) {
        return lights.get(i).getLookMatrix();
    }


    public PointLightModelX getLight(int i){
        return lights.get(i);
    }
}
