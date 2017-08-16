package tetzlaff.gl;

/**
 * A simple interface for a type that is associated with a Context implementation.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the Context.
 */
public interface Contextual<ContextType extends Context<ContextType>>
{
    /**
     * Gets the associated GL context.
     * @return A GL context associated with this object.
     */
    ContextType getContext();
}
