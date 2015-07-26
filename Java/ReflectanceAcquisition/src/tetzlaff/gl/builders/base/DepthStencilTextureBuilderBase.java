package tetzlaff.gl.builders.base;

import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;
import tetzlaff.gl.builders.DepthStencilTextureBuilder;

public abstract class DepthStencilTextureBuilderBase<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> 
	extends TextureBuilderBase<ContextType, TextureType> implements DepthStencilTextureBuilder<ContextType, TextureType>
{
	private boolean floatingPoint;
	
	protected boolean isFloatingPointEnabled()
	{
		return this.floatingPoint;
	}
	
	protected DepthStencilTextureBuilderBase(ContextType context)
	{
		super(context);
	}
	
	@Override
	public DepthStencilTextureBuilderBase<ContextType, TextureType> setFloatingPointDepthEnabled(boolean enabled)
	{
		this.floatingPoint = enabled;
		return this;
	}
}
