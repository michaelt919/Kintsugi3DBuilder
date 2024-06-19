/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit;

import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.Context;

import java.io.File;

public final class ReconstructionShaders
{
    private ReconstructionShaders()
    {
    }

    public static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getIncidentRadianceProgramBuilder(
            ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(resources,
            new File("shaders/common/imgspace.vert"),
            new File("shaders/specularfit/incidentRadiance.frag"));
    }

    public static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getBasisModelReconstructionProgramBuilder(
            ReadonlyIBRResources<ContextType> resources, SpecularMaterialResources<ContextType> specularFit,
            SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(resources,
                new File("shaders/common/imgspace.vert"),
                new File("shaders/specularfit/reconstruction/basisModel.frag"))
            .define("USE_CONSTANT_MAP", specularFit.getConstantMap() != null);
    }

    public static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getReflectivityModelReconstructionProgramBuilder(
            ReadonlyIBRResources<ContextType> resources, SpecularMaterialResources<ContextType> specularFit,
            SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(resources,
                new File("shaders/common/imgspace.vert"),
                new File("shaders/specularfit/reconstruction/reflectivityModel.frag"))
            .define("USE_CONSTANT_MAP", specularFit.getConstantMap() != null);
    }
}
