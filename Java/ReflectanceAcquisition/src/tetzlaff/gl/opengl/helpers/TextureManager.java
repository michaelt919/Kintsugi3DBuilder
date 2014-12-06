package tetzlaff.gl.opengl.helpers;

import tetzlaff.gl.exceptions.NoAvailableTextureUnitsException;

public class TextureManager<TextureType>
{
	public final int length;
	
	private int[] keys;
	private Object[] textures;
	private int nextSlot;

	public TextureManager(int length) 
	{
		this.length = length;
		keys = new int[length];
		textures = new Object[length];
		nextSlot = 0;
	}

	public int assignTextureByKey(int key, TextureType texture)
	{
		// Check if the key has already been assigned a texture
		for (int i = 0; i < length; i++)
		{
			if (keys[i] == key)
			{
				textures[i] = texture;
				return i;
			}
		}
		
		if (nextSlot == length)
		{
			// No more slots available.
			throw new NoAvailableTextureUnitsException("No more available texture units.");
		}
		else
		{
			// The key has not been assigned a texture, so use the next available slot
			keys[nextSlot] = key;
			textures[nextSlot] = texture;
			return nextSlot++;
		}
	}

	@SuppressWarnings("unchecked")
	public TextureType getTextureByUnit(int index)
	{
		return (TextureType)textures[index];
	}
}
