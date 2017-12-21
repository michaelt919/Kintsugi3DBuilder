package tetzlaff.gl.core;

/**
 * An interface for a buffer object that can serve as an "index buffer" or "element array buffer" in conjunction with one or more vertex buffer objects (VBOs).
 * This buffer specifies the order in which vertices in VBOs should be processed during a draw call.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the index buffer object is associated with.
 */
public interface IndexBuffer<ContextType extends Context<ContextType>> extends Resource, Contextual<ContextType>
{
    int count();
    IndexBuffer<ContextType> setData(int... data);
}
