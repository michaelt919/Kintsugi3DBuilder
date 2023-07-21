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
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Program;
import tetzlaff.gl.core.Texture2D;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.rendering.resources.GraphicsStream;
import tetzlaff.ibrelight.rendering.resources.GraphicsStreamResource;
import tetzlaff.ibrelight.rendering.resources.ReadonlyIBRResources;
import tetzlaff.optimization.ReadonlyErrorReport;
import tetzlaff.optimization.ShaderBasedErrorCalculator;
import tetzlaff.optimization.function.GeneralizedSmoothStepBasis;
import tetzlaff.util.ColorList;

/**
 * A class that bundles all of the GPU resources for representing a final specular fit solution.
 * @param <ContextType>
 */
public final class SpecularFitOptimizable<ContextType extends Context<ContextType>> extends SpecularFitBase<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(SpecularFitOptimizable.class);

    private final ContextType context;

    private final ReadonlyIBRResources<ContextType> resources;
    private final TextureFitSettings textureFitSettings;

    private final SpecularBasisSettings specularBasisSettings;

    private final Consumer<Program<ContextType>> setupShaderProgram;

    private final FinalDiffuseOptimization<ContextType> diffuseOptimization;

    private final NormalOptimization<ContextType> normalOptimization;

    private SpecularFitOptimizable(
        ReadonlyIBRResources<ContextType> resources, BasisResources<ContextType> basisResources, boolean basisResourcesOwned,
        SpecularFitProgramFactory<ContextType> programFactory, TextureFitSettings textureFitSettings,
        NormalOptimizationSettings normalOptimizationSettings)
        throws FileNotFoundException
    {
        super(basisResources, basisResourcesOwned, textureFitSettings);
        this.context = resources.getContext();
        this.resources = resources;
        this.textureFitSettings = textureFitSettings;
        this.specularBasisSettings = basisResources.getSpecularBasisSettings();
        this.setupShaderProgram = program -> programFactory.setupShaderProgram(resources, program);

        // Final diffuse estimation
        diffuseOptimization = new FinalDiffuseOptimization<>(resources, programFactory, textureFitSettings);

        // Normal optimization module that manages its own resources
        normalOptimization = new NormalOptimization<>(
            resources,
            programFactory,
            estimationProgram ->
            {
                Drawable<ContextType> drawable = resources.createDrawable(estimationProgram);
                programFactory.setupShaderProgram(resources, estimationProgram);
                getBasisResources().useWithShaderProgram(estimationProgram);
                getBasisWeightResources().useWithShaderProgram(estimationProgram);
                return drawable;
            },
            textureFitSettings, normalOptimizationSettings);
    }

    public static <ContextType extends Context<ContextType>> SpecularFitOptimizable<ContextType> create(
        ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory, TextureFitSettings textureFitSettings,
        SpecularBasisSettings specularBasisSettings, NormalOptimizationSettings normalOptimizationSettings)
        throws FileNotFoundException
    {
        return new SpecularFitOptimizable<>(resources,
            new BasisResources<>(resources.getContext(), specularBasisSettings), true,
            programFactory, textureFitSettings, normalOptimizationSettings);
    }

    public ReadonlyIBRResources<ContextType> getResources()
    {
        return resources;
    }

    public TextureFitSettings getTextureFitSettings()
    {
        return textureFitSettings;
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
        SpecularWeightOptimization weightOptimization = new SpecularWeightOptimization(textureFitSettings, specularBasisSettings);

        // Run once just in case
        getBasisResources().updateFromSolution(specularDecomposition);

        optimize(
            () ->
            {
                // Use the current front normal buffer for extracting reflectance information.
                reflectanceStream.getProgram().setTexture("normalEstimate", getNormalMap());

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
        SpecularWeightOptimization weightOptimization = new SpecularWeightOptimization(textureFitSettings, specularBasisSettings);

        // Instantiate once so that the memory buffers can be reused.
        GraphicsStream<ColorList[]> reflectanceStreamParallel = reflectanceStream.parallel();

        optimize(
            () ->
            {
                // Use the current front normal buffer for extracting reflectance information.
                reflectanceStream.getProgram().setTexture("normalEstimate", getNormalMap());

                basisOptimizationIteration(specularDecomposition, reflectanceStreamParallel, errorCalculator, debugDirectory);
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
        errorCalculator.getProgram().setTexture("normalEstimate", getNormalMap());
        calculateError(errorCalculator);

        if (errorCalculator.getReport().getError() > 0.0 // error == 0 probably only if there are no valid pixels
            && normalOptimization.isNormalRefinementEnabled())
        {
            normalOptimizationIteration(convergenceTolerance, errorCalculator, debugDirectory);
        }

        // Estimate specular roughness and reflectivity.
        // This can cause error to increase but it's unclear if that poses a problem for convergence.
        getRoughnessOptimization().execute();

        if (SpecularOptimization.DEBUG)
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

        reflectanceStream.getProgram().setTexture("roughnessEstimate", getSpecularRoughnessMap());
    }

    private void calculateError(ShaderBasedErrorCalculator<ContextType> errorCalculator)
    {
        if (SpecularOptimization.DEBUG)
        {
            log.info("Calculating error...");
        }

        // Calculate the error in preparation for normal estimation.
        errorCalculator.update();

        if (SpecularOptimization.DEBUG)
        {
            // Log error in debug mode.
            logError(errorCalculator.getReport());
        }
    }

    private void basisOptimizationIteration(SpecularDecompositionFromScratch specularDecomposition,
        GraphicsStream<ColorList[]> reflectanceStreamParallel, ShaderBasedErrorCalculator<ContextType> errorCalculator,
        File debugDirectory)
    {
        BRDFReconstruction brdfReconstruction = new BRDFReconstruction(
            specularBasisSettings,
            new GeneralizedSmoothStepBasis(
                specularBasisSettings.getMicrofacetDistributionResolution(),
                specularBasisSettings.getMetallicity(),
                (int) Math.round(specularBasisSettings.getSpecularSmoothness() * specularBasisSettings.getMicrofacetDistributionResolution()),
                x -> 3 * x * x - 2 * x * x * x)
//                new StepBasis(settings.microfacetDistributionResolution, settings.getMetallicity())
        );

        // Reconstruct the basis BRDFs.
        // Set up a stream and pass it to the BRDF reconstruction module to give it access to the reflectance information.
        // Operate in parallel for optimal performance.
        brdfReconstruction.execute(
            reflectanceStreamParallel.map(framebufferData -> new ReflectanceData(framebufferData[0], framebufferData[1])),
            specularDecomposition);

        // Log error in debug mode.
        if (debugDirectory != null)
        {
            // Use the current front normal buffer for calculating error.
            errorCalculator.getProgram().setTexture("normalEstimate", getNormalMap());

            // Prepare for error calculation on the GPU.
            // Basis functions will have changed.
            getBasisResources().updateFromSolution(specularDecomposition);

            log.info("Calculating error...");
            errorCalculator.update();
            logError(errorCalculator.getReport());

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
            specularDecomposition.saveDiffuseMap(textureFitSettings.gamma, debugDirectory);
        }
    }

    private void weightOptimizationIteration(SpecularDecomposition specularDecomposition,
        GraphicsStream<ColorList[]> reflectanceStream, SpecularWeightOptimization weightOptimization, File debugDirectory)
    {
        int weightBlockSize = weightOptimization.getWeightBlockSize();

        // Make sure there are enough blocks for any pixels that don't go into the weight blocks evenly.
        int blockCount = (textureFitSettings.width * textureFitSettings.height + weightBlockSize - 1) / weightBlockSize;

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
                specularDecomposition.saveDiffuseMap(
                    textureFitSettings.gamma, debugDirectory);
            }
        }
    }

    private void normalOptimizationIteration(double convergenceTolerance, ShaderBasedErrorCalculator<ContextType> errorCalculator, File debugDirectory)
    {
        log.info("Optimizing normals...");

        normalOptimization.execute(normalMap ->
            {
                // Update program to use the new front buffer for error calculation.
                errorCalculator.getProgram().setTexture("normalEstimate", normalMap);
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
        log.info("--------------------------------------------------");
        log.info("Error: " + report.getError());
        log.info("(Previous error: " + report.getPreviousError() + ')');
        log.info("--------------------------------------------------");
        log.info("");
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
