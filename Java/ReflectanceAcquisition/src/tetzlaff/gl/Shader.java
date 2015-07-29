package tetzlaff.gl;

/**
 * An interface for an object that can serve as a shader, one of possibly many components of a program.
 * This is an empty interface that simply serves as a placeholder in the type hierarchy,
 * so that a class can explicitly state that it can serve this role in a manner that can be checked at compile-time with loose coupling.
 * Implementations should provide whatever methods are needed to ensure that they can fulfill this role for a specific GL architecture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the shader is associated with.
 */
public interface Shader<ContextType extends Context<ContextType>> extends Resource, Contextual<ContextType>
{

}
