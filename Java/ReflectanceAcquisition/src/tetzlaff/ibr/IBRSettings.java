package tetzlaff.ibr;

public class IBRSettings 
{
    private float gamma = 2.2f;
    private float weightExponent = 16.0f;
    private boolean occlusionEnabled = true;
    private float occlusionBias = 0.0025f;
	private boolean ibrEnabled = true;
	private boolean fresnelEnabled = false;
	private boolean pbrGeometricAttenuationEnabled = false;

	public IBRSettings() 
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

	public boolean isIBREnabled() 
	{
		return this.ibrEnabled;
	}

	public void setIBREnabled(boolean ibrEnabled) 
	{
		this.ibrEnabled = ibrEnabled;
	}

	public boolean isFresnelEnabled() 
	{
		return this.fresnelEnabled;
	}

	public void setFresnelEnabled(boolean fresnelEnabled) 
	{
		this.fresnelEnabled = fresnelEnabled;
	}

	public boolean isPBRGeometricAttenuationEnabled() 
	{
		return this.pbrGeometricAttenuationEnabled;
	}

	public void setPBRGeometricAttenuationEnabled(boolean pbrGeometricAttenuationEnabled) 
	{
		this.pbrGeometricAttenuationEnabled = pbrGeometricAttenuationEnabled;
	}
}
