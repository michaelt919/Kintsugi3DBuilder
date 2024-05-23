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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import kintsugi3d.builder.core.*;
import kintsugi3d.builder.fit.FinalReconstruction;
import kintsugi3d.builder.fit.ReconstructionShaders;
import kintsugi3d.builder.fit.SpecularFitProcess;
import kintsugi3d.builder.fit.SpecularFitProgramFactory;
import kintsugi3d.builder.fit.settings.SpecularFitRequestParams;
import kintsugi3d.builder.metrics.ColorAppearanceRMSE;
import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.builder.util.Kintsugi3DViewerLauncher;
import kintsugi3d.gl.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpecularFitRequest implements ObservableIBRRequest //, ObservableGraphicsRequest
{
    private static final Logger log = LoggerFactory.getLogger(SpecularFitRequest.class);
    private final SpecularFitRequestParams settings;

    private static final boolean DEBUG_IMAGES = false;

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
            new SpecularFitProcess(settings).optimizeFit(renderable.getIBRResources(), callback);

            // Perform reconstruction
            //performReconstruction(renderable.getIBRResources(), renderable.getIBRResources().getSpecularMaterialResources());

            if (settings.getExportSettings().isGlTFEnabled())
            {
                renderable.saveGlTF(settings.getOutputDirectory(), settings.getExportSettings());
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
        throws IOException
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

                log.info("Reconstructing:");
                List<Map<String, ColorAppearanceRMSE>> rmseList = reconstruction.reconstruct(specularFit, Map.of(
                        "basis", ReconstructionShaders.getBasisModelReconstructionProgramBuilder(resources, specularFit, programFactory),
                        "reflectivity", ReconstructionShaders.getReflectivityModelReconstructionProgramBuilder(resources, specularFit, programFactory)),
                    ReconstructionShaders.getIncidentRadianceProgramBuilder(resources, programFactory),
                    DEBUG_IMAGES ? settings.getOutputDirectory() : null,
                    DEBUG_IMAGES ? new File(settings.getOutputDirectory(), "ground-truth") : null);

                double reconstructionRMSE = rmseList.stream().mapToDouble(map ->
                    {
                        double rmse = map.get("basis").getEncodedGroundTruth();
                        return rmse * rmse; // mean of mean-squared errors
                    })
                    .average().orElse(0.0);

                double fittedRMSE = rmseList.stream().mapToDouble(map ->
                    {
                        double rmse = map.get("reflectivity").getEncodedGroundTruth();
                        return rmse * rmse; // mean of mean-squared errors
                    })
                    .average().orElse(0.0);

                if (!settings.getReconstructionSettings().shouldReconstructAll()) // Write to just one RMSE file if only doing a single image per reconstruction method
                {
                    try (PrintStream rmseOut = new PrintStream(new File(settings.getOutputDirectory(), "rmse.txt"), StandardCharsets.UTF_8))
                    // Text file containing error information
                    {
                        rmseOut.println("basis, " + reconstructionRMSE);
                        rmseOut.println("reflectivity, " + fittedRMSE);
                    }
                }
            }
        }
    }
}
