package tetzlaff.mvc.models;

import tetzlaff.gl.vecmath.Vector3;

public abstract class LightModelBase implements LightModel
{
	Vector3 ambientLightColor;
	private boolean environmentMappingEnabled;
	private Vector3[] lightColors;
	
	protected LightModelBase(int lightCount)
	{
    	this.lightColors = new Vector3[lightCount];
    	
    	for (int i = 0; i < lightCount; i++)
    	{
    		lightColors[i] = new Vector3(0.0f, 0.0f, 0.0f);
    	}
    	
    	this.ambientLightColor = new Vector3(0.0f);
	}
	
	@Override
	public int getLightCount() 
	{
		return this.lightColors.length;
	}
	
	@Override
	public Vector3 getAmbientLightColor() 
	{
		return this.ambientLightColor;
	}

	@Override
	public void setAmbientLightColor(Vector3 ambientLightColor) 
	{
		this.ambientLightColor = ambientLightColor;
	}

	@Override
	public boolean getEnvironmentMappingEnabled() 
	{
		return this.environmentMappingEnabled;
	}

	@Override
	public void setEnvironmentMappingEnabled(boolean enabled) 
	{
		this.environmentMappingEnabled = enabled;
	}

	@Override
	public Vector3 getLightColor(int i) 
	{
		return this.lightColors[i];
	}
	
	@Override
	public void setLightColor(int i, Vector3 lightColor)
	{
		this.lightColors[i] = lightColor;
	}
}
