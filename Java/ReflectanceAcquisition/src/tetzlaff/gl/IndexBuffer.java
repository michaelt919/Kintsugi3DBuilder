package tetzlaff.gl;

public interface IndexBuffer<ContextType extends Context<? super ContextType>> extends Resource 
{
	int count();
	void setData(int[] data);
}
