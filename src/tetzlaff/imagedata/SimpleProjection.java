package tetzlaff.imagedata;

import tetzlaff.gl.vecmath.Matrix4;

/**
 * A simple perspective projection defined simply by aspect ratio and field of view.
 * @author Michael Tetzlaff
 *
 */
public class SimpleProjection implements Projection
{
    /**
     * The aspect ratio.
     */
    public final float aspectRatio;

    /**
     * The field of view.
     */
    public final float verticalFieldOfView;

    /**
     * Creates a new simple perspective projection.
     * @param aspectRatio The aspect ratio.
     * @param verticalFieldOfView The field of view.
     */
    public SimpleProjection(float aspectRatio, float verticalFieldOfView)
    {
        this.aspectRatio = aspectRatio;
        this.verticalFieldOfView = verticalFieldOfView;
    }

    @Override
    public float getVerticalFieldOfView()
    {
        return verticalFieldOfView;
    }

    @Override
    public float getAspectRatio()
    {
        return aspectRatio;
    }

    @Override
    public Matrix4 getProjectionMatrix(float nearPlane, float farPlane)
    {
        return Matrix4.perspective(this.verticalFieldOfView, this.aspectRatio, nearPlane, farPlane);
    }

    @Override
    public String toVSETString()
    {
        return String.format("f\t0\t0\t%.8f\t%.8f", aspectRatio, verticalFieldOfView);
    }
}
