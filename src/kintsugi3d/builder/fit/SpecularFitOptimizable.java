/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.fit.debug.BasisImageCreator;
import kintsugi3d.builder.fit.decomposition.*;
import kintsugi3d.builder.fit.finalize.FinalDiffuseOptimization;
import kintsugi3d.builder.fit.normal.NormalOptimization;
import kintsugi3d.builder.fit.settings.NormalOptimizationSettings;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;
import kintsugi3d.builder.resources.ibr.stream.GraphicsStream;
import kintsugi3d.builder.resources.ibr.stream.GraphicsStreamResource;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Drawable;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.core.Texture2D;
import kintsugi3d.optimization.ReadonlyErrorReport;
import kintsugi3d.optimization.ShaderBasedErrorCalculator;
import kintsugi3d.optimization.function.GeneralizedSmoothStepBasis;
import kintsugi3d.util.ColorList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * A class that bundles all of the GPU resources for representing a final specular fit solution.
 * @param <ContextType>
 */
public final class SpecularFitOptimizable<ContextType extends Context<ContextType>> extends SpecularFitBase<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(SpecularFitOptimizable.class);

    private final ContextType context;

    private final ReadonlyIBRResources<ContextType> resources;
    private final TextureResolution textureResolution;
    private final float gamma;

    private final SpecularBasisSettings specularBasisSettings;

    private final Consumer<Program<ContextType>> setupShaderProgram;

    private final FinalDiffuseOptimization<ContextType> diffuseOptimization;

    private final NormalOptimization<ContextType> normalOptimization;

    private SpecularFitOptimizable(
        ReadonlyIBRResources<ContextType> resources, BasisResources<ContextType> basisResources, boolean basisResourcesOwned,
        SpecularBasisSettings specularBasisSettings, SpecularFitProgramFactory<ContextType> programFactory,
        TextureResolution textureResolution, float gamma,
        NormalOptimizationSettings normalOptimizationSettings, boolean includeConstantTerm)
        throws IOException
    {
        super(basisResources, basisResourcesOwned, textureResolution);
        this.context = resources.getContext();
        this.resources = resources;
        this.textureResolution = textureResolution;
        this.gamma = gamma;
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

    public static <ContextType extends Context<ContextType>> SpecularFitOptimizable<ContextType> createNew(
        ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory, TextureResolution textureResolution,
        float gamma, SpecularBasisSettings specularBasisSettings, NormalOptimizationSettings normalOptimizationSettings, boolean includeConstantTerm)
        throws IOException
    {
        return new SpecularFitOptimizable<>(resources,
            new BasisResources<>(resources.getContext(), specularBasisSettings.getBasisCount(), specularBasisSettings.getBasisResolution()),
                true, specularBasisSettings, programFactory, textureResolution, gamma, normalOptimizationSettings, includeConstantTerm);
    }

    public ReadonlyIBRResources<ContextType> getResources()
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

    private void optimize(Runnable iteration, double convergenceTolerance, ShaderBasedErrorCalculator<ContextType> errorCalculator)
    {
        // Track how the error improves over iterations of the whole algorithm.
        double previousIterationError;

        do
        {
            previousIterationError = errorCalculator.getReport().getError();
            iteration.run();
        }
        while ((specularBasisSettings.getBasisCount() > 1 || normalOptimization.isNormalRefinementEnabled()) &&
            // Iteration not necessary if basisCount is 1 and normal refinement is off.
            previousIterationError - errorCalculator.getReport().getError() > convergenceTolerance);
    }

    void optimizeFromExistingBasis(SpecularDecomposition specularDecomposition,
        GraphicsStreamResource<ContextType> reflectanceStream, double convergenceTolerance,
        ShaderBasedErrorCalculator<ContextType> errorCalculator, File debugDirectory)
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
                    convergenceTolerance, errorCalculator, debugDirectory);
            },
            convergenceTolerance, errorCalculator);
    }

    void optimizeFromScratch(SpecularDecompositionFromScratch specularDecomposition,
        GraphicsStreamResource<ContextType> reflectanceStream, double convergenceTolerance,
        ShaderBasedErrorCalculator<ContextType> errorCalculator, File debugDirectory)
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

                basisOptimizationIteration(specularDecomposition, reflectanceStreamParallel, errorCalculator);

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
                    specularDecomposition.saveDiffuseMap(gamma, debugDirectory);
                }

                getBasisResources().updateFromSolution(specularDecomposition);

                weightAndNormalIteration(specularDecomposition, reflectanceStream, weightOptimization,
                    convergenceTolerance, errorCalculator, debugDirectory);
            },
            convergenceTolerance, errorCalculator);
    }

    private void weightAndNormalIteration(SpecularDecomposition specularDecomposition, GraphicsStream<ColorList[]> reflectanceStream,
        SpecularWeightOptimization weightOptimization, double convergenceTolerance, ShaderBasedErrorCalculator<ContextType> errorCalculator,
        File debugDirectory)
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
        calculateError(errorCalculator);

        if (errorCalculator.getReport().getError() > 0.0 // error == 0 probably only if there are no valid pixels
            && normalOptimization.isNormalRefinementEnabled())
        {
            normalOptimizationIteration(convergenceTolerance, errorCalculator, debugDirectory);
        }

        // Estimate specular roughness and reflectivity.
        // This can cause error to increase but it's unclear if that poses a problem for convergence.
        getRoughnessOptimization().execute(gamma);

        if (debugDirectory != null)
        {
            getRoughnessOptimization().saveTextures(debugDirectory);

            // Log error in debug mode.
            calculateError(errorCalculator);
        }
    }

    private void prepareForOptimization(GraphicsStreamResource<ContextType> reflectanceStream)
    {
        // Setup reflectance extraction program
        setupShaderProgram.accept(reflectanceStream.getProgram());

        reflectanceStream.getProgram().setTexture("roughnessMap", getSpecularRoughnessMap());
    }

    private void calculateError(ShaderBasedErrorCalculator<ContextType> errorCalculator)
    {
        log.debug("Calculating error...");

        // Calculate the error in preparation for normal estimation.
        errorCalculator.update();

        // Log error in debug mode.
        logError(errorCalculator.getReport());
    }

    private void basisOptimizationIteration(SpecularDecompositionFromScratch specularDecomposition,
        GraphicsStream<ColorList[]> reflectanceStreamParallel, ShaderBasedErrorCalculator<ContextType> errorCalculator)
    {
        BRDFReconstruction brdfReconstruction = new BRDFReconstruction(
            specularBasisSettings,
            new GeneralizedSmoothStepBasis(
                specularBasisSettings.getBasisResolution(),
                specularBasisSettings.getMetallicity(),
                (int) Math.round(specularBasisSettings.getSpecularSmoothness() * specularBasisSettings.getBasisResolution()),
                x -> 3 * x * x - 2 * x * x * x)
//                new StepBasis(settings.microfacetDistributionResolution, settings.getMetallicity())
        );

        // Reconstruct the basis BRDFs.
        // Set up a stream and pass it to the BRDF reconstruction module to give it access to the reflectance information.
        // Operate in parallel for optimal performance.
        brdfReconstruction.execute(
            reflectanceStreamParallel.map(framebufferData -> new ReflectanceData(framebufferData[0], framebufferData[1])),
            specularDecomposition);

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
                specularDecomposition.saveDiffuseMap(gamma, debugDirectory);
            }
        }
    }

    private void normalOptimizationIteration(double convergenceTolerance, ShaderBasedErrorCalculator<ContextType> errorCalculator, File debugDirectory)
    {
        log.info("Optimizing normals...");

        normalOptimization.execute(normalMap ->
            {
                // Update program to use the new front buffer for error calculation.
                errorCalculator.getProgram().setTexture("normalMap", normalMap);
                calculateError(errorCalculator);
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
