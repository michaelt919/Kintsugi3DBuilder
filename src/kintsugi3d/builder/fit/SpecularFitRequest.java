/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.metrics.ReadonlyColorAppearanceRMSE;
import kintsugi3d.builder.fit.decomposition.ReadonlyBasisResources;
import kintsugi3d.builder.fit.settings.*;
import kintsugi3d.builder.rendering.ImageBasedRenderable;
import kintsugi3d.builder.rendering.ProgressMonitoredImageBasedGraphicsRequest;
import kintsugi3d.builder.resources.project.ImageBasedGraphicsResources;
import kintsugi3d.builder.resources.project.ReadonlyImageBasedGraphicsResources;
import kintsugi3d.builder.resources.project.ShaderProgramFactory;
import kintsugi3d.builder.resources.project.specular.ReadonlyTextureResources;
import kintsugi3d.builder.state.cards.TabsManager;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;
import kintsugi3d.builder.util.ApplicationFolders;
import kintsugi3d.builder.util.Kintsugi3DViewerLauncher;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.interactive.ProgressMonitor;
import kintsugi3d.gl.interactive.UserCancellationException;
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

public final class SpecularFitRequest implements ProgressMonitoredImageBasedGraphicsRequest
{
    private static final Logger LOG = LoggerFactory.getLogger(SpecularFitRequest.class);

    private static final boolean DEBUG_IMAGES = false;

    private final ReadonlySpecularFitSettings settings;
    private final ReadonlyBasisOptimizationSettings basisOptimizationSettings;

    private File outputDirectory;

    private SpecularFitRequest(ReadonlySpecularFitSettings settings,
                               ReadonlyBasisOptimizationSettings basisOptimizationSettings, File outputDirectory)
    {
        this(settings, basisOptimizationSettings);
        this.outputDirectory = outputDirectory;
    }

    private SpecularFitRequest(ReadonlySpecularFitSettings settings,
                               ReadonlyBasisOptimizationSettings basisOptimizationSettings)
    {
        this.settings = settings;
        this.basisOptimizationSettings = basisOptimizationSettings;
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
        File outputDirectory = new File(args[2]);
        return new SpecularFitRequest(params, new BasisOptimizationSettings(), outputDirectory);
    }

    public static SpecularFitRequest createReoptimizeTexturesRequest()
    {
        return new SpecularFitRequest(getSettingsFromProject(), null);
    }

    public static SpecularFitRequest createBasisAndTexturesOptimizationRequest()
    {
        return new SpecularFitRequest(getSettingsFromProject(), getBasisOptimizationSettingsFromProject());
    }

    private static ReadonlySpecularFitSettings getSettingsFromProject()
    {
        GeneralSettingsModel projectSettings = Global.io()
            .validateRenderable()
            .getLoadedViewSet().getProjectSettings();

        // Start with texture size
        int textureSize = projectSettings.getInt("textureSize");
        SpecularFitSettings settings = new SpecularFitSettings(textureSize, textureSize);

        // General optimization settings
        settings.setConvergenceTolerance(projectSettings.getFloat("convergenceTolerance"));
        settings.setSmithMaskingShadowingEnabled(projectSettings.getBoolean("smithMaskingShadowingEnabled"));
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
        settings.getExportSettings().setShouldOpenViewerOnceComplete(projectSettings.getBoolean("openViewerOnProcessingComplete"));

        // Image cache settings
        settings.getImageCacheSettings().setCacheParentDirectory(ApplicationFolders.getFitCacheRootDirectory().toFile());

        return settings;
    }

    private static ReadonlyBasisOptimizationSettings getBasisOptimizationSettingsFromProject()
    {
        GeneralSettingsModel projectSettings = Global.io()
            .validateRenderable()
            .getLoadedViewSet().getProjectSettings();

        BasisOptimizationSettings settings = new BasisOptimizationSettings();

        // Basis settings
        int basisResolution = projectSettings.getInt("basisResolution");
        settings.setBasisResolution(basisResolution);
        settings.setMaterialCount(projectSettings.getInt("basisCount"));

        // Specular settings
        int specularMinWidthDiscrete = Math.round(projectSettings.getFloat("specularMinWidthFrac") * basisResolution);
        settings.setSpecularMinWidth(specularMinWidthDiscrete);
        settings.setSpecularMaxWidth(
            Math.round(projectSettings.getFloat("specularMaxWidthFrac") * basisResolution));
        settings.setBasisComplexity(
            Math.round(projectSettings.getFloat("basisComplexityFrac") * (basisResolution - specularMinWidthDiscrete + 1)));
        settings.setMetallicity(projectSettings.getFloat("metallicity"));

        return settings;
    }

    public ReadonlySpecularFitSettings getSettings()
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
    public <ContextType extends Context<ContextType>> void executeRequest(ImageBasedRenderable<ContextType> renderable, ProgressMonitor monitor)
        throws UserCancellationException
    {
        try
        {
            if (outputDirectory == null) // If the output directory wasn't overridden
            {
                // Set the output directory based on the view set's texture fit file path
                outputDirectory = renderable.getViewSet().getSupportingFilesDirectory();
            }

            if (monitor != null)
            {
                monitor.setProcessName("Process Textures");
            }

            // Perform the specular fit
            ImageBasedGraphicsResources<ContextType> resources = renderable.getResources();

            if (basisOptimizationSettings != null)
            {
                BasisAndTexturesOptimizationProcess process = new BasisAndTexturesOptimizationProcess(settings, basisOptimizationSettings, outputDirectory);
                resources.replaceTextureResources(process.optimizeFitWithCache(resources, monitor));
            }
            else
            {
                ReoptimizeTexturesProcess process = new ReoptimizeTexturesProcess(settings, outputDirectory);
                resources.replaceTextureResources(process.reoptimizeTexturesWithCache(resources, monitor));
            }

            // Reload shaders in case preprocessor constants (i.e. number of basis functions) have changed
            renderable.reloadShaders();

            // Save project to avoid inconsistency between results and settings
            Global.io().saveProject(() ->
            {
                // Perform reconstruction
                //performReconstruction(renderable.getGraphicsResources(), renderable.getGraphicsResources().getSpecularMaterialResources());

                if (settings.getExportSettings().shouldOpenViewerOnceComplete())
                {
                    try
                    {
                        Kintsugi3DViewerLauncher.launchViewer(new File(outputDirectory, "model.glb"));
                    }
                    catch (IOException e)
                    {
                        Global.state().getProjectModel().error("Error launching Kintsugi 3D Viewer", e);
                    }
                }

                // Refresh tabs
                new TabsManager(renderable).refreshAllTabs();
            });
        }
        catch (IOException | ParserConfigurationException | TransformerException e)
        {
            Global.state().getProjectModel().error("Error executing specular fit request", e);
        }
    }

    private <ContextType extends Context<ContextType>> void performReconstruction(
        ReadonlyImageBasedGraphicsResources<ContextType> resources, ReadonlyTextureResources<ContextType> specularFit)
        throws IOException
    {
        if (outputDirectory != null)
        {
            // Create output directory
            outputDirectory.mkdirs();

            if (resources.getViewSet() != null)
            {
                // Determine basis count and resolution based on the new specularFit.
                // This will result in a program factory that overrides whatever basis count and resolution
                // would be specified by the original resources.
                ReadonlyBasisResources<ContextType> basisResources = specularFit.getBasisResources();
                ReadonlyBasisSettings basisSettings = new SimpleBasisSettings(
                    basisResources.getActiveMaterialCount(), basisResources.getBasisResolution());

                // Reconstruct images both from basis functions and from fitted roughness
                ShaderProgramFactory<ContextType> programFactory =
                    new SpecularFitResourcesWrapper<ContextType>(settings.isSmithMaskingShadowingEnabled(), basisSettings).wrap(resources);
                FinalReconstruction<ContextType> reconstruction =
                    new FinalReconstruction<>(resources, settings.getTextureResolution(), settings.getReconstructionSettings());

                LOG.info("Reconstructing:");
                List<Map<String, ReadonlyColorAppearanceRMSE>> rmseList = reconstruction.reconstruct(specularFit, Map.of(
                        "basis", ReconstructionShaders.getBasisModelReconstructionProgramBuilder(programFactory, specularFit),
                        "reflectivity", ReconstructionShaders.getReflectivityModelReconstructionProgramBuilder(programFactory, specularFit)),
                    ReconstructionShaders.getIncidentRadianceProgramBuilder(programFactory),
                    DEBUG_IMAGES ? outputDirectory : null,
                    DEBUG_IMAGES ? new File(outputDirectory, "ground-truth") : null);

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
                    try (PrintStream rmseOut = new PrintStream(new File(outputDirectory, "rmse.txt"), StandardCharsets.UTF_8))
                    // Text file containing error information
                    {
                        rmseOut.printf("basis, %s%n", reconstructionRMSE);
                        rmseOut.printf("reflectivity, %s%n", fittedRMSE);
                    }
                }
            }
        }
    }
}
