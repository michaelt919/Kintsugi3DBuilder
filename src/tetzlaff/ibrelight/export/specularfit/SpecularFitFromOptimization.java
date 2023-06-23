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

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Texture2D;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.rendering.resources.GraphicsStream;
import tetzlaff.ibrelight.rendering.resources.GraphicsStreamResource;
import tetzlaff.optimization.ShaderBasedErrorCalculator;
import tetzlaff.optimization.function.GeneralizedSmoothStepBasis;
import tetzlaff.util.ColorList;

/**
 * A class that bundles all of the GPU resources for representing a final specular fit solution.
 * @param <ContextType>
 */
@SuppressWarnings("PackageVisibleField")
public class SpecularFitFromOptimization<ContextType extends Context<ContextType>> extends SpecularFitBase<ContextType>
{
    private final ContextType context;
    private final TextureFitSettings textureFitSettings;
    private final SpecularBasisSettings specularBasisSettings;

    /**
     * Final diffuse estimate
     */
    final FinalDiffuseOptimization<ContextType> diffuseOptimization;

    /**
     * Estimated surface normals
     */
    final NormalOptimization<ContextType> normalOptimization;

    public SpecularFitFromOptimization(ContextType context, SpecularFitProgramFactory<ContextType> programFactory,
        TextureFitSettings textureFitSettings, SpecularBasisSettings specularBasisSettings, NormalOptimizationSettings normalOptimizationSettings)
        throws FileNotFoundException
    {
        super(context, textureFitSettings, specularBasisSettings);
        this.context = context;
        this.textureFitSettings = textureFitSettings;
        this.specularBasisSettings = specularBasisSettings;

        // Final diffuse estimation
        diffuseOptimization = new FinalDiffuseOptimization<>(programFactory, textureFitSettings);

        // Normal optimization module that manages its own resources
        normalOptimization = new NormalOptimization<>(
            programFactory,
            estimationProgram ->
            {
                Drawable<ContextType> drawable = programFactory.createDrawable(estimationProgram);
                programFactory.setupShaderProgram(estimationProgram);
                basisResources.useWithShaderProgram(estimationProgram);
                return drawable;
            },
            textureFitSettings, normalOptimizationSettings);
    }

    void optimize(SpecularDecomposition specularDecomposition,
        GraphicsStreamResource<ContextType> reflectanceStream, int weightBlockSize, double convergenceTolerance,
        ShaderBasedErrorCalculator<ContextType> errorCalculator, File debugDirectory) throws IOException
    {
        reflectanceStream.getProgram().setTexture("roughnessEstimate", getSpecularRoughnessMap());

        // Track how the error improves over iterations of the whole algorithm.
        double previousIterationError;

        BRDFReconstruction brdfReconstruction = new BRDFReconstruction(
            specularBasisSettings,
            new GeneralizedSmoothStepBasis(
                specularBasisSettings.getMicrofacetDistributionResolution(),
                specularBasisSettings.getMetallicity(),
                (int) Math.round(specularBasisSettings.getSpecularSmoothness() * specularBasisSettings.getMicrofacetDistributionResolution()),
                x -> 3 * x * x - 2 * x * x * x)
//                new StepBasis(settings.microfacetDistributionResolution, settings.getMetallicity())
        );
        SpecularWeightOptimization weightOptimization = new SpecularWeightOptimization(
            textureFitSettings, specularBasisSettings, weightBlockSize);

        // Instantiate once so that the memory buffers can be reused.
        GraphicsStream<ColorList[]> reflectanceStreamParallel = reflectanceStream.parallel();

        do
        {
            previousIterationError = errorCalculator.getReport().getError();

            // Use the current front normal buffer for extracting reflectance information.
            reflectanceStream.getProgram().setTexture("normalEstimate", getNormalMap());

            // Reconstruct the basis BRDFs.
            // Set up a stream and pass it to the BRDF reconstruction module to give it access to the reflectance information.
            // Operate in parallel for optimal performance.
            brdfReconstruction.execute(
                reflectanceStreamParallel.map(framebufferData -> new ReflectanceData(framebufferData[0], framebufferData[1])),
                specularDecomposition);

            // Use the current front normal buffer for calculating error.
            errorCalculator.getProgram().setTexture("normalEstimate", getNormalMap());

            // Log error in debug mode.
            if (debugDirectory != null)
            {
                // Prepare for error calculation on the GPU.
                // Basis functions will have changed.
                basisResources.updateFromSolution(specularDecomposition);

                System.out.println("Calculating error...");
                errorCalculator.update();
                SpecularOptimization.logError(errorCalculator.getReport());

                // Save basis image visualization for reference and debugging
                try (BasisImageCreator<ContextType> basisImageCreator = new BasisImageCreator<>(context, specularBasisSettings))
                {
                    basisImageCreator.createImages(this, debugDirectory);
                }

                // write out diffuse texture for debugging
                specularDecomposition.saveDiffuseMap(textureFitSettings.gamma, debugDirectory);
            }

            if (specularBasisSettings.getBasisCount() > 1)
            {
                // Make sure there are enough blocks for any pixels that don't go into the weight blocks evenly.
                int blockCount = (textureFitSettings.width * textureFitSettings.height + weightBlockSize - 1) / weightBlockSize;

                // Initially assume that all texels are invalid.
                specularDecomposition.invalidateWeights();

                for (int i = 0; i < blockCount; i++) // TODO: this was done quickly; may need to be refactored
                {
                    System.out.println("Starting block " + i + "...");
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

            if (SpecularOptimization.DEBUG)
            {
                System.out.println("Calculating error...");
            }

            // Prepare for error calculation and then normal optimization on the GPU.
            // Weight maps will have changed.
            basisResources.updateFromSolution(specularDecomposition);

            // Calculate the error in preparation for normal estimation.
            errorCalculator.update();

            if (SpecularOptimization.DEBUG)
            {
                // Log error in debug mode.
                SpecularOptimization.logError(errorCalculator.getReport());
            }

            if (normalOptimization.isNormalRefinementEnabled())
            {
                System.out.println("Optimizing normals...");

                normalOptimization.execute(normalMap ->
                    {
                        // Update program to use the new front buffer for error calculation.
                        errorCalculator.getProgram().setTexture("normalEstimate", normalMap);

                        if (SpecularOptimization.DEBUG)
                        {
                            System.out.println("Calculating error...");
                        }

                        // Calculate the error to determine if we should stop.
                        errorCalculator.update();

                        if (SpecularOptimization.DEBUG)
                        {
                            // Log error in debug mode.
                            SpecularOptimization.logError(errorCalculator.getReport());
                        }

                        return errorCalculator.getReport();
                    },
                    convergenceTolerance);

                if (debugDirectory != null)
                {
                    normalOptimization.saveNormalMap(debugDirectory);
                }

                if (errorCalculator.getReport().getError() > errorCalculator.getReport().getPreviousError())
                {
                    // Revert error calculations to the last accepted result.
                    errorCalculator.reject();
                }
            }

            // Estimate specular roughness and reflectivity.
            // This can cause error to increase but it's unclear if that poses a problem for convergence.
            roughnessOptimization.execute();

            if (SpecularOptimization.DEBUG)
            {
                roughnessOptimization.saveTextures(debugDirectory);

                // Log error in debug mode.
                basisResources.updateFromSolution(specularDecomposition);
                System.out.println("Calculating error...");
                errorCalculator.update();
                SpecularOptimization.logError(errorCalculator.getReport());
            }
        }
        while ((specularBasisSettings.getBasisCount() > 1 || normalOptimization.isNormalRefinementEnabled()) &&
            // Iteration not necessary if basisCount is 1 and normal refinement is off.
            previousIterationError - errorCalculator.getReport().getError() > convergenceTolerance);
    }

    @Override
    public void close()
    {
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
}
