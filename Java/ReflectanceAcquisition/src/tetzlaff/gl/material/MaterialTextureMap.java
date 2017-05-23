package tetzlaff.gl.material;

import tetzlaff.gl.vecmath.Vector3;

public class MaterialTextureMap 
{
	private String mapName;
	
	private boolean clampingRequired;
	private float base;
	private float gain;
	private Vector3 offset;
	private Vector3 scale;
	
	public MaterialTextureMap() 
	{
		clampingRequired = false;
		base = 0.0f;
		gain = 1.0f;
		offset = new Vector3(0.0f);
		scale = new Vector3(1.0f);
	}

	public String getMapName() 
	{
		return mapName;
	}

	public void setMapName(String mapName) 
	{
		this.mapName = mapName;
	}
	
	public boolean isClampingRequired() 
	{
		return clampingRequired;
	}

	public void setClampingRequired(boolean clampingEnabled) 
	{
		this.clampingRequired = clampingEnabled;
	}

	public float getBase() 
	{
		return base;
	}

	public void setBase(float base) 
	{
		this.base = base;
	}

	public float getGain() 
	{
		return gain;
	}

	public void setGain(float gain) 
	{
		this.gain = gain;
	}

	public Vector3 getOffset() 
	{
		return offset;
	}

	public void setOffset(Vector3 offset) 
	{
		this.offset = offset;
	}

	public Vector3 getScale() 
	{
		return scale;
	}

	public void setScale(Vector3 scale)
	{
		this.scale = scale;
	}
}
