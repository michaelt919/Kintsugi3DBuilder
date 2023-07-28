/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.builders.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Shader;
import kintsugi3d.gl.core.ShaderType;

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
