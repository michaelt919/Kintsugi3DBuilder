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

import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture2D;

public class SimpleSpecularMaterialResources<ContextType extends Context<ContextType>>
    extends SpecularMaterialResourcesBase<ContextType>
{
    private Texture2D<ContextType> diffuseMap;
    private Texture2D<ContextType> normalMap;
    private Texture2D<ContextType> constantMap;
//    private Texture2D<ContextType> quadraticMap;
    private Texture2D<ContextType> specularReflectivityMap;
    private Texture2D<ContextType> specularRoughnessMap;
    private Texture2D<ContextType> albedoMap;
    private Texture2D<ContextType> ormMap;
    private BasisResources<ContextType> basisResources;
    private BasisWeightResources<ContextType> basisWeightResources;

    private final ContextType context;

    /**
     * Package-visible default constructor
     */
    SimpleSpecularMaterialResources(ContextType context)
    {
        this.context = context;
    }

    @Override
    public ContextType getContext()
    {
        return context;
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
    public Texture2D<ContextType> getConstantMap()
    {
        return constantMap;
    }

//    @Override
//    public Texture2D<ContextType> getQuadraticMap()
//    {
//        return quadraticMap;
//    }

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
    public Texture2D<ContextType> getAlbedoMap()
    {
        return albedoMap;
    }

    @Override
    public Texture2D<ContextType> getORMMap()
    {
        return ormMap;
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
     * Sets the constant texture.  This object will take ownership of the normal map.
     * @param constantMap
     */
    public void setConstantMap(Texture2D<ContextType> constantMap)
    {
        this.constantMap = constantMap;
    }

//    /**
//     * Sets the quadratic texture.  This object will take ownership of the normal map.
//     * @param quadraticMap
//     */
//    public void setQuadraticMap(Texture2D<ContextType> quadraticMap)
//    {
//        this.quadraticMap = quadraticMap;
//    }

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

    public void setAlbedoMap(Texture2D<ContextType> albedoMap)
    {
        this.albedoMap = albedoMap;
    }

    public void setORMMap(Texture2D<ContextType> ormMap)
    {
        this.ormMap = ormMap;
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
        if (diffuseMap != null)
        {
            diffuseMap.close();
        }

        if (normalMap != null)
        {
            normalMap.close();
        }

        if (constantMap != null)
        {
            constantMap.close();
        }

//        if (quadraticMap != null)
//        {
//            quadraticMap.close();
//        }

        if (specularReflectivityMap != null)
        {
            specularReflectivityMap.close();
        }

        if (specularRoughnessMap != null)
        {
            specularRoughnessMap.close();
        }

        if (albedoMap != null)
        {
            albedoMap.close();
        }

        if (ormMap != null)
        {
            ormMap.close();
        }

        if (basisResources != null)
        {
            basisResources.close();
        }

        if (basisWeightResources != null)
        {
            basisWeightResources.close();
        }
    }
}
