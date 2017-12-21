package tetzlaff.gl.core;

/**
 * An interface for an object that can serve as an attachment for a framebuffer.
 * This is an empty interface that simply serves as a placeholder in the type hierarchy,
 * so that a class can explicitly state that it can serve this role in a manner that can be checked at compile-time with loose coupling.
 * Implementations should provide whatever methods are needed to ensure that they can fulfill this role for a specific GL architecture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the framebuffer attachment is associated with.
 * Sub-types should be able to appropriately handle usage in conjunction with to any implementation of Framebuffer&lt;ContextType&gt;.
 * This could mean ensuring compatibility with any such implementation, and/or throwing an exception for implementations that are not compatible.
 */
public interface FramebufferAttachment<ContextType extends Context<ContextType>> extends Contextual<ContextType>
{

}
