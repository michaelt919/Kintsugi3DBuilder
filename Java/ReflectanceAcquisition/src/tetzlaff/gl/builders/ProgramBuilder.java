package tetzlaff.gl.builders;

import java.io.File;
import java.io.FileNotFoundException;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.Shader;
import tetzlaff.gl.ShaderType;

/**
 * Implements the builder design pattern for creating shader programs.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the program will be associated with.
 */
public interface ProgramBuilder<ContextType extends Context<ContextType>>
{
	/**
	 * Adds a shader to the program.
	 * @param shader The shader to add.
	 * @return The calling builder object.
	 */
	ProgramBuilder<ContextType> addShader(Shader<ContextType> shader);
	
	/**
	 * Creates and adds a new shader to the program.
	 * @param type The type of shader to add.
	 * @param shaderFile A file containing the source code for the shader.
	 * @return The calling builder object.
	 * @throws FileNotFoundException Upon a File I/O problem when reading the shader file.
	 */
	ProgramBuilder<ContextType> addShader(ShaderType type, File shaderFile) throws FileNotFoundException;
	
	/**
	 * Creates and adds a new shader to the program.
	 * @param type The type of shader to add.
	 * @param shaderSource A string containing the source code for the shader.
	 * @return The calling builder object.
	 */
	ProgramBuilder<ContextType> addShader(ShaderType type, String shaderSource);
	
	/**
	 * Creates the shader program.
	 * @return The newly created shader program.
	 */
	Program<ContextType> createProgram();
}
