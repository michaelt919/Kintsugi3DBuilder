package tetzlaff.gl;

public interface IndexBuffer<ContextType extends Context> extends Resource 
{
	int count();
	void setData(int[] data);
}
