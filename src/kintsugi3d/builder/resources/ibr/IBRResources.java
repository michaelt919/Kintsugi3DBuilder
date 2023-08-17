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

package kintsugi3d.builder.resources.ibr;

import java.util.List;

import kintsugi3d.builder.resources.ibr.stream.GraphicsStreamFactory;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.geometry.GeometryResources;
import kintsugi3d.gl.geometry.ReadonlyVertexGeometry;
import kintsugi3d.gl.material.GenericMaterialResources;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.builder.core.StandardRenderingMode;
import kintsugi3d.builder.core.ViewSet;

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
    GenericMaterialResources<ContextType> getMaterialResources();

    /**
     * 1D textures for encoding and decoding
     * @return
     */
    LuminanceMapResources<ContextType> getLuminanceMapResources();

    /**
     * Refresh the luminance map in the view set and its corresponding textures.
     */
    void updateLuminanceMap(double[] linearLuminanceValues, byte[] encodedLuminanceValues);


    /**
     * Refresh the light calibration in the view set and its corresponding uniform buffer data
     */
    void updateLightCalibration(Vector3 lightCalibration);

    /**
     * Initialize any light intensities currently set to zero with the provided light intensity.
     * Non-zero light intensities (i.e. loaded from a file) will remain unchanged.
     * @param lightIntensity The default light intensity to apply to lights with an intensity of zero.
     * @param infiniteLightSources If true, light attenuation will be disabled; otherwise, light intensity will be
     *                             scaled by the square reciprocal of distance from light.
     */
    void initializeLightIntensities(Vector3 lightIntensity, boolean infiniteLightSources);

    /**
     * Gets a shader program builder with preprocessor defines automatically injected based on the
     * characteristics of this instance.
     * This overload uses the default mode of RenderingMode.IMAGE_BASED.
     * @return A program builder with preprocessor defines specified, ready to have the vertex and fragment shaders
     * added as well as any additional application-specific preprocessor definitions.
     */
    @Override
    default ProgramBuilder<ContextType> getShaderProgramBuilder()
    {
        return getShaderProgramBuilder(StandardRenderingMode.IMAGE_BASED);
    }

    @Override
    default GraphicsStreamFactory<ContextType> streamFactory()
    {
        return new GraphicsStreamFactory<>(this);
    }
}
