package tetzlaff.gl.builders.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Shader;
import tetzlaff.gl.core.ShaderType;

public abstract class ProgramBuilderBase<ContextType extends Context<ContextType>> implements ProgramBuilder<ContextType> 
{
    @FunctionalInterface
    private interface ShaderSupplier<ContextType extends Context<ContextType>>
    {
        Shader<ContextType> get() throws FileNotFoundException;
    }

    protected final ContextType context;
    private final List<ShaderSupplier<ContextType>> shaderBuilders;

    private final Map<String, Object> defines;

    protected ProgramBuilderBase(ContextType context)
    {
        this.context = context;
        this.shaderBuilders = new ArrayList<>(2);
        this.defines = new HashMap<>(16);
    }

    protected Iterable<Shader<ContextType>> compileShaders() throws FileNotFoundException
    {
        Collection<Shader<ContextType>> shaders = new ArrayList<>(shaderBuilders.size());
        for (ShaderSupplier<ContextType> shaderBuilder : shaderBuilders)
        {
            shaders.add(shaderBuilder.get());
        }
        return shaders;
    }

    @Override
    public ProgramBuilder<ContextType> define(String key, Object value)
    {
        defines.put(key, value);
        return this;
    }

    @Override
    public ProgramBuilder<ContextType> addShader(Shader<ContextType> shader)
    {
        shaderBuilders.add(() -> shader);
        return this;
    }

    @Override
    public ProgramBuilder<ContextType> addShader(ShaderType type, File shaderFile)
    {
        shaderBuilders.add(() -> context.createShader(type, shaderFile, new HashMap<>(defines)) );

        return this;
    }

    @Override
    public ProgramBuilder<ContextType> addShader(ShaderType type, String shaderSource)
    {
        shaderBuilders.add(() -> context.createShader(type, shaderSource));
        return this;
    }

    public Map<String, Object> getDefines()
    {
        return Collections.unmodifiableMap(defines);
    }
}
