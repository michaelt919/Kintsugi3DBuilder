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
