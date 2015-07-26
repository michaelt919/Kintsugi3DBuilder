package tetzlaff.gl.builders.base;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;
import tetzlaff.gl.builders.ColorTextureBuilder;

public abstract class ColorTextureBuilderBase<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> 
	extends TextureBuilderBase<ContextType, TextureType> implements ColorTextureBuilder<ContextType, TextureType>
{
	private ColorFormat internalFormat = ColorFormat.RGBA8;
	
	protected ColorFormat getInternalFormat()
	{
		return internalFormat;
	}
	
	protected ColorTextureBuilderBase(ContextType context)
	{
		super(context);
	}
	
	@Override
	public ColorTextureBuilderBase<ContextType, TextureType> setInternalFormat(ColorFormat format)
	{
		internalFormat = format;
		return this;
	}
}
