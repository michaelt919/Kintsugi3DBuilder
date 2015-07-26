package tetzlaff.gl;

public interface Contextual<ContextType extends Context<ContextType>>
{
	ContextType getContext();
}
