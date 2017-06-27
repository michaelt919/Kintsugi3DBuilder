package tetzlaff.ibr.alexkautz_workspace.render.new_tool_setup_rename_this_later;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.mvc.models.ReadonlyCameraModel;

/**
 * Created by alexk on 6/27/2017.
 */
public class CameraModelX implements ReadonlyCameraModel {

    public CameraModelX()
    {
        this.orbitMatrix = Matrix4.IDENTITY;
        this.radius = 1.0f;
        this.logRadius = 0.0f;
    }

    float radius;

    float logRadius;

    Matrix4 orbitMatrix;

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
        this.logRadius =(float) (Math.log(radius)/Math.log(2));
    }

    public float getLogRadius() {
        return logRadius;
    }

    public void setLogRadius(float logRadius) {
        this.logRadius = logRadius;
        this.radius = (float) Math.pow(logRadius, 2);
    }

    public Matrix4 getOrbitMatrix() {
        return orbitMatrix;
    }

    public void setOrbitMatrix(Matrix4 orbitMatrix) {
        this.orbitMatrix = orbitMatrix;
    }

    @Override
    public Matrix4 getLookMatrix() {
        return Matrix4.lookAt((new Vector3(0.0f, 0.0f, (1.0f/radius))),
                new Vector3(0,0,0),
                new Vector3(0, 1, 0)).times(
                        orbitMatrix
        );
    }
}
