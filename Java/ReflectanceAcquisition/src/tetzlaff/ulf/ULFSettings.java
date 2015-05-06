package tetzlaff.ulf;

public class ULFSettings 
{
    
    private float gamma = 2.2f;
    private float weightExponent = 16.0f;
    private boolean occlusionEnabled = true;
    private float occlusionBias = 0.0025f;

	public ULFSettings() 
	{
	}

	public float getGamma() 
	{
		return this.gamma;
	}

	public void setGamma(float gamma) 
	{
		this.gamma = gamma;
	}

	public float getWeightExponent() 
	{
		return this.weightExponent;
	}

	public void setWeightExponent(float weightExponent) 
	{
		this.weightExponent = weightExponent;
	}

	public boolean isOcclusionEnabled() 
	{
		return this.occlusionEnabled;
	}

	public void setOcclusionEnabled(boolean occlusionEnabled) 
	{
		this.occlusionEnabled = occlusionEnabled;
	}

	public float getOcclusionBias() 
	{
		return this.occlusionBias;
	}

	public void setOcclusionBias(float occlusionBias) 
	{
		this.occlusionBias = occlusionBias;
	}
}
