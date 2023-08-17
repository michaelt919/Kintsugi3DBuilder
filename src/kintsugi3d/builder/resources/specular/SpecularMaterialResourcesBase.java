/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.resources.specular;

import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.core.SamplerType;
import kintsugi3d.gl.core.Texture;

public abstract class SpecularMaterialResourcesBase<ContextType extends Context<ContextType>>
    implements SpecularMaterialResources<ContextType>
{
    private static <ContextType extends Context<ContextType>> void useTextureSafe(
        Program<ContextType> program, String textureName, Texture<ContextType> texture)
    {
        if (texture == null)
        {
            program.setTexture(textureName, program.getContext().getTextureFactory().getNullTexture(SamplerType.FLOAT_2D));
        }
        else
        {
            program.setTexture(textureName, texture);
        }
    }

    @Override
    public void setupShaderProgram(Program<ContextType> program)
    {
        useTextureSafe(program, "diffuseMap", this.getDiffuseMap());
        useTextureSafe(program, "constantMap", this.getConstantMap());
        useTextureSafe(program, "normalMap", this.getNormalMap());
        useTextureSafe(program, "specularMap", this.getSpecularReflectivityMap());
        useTextureSafe(program, "roughnessMap", this.getSpecularRoughnessMap());
        useTextureSafe(program, "occlusionMap", this.getOcclusionMap());
        useTextureSafe(program, "albedoMap", this.getAlbedoMap());
        useTextureSafe(program, "ormMap", this.getORMMap());
        useTextureSafe(program, "weightMaps", this.getBasisWeightResources().weightMaps);
    }
}
