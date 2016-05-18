package tetzlaff.ulf;

import tetzlaff.gl.helpers.Matrix4;

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
	
	float getVerticalFieldOfView();
	float getAspectRatio();
	
	/**
	 * Convert to a string designed for use in a VSET file
	 */
	String toVSETString();
}
