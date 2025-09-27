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

package kintsugi3d.builder.fit;

import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.core.*;
import kintsugi3d.builder.core.metrics.ColorAppearanceRMSE;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.settings.BasisSettings;
import kintsugi3d.builder.fit.settings.SpecularFitSettings;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace;
import kintsugi3d.builder.resources.project.ReadonlyGraphicsResources;
import kintsugi3d.builder.resources.project.specular.SpecularMaterialResources;
import kintsugi3d.builder.state.cards.TabsManager;
import kintsugi3d.builder.state.project.ProjectModel;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;
import kintsugi3d.builder.util.Kintsugi3DViewerLauncher;
import kintsugi3d.gl.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class SpecularFitRequest implements ObservableProjectGraphicsRequest
{
    private static final Logger LOG = LoggerFactory.getLogger(SpecularFitRequest.class);
    private final SpecularFitSettings settings;

    private static final boolean DEBUG_IMAGES = false;

    public SpecularFitRequest(SpecularFitSettings settings)
    {
        this.settings = settings;
    }

    /**
     * Default constructor for CLI args requests
     *
     * @param args args[0] is the project name; args[1] is the name of this class; args[2] is the output directory
     * @return the request object
     */
    public static SpecularFitRequest create(String... args)
    {
        SpecularFitSettings params = new SpecularFitSettings(2048, 2048);
        params.setOutputDirectory(new File(args[2]));
        return new SpecularFitRequest(params);
    }

    public SpecularFitRequest()
    {
        this(getSettingsFromProject());
    }

    private static SpecularFitSettings getSettingsFromProject()
    {
        GeneralSettingsModel projectSettings = Global.state().getIOModel()
            .validateHandler()
            .getLoadedViewSet().getProjectSettings();

        // Start with texture size
        int textureSize = projectSettings.getInt("textureSize");
        SpecularFitSettings settings = new SpecularFitSettings(textureSize, textureSize);

        // Basis settings
        int basisResolution = projectSettings.getInt("basisResolution");
        settings.getSpecularBasisSettings().setBasisResolution(basisResolution);
        settings.getSpecularBasisSettings().setBasisCount(projectSettings.getInt("basisCount"));
        settings.getSpecularBasisSettings().setSmithMaskingShadowingEnabled(projectSettings.getBoolean("smithMaskingShadowingEnabled"));

        // Specular / general settings
        int specularMinWidthDiscrete = Math.round(projectSettings.getFloat("specularMinWidthFrac") * basisResolution);
        settings.getSpecularBasisSettings().setSpecularMinWidth(specularMinWidthDiscrete);
        settings.getSpecularBasisSettings().setSpecularMaxWidth(
            Math.round(projectSettings.getFloat("specularMaxWidthFrac") * basisResolution));
        settings.getSpecularBasisSettings().setBasisComplexity(
            Math.round(projectSettings.getFloat("basisComplexityFrac") * (basisResolution - specularMinWidthDiscrete + 1)));
        settings.setConvergenceTolerance(projectSettings.getFloat("convergenceTolerance"));
        settings.getSpecularBasisSettings().setMetallicity(projectSettings.getFloat("metallicity"));
        settings.setShouldIncludeConstantTerm(projectSettings.getBoolean("constantTermEnabled"));

        // Normal estimation settings
        settings.getNormalOptimizationSettings().setNormalRefinementEnabled(projectSettings.getBoolean("normalOptimizationEnabled"));
        settings.getNormalOptimizationSettings().setMinNormalDamping(projectSettings.getFloat("minNormalDamping"));
        settings.getNormalOptimizationSettings().setNormalSmoothingIterations(projectSettings.getInt("normalSmoothIterations"));
        settings.getNormalOptimizationSettings().setUnsuccessfulLMIterationsAllowed(projectSettings.getInt("unsuccessfulLMIterationsAllowed"));

        // Settings which shouldn't aren't currently exposed.
        settings.getNormalOptimizationSettings().setLevenbergMarquardtEnabled(true);
        settings.getReconstructionSettings().setReconstructAll(false);

        // glTF export settings
        settings.getExportSettings().setShouldSaveModel(true);
        settings.getExportSettings().setShouldCombineWeights(true);
        settings.getExportSettings().setShouldOpenViewerOnceComplete(projectSettings.getBoolean("openViewerOnProcessingComplete"));

        // Image cache settings
        settings.getImageCacheSettings().setCacheParentDirectory(ApplicationFolders.getFitCacheRootDirectory().toFile());

        return settings;
    }

    public SpecularFitSettings getSettings()
    {
        return settings;
    }

    /**
     * This version optimizes from scratch and requires project graphics resources.
     *
     * @param renderable The implementation of the Kintsugi 3D Builder renderer.
     *                   This can be used to dynamically generate renders of the current view,
     *                   or just to access the GraphicsResources and the graphics Context.
     * @param monitor    A callback that can be fired to update the loading bar.
     *                   If this is unused, an "infinite loading" indicator will be displayed instead.
     */
    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(ProjectInstance<ContextType> renderable, ProgressMonitor monitor)
        throws UserCancellationException
    {
        try
        {
            // Set the output directory based on the view set's texture fit file path
            settings.setOutputDirectory(renderable.getActiveViewSet().getSupportingFilesDirectory());

            if (monitor != null)
            {
                monitor.setProcessName("Process Textures");
            }

            // Perform the specular fit
            SpecularFitProcess process = new SpecularFitProcess(settings);
            GraphicsResourcesImageSpace<ContextType> resources = renderable.getResources();

            if (settings.shouldOptimizeBasis())
            {
                process.optimizeFitWithCache(resources, monitor);
            }
            else
            {
                BasisSettings basisSettings = settings.getSpecularBasisSettings();
                BasisResources<ContextType> basisResources = resources.getSpecularMaterialResources().getBasisResources();
                basisSettings.setBasisCount(basisResources.getBasisCount());
                basisSettings.setBasisResolution(basisResources.getBasisResolution());

                process.reoptimizeTexturesWithCache(resources, monitor);
            }

            // Reload shaders in case preprocessor constants (i.e. number of basis functions) have changed
            renderable.reloadShaders();

            // Quietly (no confirmation modal) saves textures
            // as well as glTF (for Kintsugi 3D Viewer) and project to avoid inconsistency between results and settings
            Global.state().getIOModel().saveAll();

            // Perform reconstruction
            //performReconstruction(renderable.getGraphicsResources(), renderable.getGraphicsResources().getSpecularMaterialResources());

            if (settings.getExportSettings().shouldOpenViewerOnceComplete())
            {
                Kintsugi3DViewerLauncher.launchViewer(new File(settings.getOutputDirectory(), "model.glb"));
            }

            ProjectModel projectModel = Global.state().getProjectModel();
            projectModel.setProjectProcessed(true);
            projectModel.setProcessedTextureResolution(settings.getTextureResolution().width);
            projectModel.notifyProcessingComplete();

            // Refresh tabs
            new TabsManager(renderable).refreshTab("Materials");
        }
        catch (IOException | ParserConfigurationException | TransformerException e)
        {
            ExceptionHandling.error("Error executing specular fit request:", e);
        }
    }

    private <ContextType extends Context<ContextType>> void performReconstruction(
        ReadonlyGraphicsResources<ContextType> resources, SpecularMaterialResources<ContextType> specularFit)
        throws IOException
    {
        if (settings.getOutputDirectory() != null)
        {
            // Create output directory
            settings.getOutputDirectory().mkdirs();

            if (resources.getViewSet() != null)
            {
                // Reconstruct images both from basis functions and from fitted roughness
                SpecularFitProgramFactory<ContextType> programFactory = new SpecularFitProgramFactory<>(settings.getSpecularBasisSettings());
                FinalReconstruction<ContextType> reconstruction =
                    new FinalReconstruction<>(resources, settings.getTextureResolution(), settings.getReconstructionSettings());

                LOG.info("Reconstructing:");
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
