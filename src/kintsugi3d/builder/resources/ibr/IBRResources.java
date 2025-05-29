/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.resources.ibr;

import java.util.List;

import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.resources.ibr.stream.GraphicsStreamFactory;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Resource;
import kintsugi3d.gl.geometry.GeometryResources;
import kintsugi3d.gl.geometry.ReadonlyVertexGeometry;
import kintsugi3d.gl.vecmath.Vector3;

public interface IBRResources<ContextType extends Context<ContextType>> extends Resource, ReadonlyIBRResources<ContextType>
{
    /**
     * The view set that these resources were loaded from.
     */
    @Override
    ViewSet getViewSet();

    @Override
    default ReadonlyVertexGeometry getGeometry()
    {
        return getGeometryResources().geometry;
    }

    /**
     * Gets a read-only view of the whole list of camera weights
     * @return
     */
    List<Float> getCameraWeights();

    GeometryResources<ContextType> getGeometryResources();

    /**
     * Diffuse, normal, specular, roughness maps
     * @return
     */
    SpecularMaterialResources<ContextType> getSpecularMaterialResources();

    /**
     * 1D textures for encoding and decoding
     * @return
     */
    LuminanceMapResources<ContextType> getLuminanceMapResources();

    /**
     * Refresh the luminance map in the view set and its corresponding textures.
     * @param linearLuminanceValues
     * @param encodedLuminanceValues
     */
    void updateLuminanceMap(double[] linearLuminanceValues, byte[] encodedLuminanceValues);


    /**
     * Refresh the light calibration in the view set and its corresponding uniform buffer data
     * @param lightCalibration
     */
    void updateLightCalibration(Vector3 lightCalibration);

    /**
     * Replace the specular material resources (textures); releasing the old resources if they were present
     * @param specularMaterialResources The new resources / textures
     */
    void replaceSpecularMaterialResources(SpecularMaterialResources<ContextType> specularMaterialResources);

    /**
     * Initialize any light intensities currently set to zero with the provided light intensity.
     * Non-zero light intensities (i.e. loaded from a file) will remain unchanged.
     * @param lightIntensity The default light intensity to apply to lights with an intensity of zero.
     * @param infiniteLightSources If true, light attenuation will be disabled; otherwise, light intensity will be
     *                             scaled by the square reciprocal of distance from light.
     */
    void initializeLightIntensities(Vector3 lightIntensity, boolean infiniteLightSources);

    @Override
    default GraphicsStreamFactory<ContextType> streamFactory()
    {
        return new GraphicsStreamFactory<>(this);
    }
}
