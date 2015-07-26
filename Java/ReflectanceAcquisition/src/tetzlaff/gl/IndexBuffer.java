package tetzlaff.gl;

public interface IndexBuffer<ContextType extends Context<ContextType>> extends Resource, Contextual<ContextType>
{
	int count();
	IndexBuffer<ContextType> setData(int[] data);
}
