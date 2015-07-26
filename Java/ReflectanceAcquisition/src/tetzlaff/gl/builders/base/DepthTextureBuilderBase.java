package tetzlaff.gl.builders.base;

import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;
import tetzlaff.gl.builders.DepthTextureBuilder;

public abstract class DepthTextureBuilderBase<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> 
	extends TextureBuilderBase<ContextType, TextureType> implements DepthTextureBuilder<ContextType, TextureType>
{
	private int precision = 8;
	private boolean floatingPoint;
	
	protected int getInternalPrecision()
	{
		return this.precision;
	}
	
	protected boolean isFloatingPointEnabled()
	{
		return this.floatingPoint;
	}
	
	protected DepthTextureBuilderBase(ContextType context)
	{
		super(context);
	}
	
	@Override
	public DepthTextureBuilderBase<ContextType, TextureType> setInternalPrecision(int precision)
	{
		this.precision = precision;;
		return this;
	}
	
	@Override
	public DepthTextureBuilderBase<ContextType, TextureType> setFloatingPointEnabled(boolean enabled)
	{
		this.floatingPoint = enabled;
		return this;
	}
}
