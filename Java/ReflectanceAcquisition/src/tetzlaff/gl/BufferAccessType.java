package tetzlaff.gl;

/**
 * Enumerates the possible ways in which a buffer could be read or written to with respect to the application or the shaders.
 * @author Michael Tetzlaff
 *
 */
public enum BufferAccessType 
{
	/**
	 * The buffer will be primarily written to by the application and read by shaders.
	 */
	DRAW,
	
	/**
	 * The buffer will be primarily written to by shaders and read by the application.
	 */
	READ,
	
	/**
	 * The buffer will be primarily both written to and read from shaders.
	 */
	COPY
}
