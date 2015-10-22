package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.Vector4;

/**
 * Defines an interface for an object that draws unstructured light fields.
 * @author Michael Tetzlaff
 *
 */
public interface ULFDrawable extends Drawable
{
	/**
	 * Sets a loading monitor with callbacks that are fired when the light field finishes loading and/or at certain checkpoints when loading.
	 * @param callback The loading monitor.
	 */
	void setOnLoadCallback(ULFLoadingMonitor callback);
	
	/**
	 * Gets the exponent of the gamma curve when applying color correction on the final rendering and inverse color correction on the input images.
	 * @return The gamma curve exponent.
	 */
	float getGamma();
	
	/**
	 * Gets the exponent to use in the view weighting formula.
	 * @return The view weighting exponent.
	 */
	float getWeightExponent();
	
	/**
	 * Gets whether or not visibility testing is enabled.
	 * @return true if visibility testing is enabled, false otherwise.
	 */
	boolean isOcclusionEnabled();
	
	/**
	 * Gets the depth bias to use when performing visibility testing.
	 * @return The depth bias value.
	 */
	float getOcclusionBias();
	
	/**
	 * Gets whether or not the light field should first be rendered to a half-resolution off-screen framebuffer and then upsampled.
	 * Enabling this should increase performance at the cost of image fidelity.
	 * @return true if rendering at half-resolution is enabled, false otherwise.
	 */
	boolean getHalfResolution();
	
	/**
	 * Gets the background color to use when rendering.
	 * @return The background color as a vector in RGBA space.
	 */
	Vector4 getBackgroundColor();
	
	/**
	 * Gets whether or not to use the k-nearest neighbors algorithm proposed in Buehler et al., 2001.
	 * If this is false, all views will be used as proposed by Berrier et al., 2015, will be used instead.
	 * The results of using each algorithm may vary dramatically.
	 * Depending on how GPU hardware is optimized, enabling k-nearest neighbors may either increase or decrease performance.
	 * In some circumstances it may also affect visual fidelity either positively or negatively.
	 * @return true if k-nearest neighbors is enabled, false otherwise.
	 */
	boolean isKNeighborsEnabled();
	
	/**
	 * Gets the number of neighbors to use in k-nearest neighbors mode.
	 * @return The number of neighbors to use.
	 * @see isKNeighborsEnabled()
	 */
	int getKNeighborCount();
	
	/**
	 * Gets the exponent of the gamma curve when applying color correction on the final rendering and inverse color correction on the input images.
	 * @param gamma The gamma curve exponent.
	 */
	void setGamma(float gamma);
	
	/**
	 * Sets the exponent to use in the view weighting formula.
	 * @param weightExponent The view weighting exponent.
	 */
	void setWeightExponent(float weightExponent);
	
	/**
	 * Sets whether or not visibility testing is enabled.
	 * @param occlusionEnabled true if visibility testing is enabled, false otherwise.
	 */
	void setOcclusionEnabled(boolean occlusionEnabled);
	
	/**
	 * Sets the depth bias to use when performing visibility testing.
	 * @param occlusionBias The depth bias value.
	 */
	void setOcclusionBias(float occlusionBias);
	
	/**
	 * Sets whether or not to render the camera poses as rectangles to visualize the light field sampling.
	 * @param camerasEnabled true if camera visualizations should be rendered, false otherwise.
	 */
	void setVisualizeCameras(boolean camerasEnabled);
	
	/**
	 * Sets whether or not the light field should first be rendered to a half-resolution off-screen framebuffer and then upsampled.
	 * Enabling this should increase performance at the cost of image fidelity.
	 * @param halfResEnabled true if rendering at half-resolution should be enabled, false otherwise.
	 */
	void setHalfResolution(boolean halfResEnabled);
	
	/**
	 * Sets whether or not multisampling should be enabled.
	 * Disabling multisampling should improve performance at the cost of image fidelity.
	 * @param multisamplingEnabled true if multisampling should be enabled, false otherwise.
	 */
	void setMultisampling(boolean multisamplingEnabled);
	
	/**
	 * Sets the background color to use when rendering.
	 * @param backgroundColor The background color as a vector in RGBA space.
	 */
	void setBackgroundColor(Vector4 backgroundColor);
	
	/**
	 * Sets whether or not to use the k-nearest neighbors algorithm proposed in Buehler et al., 2001.
	 * If this is false, all views will be used as proposed by Berrier et al., 2015, will be used instead.
	 * The results of using each algorithm may vary dramatically.
	 * Depending on how GPU hardware is optimized, enabling k-nearest neighbors may either increase or decrease performance.
	 * In some circumstances it may also affect visual fidelity either positively or negatively.
	 * @param kNeighborsEnabled true if k-nearest neighbors should be enabled, false otherwise.
	 */
	void setKNeighborsEnabled(boolean kNeighborsEnabled);
	
	/**
	 * Sets the number of neighbors to use in k-nearest neighbors mode.
	 * @param kNeighborCount The number of neighbors to use.
	 * @see setKNeighborsEnabled
	 */
	void setKNeighborCount(int kNeighborCount);
	
	/**
	 * Requests that the unstructured light field be rendered using camera poses as defined by an external view set.
	 * @param width The width of each output image.
	 * @param height The height of each output image.
	 * @param targetVSETFile The view set file containing the camera poses from which to render.
	 * @param exportPath A file path to a directory where the images are to be written.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	void requestResample(int width, int height, File targetVSETFile, File exportPath) throws IOException;

}
