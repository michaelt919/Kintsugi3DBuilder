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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.geometry.ReadonlyVertexGeometry;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.*;
import tetzlaff.ibrelight.export.specularfit.gltf.SpecularFitGltfExporter;
import tetzlaff.ibrelight.rendering.resources.IBRResources;
import tetzlaff.ibrelight.rendering.resources.IBRResourcesImageSpace;
import tetzlaff.ibrelight.rendering.resources.ReadonlyIBRResources;
import tetzlaff.interactive.GraphicsRequest;
import tetzlaff.models.ReadonlyObjectModel;

public class SpecularFitRequest<ContextType extends Context<ContextType>> implements IBRRequest<ContextType>, GraphicsRequest<ContextType>
{
    private final SpecularFitRequestParams settings;

    /**
     * Default constructor for CLI args requests
     * @param modelAccess
     * @param args args[0] is the project name; args[1] is the name of this class; args[2] is the output directory
     * @return the request object
     */
    public static <ContextType extends Context<ContextType>> SpecularFitRequest<ContextType> create(
            IBRelightModels modelAccess, String... args)
    {
        return new SpecularFitRequest<>(new SpecularFitRequestParams(
            new TextureFitSettings(2048, 2048, modelAccess.getSettingsModel().getFloat("gamma")),
            modelAccess.getSettingsModel(), new File(args[2])));
    }

    public SpecularFitRequest(SpecularFitRequestParams settings)
    {
        this.settings = settings;
    }

    /**
     * This version loads a prior solution from file and thus doesn't require IBR resources to be loaded.
     * @param context The graphics context to be used.
     * @param callback A callback that can be fired to update the loading bar.
     *                 If this is unused, an "infinite loading" indicator will be displayed instead.
     */
    @Override
    public void executeRequest(ContextType context, LoadingMonitor callback)
    {
        try
        {
            // Perform the specular fit using prior basis solution.
            SpecularResources<ContextType> specularFit;

            // Assume fitting from prior solution
            System.out.println("No IBRelight project loaded; loading prior solution");
            specularFit = new SpecularOptimization(settings).loadPriorSolution(context, settings.getPriorSolutionDirectory());

            // Load just geometry, tonemapping, settings.
            SimpleLoadOptionsModel loadOptions = new SimpleLoadOptionsModel();
            loadOptions.setColorImagesRequested(false);
            loadOptions.setDepthImagesRequested(false);

            try(IBRResources<ContextType> resources = IBRResourcesImageSpace.getBuilderForContext(context)
                .setLoadOptions(loadOptions)
                .useExistingViewSet(settings.getReconstructionSettings().getReconstructionViewSet().copy())
                .create())
            {
                if (settings.getExportSettings().isGlTFEnabled())
                {
                    saveGlTF(resources.getGeometry(), resources.getViewSet(), null);
                }

                // Reconstruct if requested
                performReconstruction(resources, specularFit);

                // Close specular fit immediately when this is just an export operation.
                specularFit.close();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * This version optimizes from scratch and requires IBR resources.
     * @param renderable The implementation of the IBRelight renderer.
     *                   This can be used to dynamically generate renders of the current view,
     *                   or just to access the IBRResources and the graphics Context.
     * @param callback A callback that can be fired to update the loading bar.
     *                 If this is unused, an "infinite loading" indicator will be displayed instead.
     */
    @Override
    public void executeRequest(IBRInstance<ContextType> renderable, LoadingMonitor callback)
    {
        try
        {
            // Perform the specular fit
            SpecularResources<ContextType> specularFit = new SpecularOptimization(settings)
                .optimizeFit(renderable.getIBRResources());

            if (settings.getExportSettings().isGlTFEnabled())
            {
                saveGlTF(renderable.getActiveGeometry(), renderable.getActiveViewSet(), renderable.getSceneModel().getObjectModel());
            }

            // Reconstruct if requested
            performReconstruction(renderable.getIBRResources(), specularFit);

            // Close immediately when this is just an export operation.
            specularFit.close();
        }
        catch(IOException e) // thrown by createReflectanceProgram
        {
            e.printStackTrace();
        }
    }

    private void performReconstruction(ReadonlyIBRResources<ContextType> resources, SpecularResources<ContextType> specularFit)
        throws FileNotFoundException
    {
        // Create output directory
        settings.getOutputDirectory().mkdirs();

        if (resources.getViewSet() != null)
        {
            // Reconstruct images both from basis functions and from fitted roughness
            SpecularFitProgramFactory<ContextType> programFactory = new SpecularFitProgramFactory<>(
               settings.getIbrSettings(), settings.getSpecularBasisSettings());
            FinalReconstruction<ContextType> reconstruction =
                new FinalReconstruction<>(resources, settings.getTextureFitSettings(), settings.getReconstructionSettings());

            System.out.println("Reconstructing ground truth images from basis representation:");
            double reconstructionRMSE =
                reconstruction.reconstruct(specularFit, getImageReconstructionProgramBuilder(resources, programFactory),
                    settings.getReconstructionSettings().shouldReconstructAll(),
                    "reconstruction", "ground-truth", settings.getOutputDirectory());

            System.out.println("Reconstructing ground truth images from fitted roughness / specular color:");
            double fittedRMSE =
                reconstruction.reconstruct(specularFit, getFittedImageReconstructionProgramBuilder(resources, programFactory),
                    settings.getReconstructionSettings().shouldReconstructAll(),
                    "fitted", null, settings.getOutputDirectory());

            if (!settings.getReconstructionSettings().shouldReconstructAll()) // Write to just one RMSE file if only doing a single image per reconstruction method
            {
                try (PrintStream rmseOut = new PrintStream(new File(settings.getOutputDirectory(), "rmse.txt")))
                // Text file containing error information
                {
                    rmseOut.println("reconstruction, " + reconstructionRMSE);
                    rmseOut.println("fitted, " + fittedRMSE);
                }
            }
        }
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getImageReconstructionProgramBuilder(
        ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(resources,
                new File("shaders/common/imgspace.vert"),
                new File("shaders/specularfit/reconstructImage.frag"));
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getFittedImageReconstructionProgramBuilder(
        ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(resources,
                new File("shaders/common/imgspace.vert"),
                new File("shaders/specularfit/renderFit.frag"));
    }

    public void saveGlTF(ReadonlyVertexGeometry geometry, ReadonlyViewSet viewSet, ReadonlyObjectModel objectModel)
    {
        if (geometry == null)
        {
            throw new IllegalArgumentException("Geometry is null; cannot export GLTF.");
        }

        System.out.println("Starting glTF export...");

        try
        {
            Matrix4 rotation = viewSet == null ? Matrix4.IDENTITY : viewSet.getCameraPose(viewSet.getPrimaryViewIndex());
            Vector3 translation = rotation.getUpperLeft3x3().times(geometry.getCentroid().times(-1.0f));
            Matrix4 transform = Matrix4.fromColumns(rotation.getColumn(0), rotation.getColumn(1), rotation.getColumn(2), translation.asVector4(1.0f));

            SpecularFitGltfExporter exporter = SpecularFitGltfExporter.fromVertexGeometry(geometry, transform);
            exporter.setDefaultNames();
            exporter.addWeightImages(settings.getSpecularBasisSettings().getBasisCount(), settings.getExportSettings().isCombineWeights());

            // Deal with LODs if enabled
            if (settings.getExportSettings().isGenerateLowResTextures())
            {
                exporter.addAllDefaultLods(settings.getTextureFitSettings().height,
                    settings.getExportSettings().getMinimumTextureResolution());
                exporter.addWeightImageLods(settings.getSpecularBasisSettings().getBasisCount(),
                    settings.getTextureFitSettings().height, settings.getExportSettings().getMinimumTextureResolution());
            }

            exporter.write(new File(settings.getOutputDirectory(), "model.glb"));
            System.out.println("DONE!");
        }
        catch (IOException e)
        {
            System.out.println("Error occurred during glTF export:");
            e.printStackTrace();
        }
    }
}
