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

import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.core.UserCancellationException;
import kintsugi3d.builder.fit.debug.BasisImageCreator;
import kintsugi3d.builder.fit.decomposition.*;
import kintsugi3d.builder.fit.finalize.FinalDiffuseOptimization;
import kintsugi3d.builder.fit.normal.NormalOptimization;
import kintsugi3d.builder.fit.settings.NormalOptimizationSettings;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.builder.resources.project.ReadonlyGraphicsResources;
import kintsugi3d.builder.resources.project.specular.SpecularMaterialResources;
import kintsugi3d.builder.resources.project.stream.GraphicsStream;
import kintsugi3d.builder.resources.project.stream.GraphicsStreamResource;
import kintsugi3d.gl.core.*;
import kintsugi3d.optimization.ReadonlyErrorReport;
import kintsugi3d.optimization.ShaderBasedErrorCalculator;
import kintsugi3d.optimization.function.GeneralizedSmoothStepBasis;
import kintsugi3d.util.ColorList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A class that bundles all of the GPU resources for representing a final specular fit solution.
 * @param <ContextType>
 */
public final class SpecularFitOptimizable<ContextType extends Context<ContextType>> extends SpecularFitBase<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(SpecularFitOptimizable.class);

    private final ContextType context;

    private final ReadonlyGraphicsResources<ContextType> resources;
    private final TextureResolution textureResolution;

    private final SpecularBasisSettings specularBasisSettings;

    private final Consumer<Program<ContextType>> setupShaderProgram;

    private final FinalDiffuseOptimization<ContextType> diffuseOptimization;

    private final NormalOptimization<ContextType> normalOptimization;

    private final ShaderBasedErrorCalculator<ContextType> errorCalculator;

    private SpecularFitOptimizable(
        ReadonlyGraphicsResources<ContextType> resources, BasisResources<ContextType> basisResources, boolean basisResourcesOwned,
        SpecularBasisSettings specularBasisSettings, SpecularFitProgramFactory<ContextType> programFactory,
        TextureResolution textureResolution, NormalOptimizationSettings normalOptimizationSettings, boolean includeConstantTerm)
        throws IOException
    {
        super(basisResources, basisResourcesOwned, textureResolution);
        this.context = resources.getContext();
        this.resources = resources;
        this.textureResolution = textureResolution;
        this.specularBasisSettings = specularBasisSettings;
        this.setupShaderProgram = program -> programFactory.setupShaderProgram(resources, program);

        // Final diffuse estimation
        diffuseOptimization = new FinalDiffuseOptimization<>(resources, programFactory, textureResolution, includeConstantTerm);

        // Normal optimization module that manages its own resources
        normalOptimization = new NormalOptimization<>(
            resources,
            programFactory,
            estimationProgram -> getNormalDrawable(estimationProgram, programFactory),
            textureResolution, normalOptimizationSettings);

        errorCalculator = ShaderBasedErrorCalculator.create(resources.getContext(),
            () -> createErrorCalcProgram(resources, programFactory),
            program -> createErrorCalcDrawable(this, resources, program),
            textureResolution.width, textureResolution.height);
    }

    private Drawable<ContextType> getNormalDrawable(Program<ContextType> estimationProgram,
        SpecularFitProgramFactory<ContextType> programFactory)
    {
        Drawable<ContextType> drawable = resources.createDrawable(estimationProgram);
        programFactory.setupShaderProgram(resources, estimationProgram);
        getBasisResources().useWithShaderProgram(estimationProgram);
        getBasisWeightResources().useWithShaderProgram(estimationProgram);
        return drawable;
    }

    private static <ContextType extends Context<ContextType>>
    ProgramObject<ContextType> createErrorCalcProgram(
        ReadonlyGraphicsResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory)
    {
        try
        {
            return programFactory.createProgram(resources,
                new File("shaders/common/texspace_dynamic.vert"),
                new File("shaders/specularfit/errorCalc.frag"));
        }
        catch (IOException e)
        {
            log.error("An error occurred creating error calculation shader:", e);
            return null;
        }
    }

    private static <ContextType extends Context<ContextType>> Drawable<ContextType> createErrorCalcDrawable(
        SpecularMaterialResources<ContextType> specularFit, ReadonlyGraphicsResources<ContextType> resources, Program<ContextType> errorCalcProgram)
    {
        Drawable<ContextType> errorCalcDrawable = resources.createDrawable(errorCalcProgram);
        specularFit.getBasisResources().useWithShaderProgram(errorCalcProgram);
        specularFit.getBasisWeightResources().useWithShaderProgram(errorCalcProgram);
        errorCalcProgram.setTexture("roughnessMap", specularFit.getSpecularRoughnessMap());
        errorCalcProgram.setTexture("normalMap", specularFit.getNormalMap());
        errorCalcProgram.setUniform("sRGB", false);
        return errorCalcDrawable;
    }

    public static <ContextType extends Context<ContextType>> SpecularFitOptimizable<ContextType> createNew(
        ReadonlyGraphicsResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory, TextureResolution textureResolution,
        SpecularBasisSettings specularBasisSettings, NormalOptimizationSettings normalOptimizationSettings, boolean includeConstantTerm)
        throws IOException
    {
        return new SpecularFitOptimizable<>(resources,
            new BasisResources<>(resources.getContext(), specularBasisSettings.getBasisCount(), specularBasisSettings.getBasisResolution()),
                true, specularBasisSettings, programFactory, textureResolution, normalOptimizationSettings, includeConstantTerm);
    }

    public ReadonlyGraphicsResources<ContextType> getResources()
    {
        return resources;
    }

    public TextureResolution getTextureResolution()
    {
        return textureResolution;
    }

    public SpecularBasisSettings getSpecularBasisSettings()
    {
        return specularBasisSettings;
    }

    private void optimize(Runnable iteration, double convergenceTolerance, ProgressMonitor monitor)
        throws UserCancellationException
    {
        //monitor.setMaxProgress(1.0 / convergenceTolerance);

        // Track how the error improves over iterations of the whole algorithm.
        double deltaError;
        double minDeltaError = Double.POSITIVE_INFINITY;

        do
        {
            monitor.allowUserCancellation();

            double previousIterationError = errorCalculator.getReport().getError();
            iteration.run();

            deltaError = previousIterationError - errorCalculator.getReport().getError();
            minDeltaError = Math.min(minDeltaError, deltaError);
            //monitor.setProgress(1.0 / Math.max(convergenceTolerance, minDeltaError), MessageFormat.format("Delta error: {0}", minDeltaError));
        }
        while ((specularBasisSettings.getBasisCount() > 1 || normalOptimization.isNormalRefinementEnabled()) &&
            // Iteration not necessary if basisCount is 1 and normal refinement is off.
            deltaError > convergenceTolerance);
    }

    void optimizeFromExistingBasis(SpecularDecomposition specularDecomposition,
        GraphicsStreamResource<ContextType> reflectanceStream, double convergenceTolerance, ProgressMonitor monitor, File debugDirectory)
            throws UserCancellationException
    {
        prepareForOptimization(reflectanceStream);

        // Track how the error improves over iterations of the whole algorithm.
        SpecularWeightOptimization weightOptimization = new SpecularWeightOptimization(textureResolution, specularBasisSettings);

        // Run once just in case
        getBasisResources().updateFromSolution(specularDecomposition);

        optimize(
            () ->
            {
                // Use the current front normal buffer for extracting reflectance information.
                reflectanceStream.getProgram().setTexture("normalMap", getNormalMap());

                weightAndNormalIteration(specularDecomposition, reflectanceStream, weightOptimization,
                    convergenceTolerance, debugDirectory);
            },
            convergenceTolerance, monitor);
    }

    void optimizeFromScratch(SpecularDecompositionFromScratch specularDecomposition,
        GraphicsStreamResource<ContextType> reflectanceStream, double convergenceTolerance, ProgressMonitor monitor, File debugDirectory)
            throws UserCancellationException
    {
        prepareForOptimization(reflectanceStream);

        // Track how the error improves over iterations of the whole algorithm.
        SpecularWeightOptimization weightOptimization = new SpecularWeightOptimization(textureResolution, specularBasisSettings);

        // Instantiate once so that the memory buffers can be reused.
        GraphicsStream<ColorList[]> reflectanceStreamParallel = reflectanceStream.parallel();

        optimize(
            () ->
            {
                // Use the current front normal buffer for extracting reflectance information.
                reflectanceStream.getProgram().setTexture("normalMap", getNormalMap());

                basisOptimizationIteration(specularDecomposition, reflectanceStreamParallel, monitor);

                if (debugDirectory != null)
                {
                    // Save basis image visualization for reference and debugging
                    try (BasisImageCreator<ContextType> basisImageCreator = new BasisImageCreator<>(context, specularBasisSettings))
                    {
                        basisImageCreator.createImages(this, debugDirectory);
                    }
                    catch (IOException e)
                    {
                        log.error("Error occurred while creating basis images:", e);
                    }

                    // write out diffuse texture for debugging
                    specularDecomposition.saveDiffuseMap(debugDirectory);
                }

                getBasisResources().updateFromSolution(specularDecomposition);

                weightAndNormalIteration(specularDecomposition, reflectanceStream, weightOptimization,
                    convergenceTolerance, debugDirectory);
            },
            convergenceTolerance, monitor);
    }

    private void weightAndNormalIteration(SpecularDecomposition specularDecomposition, GraphicsStream<ColorList[]> reflectanceStream,
        SpecularWeightOptimization weightOptimization, double convergenceTolerance, File debugDirectory)
    {
        if (specularBasisSettings.getBasisCount() > 1)
        {
            weightOptimizationIteration(specularDecomposition, reflectanceStream, weightOptimization, debugDirectory);
        }

        // Prepare for error calculation and then normal optimization on the GPU.
        // Weight maps will have changed.
        // TODO: do we need to do this when basis count == 1?  Not sure where they're initialized otherwise.
        getBasisWeightResources().updateFromSolution(specularDecomposition);

        // Use the current front normal buffer for calculating error.
        errorCalculator.getProgram().setTexture("normalMap", getNormalMap());
        calculateError();

        if (errorCalculator.getReport().getError() > 0.0 // error == 0 probably only if there are no valid pixels
            && normalOptimization.isNormalRefinementEnabled())
        {
            normalOptimizationIteration(convergenceTolerance, debugDirectory);
        }

        // Estimate specular roughness and reflectivity.
        // This can cause error to increase but it's unclear if that poses a problem for convergence.
        getRoughnessOptimization().execute();

        if (debugDirectory != null)
        {
            getRoughnessOptimization().saveTextures(debugDirectory);

            // Log error in debug mode.
            calculateError();
        }
    }

    private void prepareForOptimization(GraphicsStreamResource<ContextType> reflectanceStream)
    {
        // Setup reflectance extraction program
        setupShaderProgram.accept(reflectanceStream.getProgram());

        reflectanceStream.getProgram().setTexture("roughnessMap", getSpecularRoughnessMap());
    }

    private void calculateError()
    {
        log.debug("Calculating error...");

        // Calculate the error in preparation for normal estimation.
        errorCalculator.update();

        // Log error in debug mode.
        logError(errorCalculator.getReport());
    }

    private void basisOptimizationIteration(SpecularDecompositionFromScratch specularDecomposition,
        GraphicsStream<ColorList[]> reflectanceStreamParallel, ProgressMonitor monitor)
    {
        BRDFReconstruction brdfReconstruction = new BRDFReconstruction(
            specularBasisSettings,
            new GeneralizedSmoothStepBasis(
                specularBasisSettings.getBasisResolution(),
                specularBasisSettings.getMetallicity(),
                specularBasisSettings.getSpecularMinWidth(),
                specularBasisSettings.getSpecularMaxWidth(),
                specularBasisSettings.getBasisComplexity(),
                x -> 3 * x * x - 2 * x * x * x)
//                new StepBasis(settings.microfacetDistributionResolution, settings.getMetallicity())
        );

        // Reconstruct the basis BRDFs.
        // Set up a stream and pass it to the BRDF reconstruction module to give it access to the reflectance information.
        // Operate in parallel for optimal performance.
        brdfReconstruction.execute(
            reflectanceStreamParallel.map(framebufferData -> new ReflectanceData(framebufferData[0], framebufferData[1])),
            specularDecomposition, monitor);

        // Use the current front normal buffer for calculating error.
        errorCalculator.getProgram().setTexture("normalMap", getNormalMap());

        // Prepare for error calculation on the GPU.
        // Basis functions will have changed.
        getBasisResources().updateFromSolution(specularDecomposition);

        log.debug("Calculating error...");
        errorCalculator.update();
        logError(errorCalculator.getReport());
    }

    private void weightOptimizationIteration(SpecularDecomposition specularDecomposition,
        GraphicsStream<ColorList[]> reflectanceStream, SpecularWeightOptimization weightOptimization, File debugDirectory)
    {
        int weightBlockSize = weightOptimization.getWeightBlockSize();

        // Make sure there are enough blocks for any pixels that don't go into the weight blocks evenly.
        int blockCount = (textureResolution.width * textureResolution.height + weightBlockSize - 1) / weightBlockSize;

        // Initially assume that all texels are invalid.
        specularDecomposition.invalidateWeights();

        for (int i = 0; i < blockCount; i++) // TODO: this was done quickly; may need to be refactored
        {
            if (blockCount > 1)
            {
                log.info("Starting block " + i + "...");
            }

            weightOptimization.execute(
                reflectanceStream.map(framebufferData -> new ReflectanceData(framebufferData[0], framebufferData[1])),
                specularDecomposition, i * weightBlockSize);

            if (debugDirectory != null)
            {
                // write out weight textures for debugging
                specularDecomposition.saveWeightMaps(debugDirectory);

                // write out diffuse texture for debugging
                specularDecomposition.saveDiffuseMap(debugDirectory);
            }
        }
    }

    private void normalOptimizationIteration(double convergenceTolerance, File debugDirectory)
    {
        log.info("Optimizing normals...");

        normalOptimization.execute(normalMap ->
            {
                // Update program to use the new front buffer for error calculation.
                errorCalculator.getProgram().setTexture("normalMap", normalMap);
                calculateError();
                return errorCalculator.getReport();
            },
            convergenceTolerance);

        if (debugDirectory != null)
        {
            saveNormalMap(debugDirectory);
        }

        if (errorCalculator.getReport().getError() > errorCalculator.getReport().getPreviousError())
        {
            // Revert error calculations to the last accepted result.
            errorCalculator.reject();
        }
    }

    private static void logError(ReadonlyErrorReport report)
    {
        log.debug("Error: " + report.getError() + " (Previous error: " + report.getPreviousError() + ")");
    }

    @Override
    public void close()
    {
        super.close();
        diffuseOptimization.close();
        normalOptimization.close();
        errorCalculator.close();
    }

    @Override
    public Texture2D<ContextType> getDiffuseMap()
    {
        return diffuseOptimization.getDiffuseMap();
    }

    @Override
    public Texture2D<ContextType> getNormalMap()
    {
        return normalOptimization.getNormalMap();
    }

    @Override
    public Texture2D<ContextType> getConstantMap()
    {
        return diffuseOptimization.includesConstantMap() ? diffuseOptimization.getConstantMap() : null;
    }

//    @Override
//    public Texture2D<ContextType> getQuadraticMap()
//    {
//        return diffuseOptimization.includesConstantMap() ? diffuseOptimization.getQuadraticMap() : null;
//    }

    @Override
    public Map<String, Texture2D<ContextType>> getMetadataMaps()
    {
        return Map.of("error", errorCalculator.getFramebufferAsTexture());
    }

    public static Collection<String> getSerializableMetadataMapNames()
    {
        return List.of("error");
    }

    /**
     * Always returns null; albedo map should not be needed while optimizing; only afterwards
     * @return null
     */
    @Override
    public Texture2D<ContextType> getAlbedoMap()
    {
        return null;
    }

    /**
     * Always returns null; ORM map should not be needed while optimizing; only afterwards
     * @return null
     */
    @Override
    public Texture2D<ContextType> getORMMap()
    {
        return null;
    }

    /**
     * Final diffuse estimate
     */
    public FinalDiffuseOptimization<ContextType> getDiffuseOptimization()
    {
        return diffuseOptimization;
    }

    /**
     * Estimated surface normals
     */
    public NormalOptimization<ContextType> getNormalOptimization()
    {
        return normalOptimization;
    }
}
