package tetzlaff.gl.builders.base;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;
import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.builders.DepthTextureBuilder;
import tetzlaff.gl.builders.StencilTextureBuilder;
import tetzlaff.gl.builders.TextureBuilder;

public abstract class StencilTextureBuilderBase<ContextType extends Context<? super ContextType>, TextureType extends Texture<ContextType>> 
	extends TextureBuilderBase<ContextType, TextureType> implements StencilTextureBuilder<ContextType, TextureType>
{
	private int precision = 8;
	
	protected int getInternalPrecision()
	{
		return this.precision;
	}
	
	protected StencilTextureBuilderBase(ContextType context)
	{
		super(context);
	}
	
	@Override
	public StencilTextureBuilderBase<ContextType, TextureType> setInternalPrecision(int precision)
	{
		this.precision = precision;;
		return this;
	}
}
