package tetzlaff.imagedata;

import tetzlaff.gl.vecmath.Matrix4;

/**
 * An interface for a definition of 3D to 2D projection that can be expressed as a projective transformation matrix.
 * @author Michael Tetzlaff
 *
 */
public interface Projection 
{
    /**
     * Gets the projective transformation matrix for this projection.
     * @param nearPlane The plane in 3D Cartesian space that will get mapped to the plane z=1.
     * @param farPlane The plane in 3D Cartesian space that will get mapped to the plane z=-1.
     * @return The projective transformation matrix.
     */
    Matrix4 getProjectionMatrix(float nearPlane, float farPlane);

    /**
     * Gets the vertical field of view.
     * @return The vertical field of view.
     */
    float getVerticalFieldOfView();

    /**
     * Gets the aspect ratio of the projection.
     * @return The aspect ratio.
     */
    float getAspectRatio();

    /**
     * Convert to a string designed for use in a VSET file
     * @return A string representing this projection.
     */
    String toVSETString();
}
