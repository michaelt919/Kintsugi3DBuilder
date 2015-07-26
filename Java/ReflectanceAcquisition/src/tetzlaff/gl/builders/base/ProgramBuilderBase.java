package tetzlaff.gl.builders.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import tetzlaff.gl.Context;
import tetzlaff.gl.Shader;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.builders.ProgramBuilder;

public abstract class ProgramBuilderBase<ContextType extends Context<ContextType>> implements ProgramBuilder<ContextType> 
{
	protected final ContextType context;
	private final List<Shader<ContextType>> shaders;
	
	protected ProgramBuilderBase(ContextType context)
	{
		this.context = context;
		this.shaders = new ArrayList<Shader<ContextType>>();
	}
	
	protected Iterable<Shader<ContextType>> getShaders()
	{
		return this.shaders;
	}
	
	@Override
	public ProgramBuilder<ContextType> addShader(Shader<ContextType> shader)
	{
		shaders.add(shader);
		return this;
	}
	
	@Override
	public ProgramBuilder<ContextType> addShader(ShaderType type, File shaderFile) throws FileNotFoundException
	{
		shaders.add(context.createShader(type, shaderFile));
		return this;
	}
	
	@Override
	public ProgramBuilder<ContextType> addShader(ShaderType type, String shaderSource)
	{
		shaders.add(context.createShader(type, shaderSource));
		return this;
	}
}
