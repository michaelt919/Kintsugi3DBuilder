package tetzlaff.gl;

/**
 * An interface for a buffer object that can serve as an "index buffer" or "element array buffer" in conjunction with one or more vertex buffer objects (VBOs).
 * This buffer specifies the order in which vertices in VBOs should be processed during a draw call.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the index buffer object is associated with.
 */
public interface IndexBuffer<ContextType extends Context<ContextType>> extends Resource, Contextual<ContextType>
{
	/**
	 * Gets the number of indices in the buffer.
	 * @return
	 */
	int count();
	
	/**
	 * Sets the content of the index buffer.
	 * @param data An array containing the array indices.
	 * @return The calling object.
	 */
	IndexBuffer<ContextType> setData(int[] data);
}
