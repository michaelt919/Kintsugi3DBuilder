/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Texture2D;
import tetzlaff.ibrelight.rendering.resources.ImageCache;

public class SimpleSpecularResources<ContextType extends Context<ContextType>> implements SpecularResources<ContextType>
{
    private Texture2D<ContextType> diffuseMap;
    private Texture2D<ContextType> normalMap;
    private Texture2D<ContextType> specularReflectivityMap;
    private Texture2D<ContextType> specularRoughnessMap;
    private BasisResources<ContextType> basisResources;
    private BasisWeightResources<ContextType> basisWeightResources;

    /**
     * Package-visible default constructor
     */
    SimpleSpecularResources()
    {
    }

    @Override
    public int getWidth()
    {
        // TODO validation that textures all have the same resolution?
        return basisWeightResources.weightMaps.getWidth();
    }

    @Override
    public int getHeight()
    {
        // TODO validation that textures all have the same resolution?
        return basisWeightResources.weightMaps.getHeight();
    }

    @Override
    public Texture2D<ContextType> getDiffuseMap()
    {
        return diffuseMap;
    }

    @Override
    public Texture2D<ContextType> getNormalMap()
    {
        return normalMap;
    }

    @Override
    public Texture2D<ContextType> getSpecularReflectivityMap()
    {
        return specularReflectivityMap;
    }

    @Override
    public Texture2D<ContextType> getSpecularRoughnessMap()
    {
        return specularRoughnessMap;
    }

    @Override
    public BasisResources<ContextType> getBasisResources()
    {
        return basisResources;
    }


    @Override
    public BasisWeightResources<ContextType> getBasisWeightResources()
    {
        return basisWeightResources;
    }

    public SpecularResources<ContextType> createBlockResources(ImageCache<ContextType> imageCache, int i, int j)
    {
        SimpleSpecularResources<ContextType> blockResources = new SimpleSpecularResources<>();
        blockResources.diffuseMap = imageCache.cropForBlock(diffuseMap, i, j);
        blockResources.normalMap = imageCache.cropForBlock(normalMap, i, j);
        blockResources.specularReflectivityMap = imageCache.cropForBlock(specularReflectivityMap, i, j);
        blockResources.specularRoughnessMap = imageCache.cropForBlock(specularRoughnessMap, i, j);
        blockResources.basisWeightResources = imageCache.cropForBlock(basisWeightResources, i, j);
        return blockResources;
    }

    /**
     * Sets the diffuse texture.  This object will take ownership of the diffuse map.
     * @param diffuseMap
     */
    public void setDiffuseMap(Texture2D<ContextType> diffuseMap)
    {
        this.diffuseMap = diffuseMap;
    }

    /**
     * Sets the normal texture.  This object will take ownership of the normal map.
     * @param normalMap
     */
    public void setNormalMap(Texture2D<ContextType> normalMap)
    {
        this.normalMap = normalMap;
    }

    /**
     * Sets the specular reflectivity texture.  This object will take ownership of the specular map.
     * @param specularReflectivityMap
     */
    public void setSpecularReflectivityMap(Texture2D<ContextType> specularReflectivityMap)
    {
        this.specularReflectivityMap = specularReflectivityMap;
    }

    /**
     * Sets the specular roughness texture.  This object will take ownership of the roughness map.
     * @param specularRoughnessMap
     */
    public void setSpecularRoughnessMap(Texture2D<ContextType> specularRoughnessMap)
    {
        this.specularRoughnessMap = specularRoughnessMap;
    }

    public void setBasisResources(BasisResources<ContextType> basisResources)
    {
        this.basisResources = basisResources;
    }

    /**
     * Sets the basis resources (i.e. weight texture array, basis function 1D texture array, etc.).
     * This object will take ownership of the basis resources.
     * @param basisWeightResources
     */
    public void setBasisWeightResources(BasisWeightResources<ContextType> basisWeightResources)
    {
        this.basisWeightResources = basisWeightResources;
    }

    @Override
    public void close()
    {
        diffuseMap.close();
        normalMap.close();
        specularReflectivityMap.close();
        specularRoughnessMap.close();
        basisResources.close();
        basisWeightResources.close();
    }
}
