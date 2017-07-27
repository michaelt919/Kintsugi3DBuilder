package tetzlaff.gl.builders;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.Shader;
import tetzlaff.gl.ShaderType;

import java.io.File;
import java.io.FileNotFoundException;

public interface ProgramBuilder<ContextType extends Context<ContextType>>
{
	ProgramBuilder<ContextType> addShader(Shader<ContextType> shader);
	ProgramBuilder<ContextType> addShader(ShaderType type, File shaderFile) throws FileNotFoundException;
	ProgramBuilder<ContextType> addShader(ShaderType type, String shaderSource);
	
	Program<ContextType> createProgram();
}
