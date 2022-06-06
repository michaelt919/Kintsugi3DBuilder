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

package tetzlaff.ibrelight.export.specularfit;

import java.io.*;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.*;

public class SpecularFitRequest<ContextType extends Context<ContextType>> implements IBRRequest<ContextType>
{
    private final SpecularFitSettings settings;

    public SpecularFitRequest(SpecularFitSettings settings)
    {
        this.settings = settings;
    }

    @Override
    public void executeRequest(IBRInstance<ContextType> renderable, LoadingMonitor callback)
    {
        try
        {
            // Perform the specular fit
            SpecularFit specularFit = new SpecularOptimization(settings).createFit(renderable.getIBRResources());

            // Reconstruct images both from basis functions and from fitted roughness
            SpecularFitProgramFactory<ContextType> programFactory = new SpecularFitProgramFactory<>(renderable.getIBRResources(), settings);
            FinalReconstruction<ContextType> reconstruction = new FinalReconstruction<>(renderable.getIBRResources(), settings);
            reconstruction.reconstruct(specularFit, getImageReconstructionProgramBuilder(programFactory), "reconstructions");
            reconstruction.reconstruct(specularFit, getFittedImageReconstructionProgramBuilder(programFactory), "fitted");

            specularFit.close(); // Close immediately when this is just an export operation.
        }
        catch(IOException e) // thrown by createReflectanceProgram
        {
            e.printStackTrace();
        }
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getImageReconstructionProgramBuilder(SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
                new File("shaders/common/imgspace.vert"),
                new File("shaders/specularfit/reconstructImage.frag"));
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getFittedImageReconstructionProgramBuilder(SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
                new File("shaders/common/imgspace.vert"),
                new File("shaders/specularfit/renderFit.frag"));
    }
}
