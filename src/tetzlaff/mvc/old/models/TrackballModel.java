package tetzlaff.mvc.old.models;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.ReadonlyCameraModel;

public class TrackballModel implements ReadonlyCameraModel
{
    private Matrix4 trackballMatrix;
    private float scale;
    private float logScale;
    private Vector3 cameraPosition;

    public TrackballModel()
    {
        this.trackballMatrix = Matrix4.IDENTITY;
        this.scale = 1.0f;
        this.setLogScale(0.0f);
        this.cameraPosition = new Vector3(0.0f, 0.0f, 1.0f);
    }

    @Override
    public Matrix4 getLookMatrix()
    {
        return Matrix4.lookAt(
                this.cameraPosition.times(1.0f / this.getScale()),
                new Vector3(0.0f, 0.0f, 0.0f),
                new Vector3(0.0f, 1.0f, 0.0f))
            .times(this.getTrackballMatrix());
    }

    public Matrix4 getTrackballMatrix()
    {
        return this.trackballMatrix;
    }

    public void setTrackballMatrix(Matrix4 matrix)
    {
        /*this.oldTrackballMatrix =*/ this.trackballMatrix = matrix;
    }

    public float getScale()
    {
        return this.scale;
    }

    public void setScale(float scale)
    {
        this.scale = scale;
        this.logScale = (float)(Math.log(this.scale) / Math.log(2.0));
    }

    public float getLogScale()
    {
        return logScale;
    }

    public void setLogScale(float logScale)
    {
        this.logScale = logScale;
        this.scale = (float)Math.pow(2, this.logScale);
    }

    public Vector3 getCameraPosition()
    {
        return this.cameraPosition;
    }

    public void setCameraPosition(Vector3 cameraPosition)
    {
        this.cameraPosition = cameraPosition;
    }
}
