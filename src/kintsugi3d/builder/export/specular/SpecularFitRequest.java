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

package kintsugi3d.builder.export.specular;

import kintsugi3d.builder.core.*;
import kintsugi3d.builder.export.specular.gltf.SpecularFitGltfExporter;
import kintsugi3d.builder.fit.SpecularFitProcess;
import kintsugi3d.builder.fit.SpecularFitProgramFactory;
import kintsugi3d.builder.fit.debug.FinalReconstruction;
import kintsugi3d.builder.fit.settings.SpecularFitRequestParams;
import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.builder.state.ReadonlyObjectModel;
import kintsugi3d.builder.util.Kintsugi3DViewerLauncher;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.geometry.ReadonlyVertexGeometry;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

public class SpecularFitRequest implements ObservableIBRRequest //, ObservableGraphicsRequest
{
    private static final Logger log = LoggerFactory.getLogger(SpecularFitRequest.class);
    private final SpecularFitRequestParams settings;

    /**
     * Default constructor for CLI args requests
     * @param modelAccess
     * @param args args[0] is the project name; args[1] is the name of this class; args[2] is the output directory
     * @return the request object
     */
    public static SpecularFitRequest create(
            Kintsugi3DBuilderState modelAccess, String... args)
    {
        SpecularFitRequestParams params = new SpecularFitRequestParams(
            new TextureResolution(2048, 2048),
            modelAccess.getSettingsModel());
        params.setGamma(modelAccess.getSettingsModel().getFloat("gamma"));
        params.setOutputDirectory(new File(args[2]));
        return new SpecularFitRequest(params, modelAccess);
    }

    public SpecularFitRequest(SpecularFitRequestParams settings, Kintsugi3DBuilderState modelAccess)
    {
        this.settings = settings;
    }

    /**
     * This version optimizes from scratch and requires IBR resources.
     * @param renderable The implementation of the Kintsugi 3D Builder renderer.
     *                   This can be used to dynamically generate renders of the current view,
     *                   or just to access the IBRResources and the graphics Context.
     * @param callback A callback that can be fired to update the loading bar.
     *                 If this is unused, an "infinite loading" indicator will be displayed instead.
     */
    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(IBRInstance<ContextType> renderable, LoadingMonitor callback)
    {
        try
        {
            // Set the output directory based on the view set's texture fit file path
            settings.setOutputDirectory(renderable.getActiveViewSet().getSupportingFilesFilePath());

            // Perform the specular fit
            new SpecularFitProcess(settings).optimizeFit(renderable.getIBRResources());

            // Perform reconstruction
            performReconstruction(renderable.getIBRResources(), renderable.getIBRResources().getSpecularMaterialResources());

            if (settings.getExportSettings().isGlTFEnabled())
            {
                saveGlTF(renderable.getActiveGeometry(), renderable.getActiveViewSet(), renderable.getSceneModel().getObjectModel());
            }

            if (settings.getExportSettings().isOpenViewerOnceComplete())
            {
                Kintsugi3DViewerLauncher.launchViewer(new File(settings.getOutputDirectory(), "model.glb"));
            }
        }
        catch(IOException e) // thrown by createReflectanceProgram
        {
            log.error("Error executing specular fit request:", e);
        }
    }

    private <ContextType extends Context<ContextType>> void performReconstruction(
        ReadonlyIBRResources<ContextType> resources, SpecularMaterialResources<ContextType> specularFit)
        throws FileNotFoundException
    {
        if (settings.getOutputDirectory() != null)
        {
            // Create output directory
            settings.getOutputDirectory().mkdirs();

            if (resources.getViewSet() != null)
            {
                // Reconstruct images both from basis functions and from fitted roughness
                SpecularFitProgramFactory<ContextType> programFactory = new SpecularFitProgramFactory<>(
                    settings.getIbrSettings(), settings.getSpecularBasisSettings());
                FinalReconstruction<ContextType> reconstruction =
                    new FinalReconstruction<>(resources, settings.getTextureResolution(), settings.getReconstructionSettings());

                log.info("Reconstructing ground truth images from basis representation:");
                double reconstructionRMSE =
                    reconstruction.reconstruct(specularFit, getImageReconstructionProgramBuilder(resources, programFactory),
                        settings.getReconstructionSettings().shouldReconstructAll(),
                        "reconstruction", "ground-truth", settings.getOutputDirectory());

                log.info("Reconstructing ground truth images from fitted roughness / specular color:");
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
        if (settings.getOutputDirectory() != null)
        {
            if (geometry == null)
            {
                throw new IllegalArgumentException("Geometry is null; cannot export GLTF.");
            }

            log.info("Starting glTF export...");

            try
            {
                Matrix4 rotation = viewSet == null ? Matrix4.IDENTITY : viewSet.getCameraPose(viewSet.getPrimaryViewIndex());
                Vector3 translation = rotation.getUpperLeft3x3().times(geometry.getCentroid().times(-1.0f));
                Matrix4 transform = Matrix4.fromColumns(rotation.getColumn(0), rotation.getColumn(1), rotation.getColumn(2), translation.asVector4(1.0f));

                transform = objectModel == null ? Matrix4.IDENTITY : objectModel.getTransformationMatrix().times(transform);

                SpecularFitGltfExporter exporter = SpecularFitGltfExporter.fromVertexGeometry(geometry, transform);
                exporter.setDefaultNames();
                exporter.addWeightImages(settings.getSpecularBasisSettings().getBasisCount(), settings.getExportSettings().isCombineWeights());

                // Add diffuse constant if requested
                if (settings.shouldIncludeConstantTerm())
                {
                    exporter.setDiffuseConstantUri("constant.png");
                }

                // Deal with LODs if enabled
                if (settings.getExportSettings().isGenerateLowResTextures())
                {
                    exporter.addAllDefaultLods(settings.getTextureResolution().height,
                        settings.getExportSettings().getMinimumTextureResolution());
                    exporter.addWeightImageLods(settings.getSpecularBasisSettings().getBasisCount(),
                        settings.getTextureResolution().height, settings.getExportSettings().getMinimumTextureResolution());

                    if (settings.shouldIncludeConstantTerm())
                    {
                        exporter.addDiffuseConstantLods("constant.png", settings.getTextureResolution().height,
                                settings.getExportSettings().getMinimumTextureResolution());
                    }
                }

                exporter.write(new File(settings.getOutputDirectory(), "model.glb"));
                log.info("DONE!");
            }
            catch (IOException e)
            {
                log.error("Error occurred during glTF export:", e);
            }
        }
    }
}
