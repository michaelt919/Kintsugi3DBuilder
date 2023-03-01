package tetzlaff.gl.core;

public abstract class DrawableBase<ContextType extends Context<ContextType>> implements Drawable<ContextType>
{
    private PrimitiveMode defaultPrimitiveMode = PrimitiveMode.TRIANGLES;

    @Override
    public PrimitiveMode getDefaultPrimitiveMode()
    {
        return defaultPrimitiveMode;
    }

    @Override
    public void setDefaultPrimitiveMode(PrimitiveMode primitiveMode)
    {
        this.defaultPrimitiveMode = primitiveMode;
    }
}
