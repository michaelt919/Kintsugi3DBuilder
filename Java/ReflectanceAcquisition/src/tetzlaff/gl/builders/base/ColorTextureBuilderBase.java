package tetzlaff.gl.builders.base;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;
import tetzlaff.gl.builders.ColorTextureBuilder;

public abstract class ColorTextureBuilderBase<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> 
	extends TextureBuilderBase<ContextType, TextureType> implements ColorTextureBuilder<ContextType, TextureType>
{
	private ColorFormat internalColorFormat = ColorFormat.RGBA8;
	private CompressionFormat internalCompressionFormat = null;
	
	protected ColorFormat getInternalColorFormat()
	{
		return internalColorFormat;
	}
	
	protected CompressionFormat getInternalCompressionFormat()
	{
		return internalCompressionFormat;
	}
	
	protected boolean isInternalFormatCompressed()
	{
		return internalCompressionFormat != null;
	}
	
	protected ColorTextureBuilderBase(ContextType context)
	{
		super(context);
	}
	
	@Override
	public ColorTextureBuilderBase<ContextType, TextureType> setInternalFormat(ColorFormat format)
	{
		internalColorFormat = format;
		internalCompressionFormat = null;
		return this;
	}
	
	@Override
	public ColorTextureBuilderBase<ContextType, TextureType> setInternalFormat(CompressionFormat format)
	{
		internalColorFormat = null;
		internalCompressionFormat = format;
		return this;
	}
	
	@Override
	public ColorTextureBuilderBase<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations)
	{
		super.setMultisamples(samples, fixedSampleLocations);
		return this;
	}
	
	@Override
	public ColorTextureBuilderBase<ContextType, TextureType> setMipmapsEnabled(boolean enabled)
	{
		super.setMipmapsEnabled(enabled);
		return this;
	}
	
	@Override
	public ColorTextureBuilderBase<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled)
	{
		super.setLinearFilteringEnabled(enabled);
		return this;
	}
	
	@Override
	public ColorTextureBuilderBase<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy)
	{
		super.setMaxAnisotropy(maxAnisotropy);
		return this;
	}
}
