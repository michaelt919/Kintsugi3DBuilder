package tetzlaff.ibr.rendering2;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.mvc.models.ControllableLightModel;
import tetzlaff.mvc.models.ReadonlyLightModel;

import java.util.ArrayList;

public class LightModel2 implements ControllableLightModel {

    public LightModel2(int startingNumLights) {
        lights = new ArrayList<PointLightModel2>();
        for (int i = 0; i < startingNumLights; i++) addLight();

        ambientLightColor = new Vector3(0f,0f,0f);
    }

    private final ArrayList<PointLightModel2> lights;
    private Vector3 ambientLightColor;

    public void addLight(){
        PointLightModel2 newLight = new PointLightModel2();
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

    @Override
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


    public PointLightModel2 getLight(int i){
        return lights.get(i);
    }
}
