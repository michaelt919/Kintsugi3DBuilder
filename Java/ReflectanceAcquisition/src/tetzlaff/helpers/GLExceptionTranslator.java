package tetzlaff.helpers;

import java.io.FileNotFoundException;

import tetzlaff.gl.exceptions.GLException;

/**
 * An exception translator designed for graphics applications.
 * Errors are classified as either graphics/GL, resource (File I/O), or general/application errors.
 * @author Michael Tetzlaff
 *
 */
public class GLExceptionTranslator implements ExceptionTranslator 
{
	@Override
	public ErrorMessage translate(Exception e) 
	{
		if(e instanceof GLException || (e.getCause() != null && e.getCause() instanceof GLException))
		{
			return new ErrorMessage("GL Rendering Error", "An error occured with the rendering system. " +
					"Your GPU and/or video memory may be insufficient for rendering this model.\n\n[" +
					e.getMessage() + "]");
		}
		else if(e instanceof FileNotFoundException || (e.getCause() != null && e.getCause() instanceof FileNotFoundException))
		{
			return new ErrorMessage("Resource Error", "An error occured while loading resources. " +
					"Check that all necessary files exist and that the proper paths were supplied.\n\n[" +
					e.getMessage() + "]");
		}
		else
		{
			return new ErrorMessage("Application Error", "An error occured that prevents this model from being rendered." +
					"\n\n[" + e.getMessage() + "]");
		}
	}
	
}
