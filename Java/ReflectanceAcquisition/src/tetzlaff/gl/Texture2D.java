package tetzlaff.gl;

public interface Texture2D<ContextType extends Context<? super ContextType>> extends Texture<ContextType>, FramebufferAttachment<ContextType>
{
	int getWidth();
	int getHeight();
}
