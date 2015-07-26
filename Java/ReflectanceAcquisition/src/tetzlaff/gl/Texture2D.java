package tetzlaff.gl;

public interface Texture2D<ContextType extends Context<ContextType>> extends Texture<ContextType>, FramebufferAttachment<ContextType>
{
	int getWidth();
	int getHeight();
}
