/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.material;

import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.vecmath.Vector3;

import java.io.File;
import java.io.IOException;

public interface ReadonlyMaterial
{
    String getName();

    Vector3 getAmbient();

    Vector3 getDiffuse();

    Vector3 getSpecular();

    float getExponent();

    float getRoughness();

    Vector3 getEmission();

    float getMetallic();

    Vector3 getSheen();

    Vector3 getClearcoat();

    float getClearcoatRoughness();

    float getOpacity();

    float getTransparency();

    float getTranslucency();

    ReadonlyMaterialColorMap getAmbientMap();

    ReadonlyMaterialColorMap getDiffuseMap();

    ReadonlyMaterialColorMap getSpecularMap();

    ReadonlyMaterialScalarMap getExponentMap();

    ReadonlyMaterialScalarMap getRoughnessMap();

    ReadonlyMaterialScalarMap getOpacityMap();

    ReadonlyMaterialScalarMap getTransparencyMap();

    ReadonlyMaterialScalarMap getTranslucencyMap();

    ReadonlyMaterialColorMap getEmissionMap();

    ReadonlyMaterialScalarMap getMetallicMap();

    ReadonlyMaterialColorMap getSheenMap();

    ReadonlyMaterialScalarMap getAnisotropyMap();

    ReadonlyMaterialScalarMap getAnisotropyRotationMap();

    ReadonlyMaterialBumpMap getBumpMap();

    ReadonlyMaterialTextureMap getNormalMap();

    ReadonlyMaterialScalarMap getDisplacementMap();

    ReadonlyMaterialScalarMap getAmbientOcclusionMap();

    <ContextType extends Context<ContextType>> MaterialResources<ContextType> createResources(
        ContextType context, File textureDirectory, TextureLoadOptions loadOptions) throws IOException;
}
