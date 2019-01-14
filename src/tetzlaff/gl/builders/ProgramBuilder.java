package tetzlaff.gl.builders;

import java.io.File;
import java.io.FileNotFoundException;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Program;
import tetzlaff.gl.core.Shader;
import tetzlaff.gl.core.ShaderType;

public interface ProgramBuilder<ContextType extends Context<ContextType>>
{
    ProgramBuilder<ContextType> define(String key, Object value);
    ProgramBuilder<ContextType> addShader(Shader<ContextType> shader);
    ProgramBuilder<ContextType> addShader(ShaderType type, File shaderFile);
    ProgramBuilder<ContextType> addShader(ShaderType type, String shaderSource);

    Program<ContextType> createProgram() throws FileNotFoundException;
}