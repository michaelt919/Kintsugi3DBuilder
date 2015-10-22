package tetzlaff.ulf;

/**
 * A model of the rendering settings for unstructured light fields.
 * @author Michael Tetzlaff
 *
 */
public class ULFSettings 
{
    /**
     * The exponent of the gamma curve when applying color correction on the final rendering and inverse color correction on the input images.
     */
    private float gamma = 2.2f;
    
    /**
     * The exponent to use in the view weighting formula.
     */
    private float weightExponent = 16.0f;
    
    /**
     * Whether or not visibility testing is enabled.
     */
    private boolean occlusionEnabled = true;
    
    /**
     * The depth bias to use when performing visibility testing.
     */
    private float occlusionBias = 0.0025f;

    /**
     * Creates a settings model with the default options.
     */
	public ULFSettings() 
	{
	}

	/**
	 * Gets the exponent of the gamma curve when applying color correction on the final rendering and inverse color correction on the input images.
	 * @return The gamma curve exponent.
	 */
	public float getGamma() 
	{
		return this.gamma;
	}

	/**
	 * Gets the exponent of the gamma curve when applying color correction on the final rendering and inverse color correction on the input images.
	 * @param gamma The gamma curve exponent.
	 */
	public void setGamma(float gamma) 
	{
		this.gamma = gamma;
	}

	/**
	 * Gets the exponent to use in the view weighting formula.
	 * @return The view weighting exponent.
	 */
	public float getWeightExponent() 
	{
		return this.weightExponent;
	}

	/**
	 * Sets the exponent to use in the view weighting formula.
	 * @param weightExponent The view weighting exponent.
	 */
	public void setWeightExponent(float weightExponent) 
	{
		this.weightExponent = weightExponent;
	}

	/**
	 * Gets whether or not visibility testing is enabled.
	 * @return true if visibility testing is enabled, false otherwise.
	 */
	public boolean isOcclusionEnabled() 
	{
		return this.occlusionEnabled;
	}

	/**
	 * Sets whether or not visibility testing is enabled.
	 * @param occlusionEnabled true if visibility testing is enabled, false otherwise.
	 */
	public void setOcclusionEnabled(boolean occlusionEnabled) 
	{
		this.occlusionEnabled = occlusionEnabled;
	}

	/**
	 * Gets the depth bias to use when performing visibility testing.
	 * @return The depth bias value.
	 */
	public float getOcclusionBias() 
	{
		return this.occlusionBias;
	}

	/**
	 * Sets the depth bias to use when performing visibility testing.
	 * @param occlusionBias The depth bias value.
	 */
	public void setOcclusionBias(float occlusionBias) 
	{
		this.occlusionBias = occlusionBias;
	}
}
