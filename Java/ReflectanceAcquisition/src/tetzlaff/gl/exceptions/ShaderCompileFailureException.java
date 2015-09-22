package tetzlaff.gl.exceptions;

/**
 * Thrown when a shader fails to compile.
 * @author Michael Tetzlaff
 *
 */
public class ShaderCompileFailureException extends RuntimeException 
{
	private static final long serialVersionUID = 7556469381337373536L;

	public ShaderCompileFailureException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ShaderCompileFailureException(String arg0, Throwable arg1, boolean arg2,
			boolean arg3) {
		super(arg0, arg1, arg2, arg3);
		// TODO Auto-generated constructor stub
	}

	public ShaderCompileFailureException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public ShaderCompileFailureException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public ShaderCompileFailureException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
