/*
 *  Copyright (c) Zhangchi (Josh) Lyu, Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.PTMfit;

import org.ejml.data.SingularMatrixException;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.FramebufferObjectBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.TextureFitSettings;

import tetzlaff.ibrelight.rendering.resources.GraphicsStreamResource;
import tetzlaff.ibrelight.rendering.resources.IBRResourcesImageSpace;
import tetzlaff.optimization.LeastSquaresMatrixBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public class PTMOptimization<ContextType extends Context<ContextType>>
{
    private TextureFitSettings settings;
    private PolynomialTextureMapBuilder mapBuilder;

    private float[] averageColors;
    private float[] unlitColors;

    public PTMOptimization(TextureFitSettings setting)
    {
        settings = setting;
        mapBuilder = new PolynomialTextureMapBuilder(settings.width,settings.height);
    }

    public void createFit(IBRResourcesImageSpace<ContextType> resources, File outputDirectory)
            throws IOException
    {
        ContextType context = resources.getContext();
        context.getState().disableBackFaceCulling();

        PTMProgramFactory<ContextType> programFactory = new PTMProgramFactory<>(resources);
        try(
            GraphicsStreamResource<ContextType> luminanceStream = resources.streamFactory().streamAsResource(
                getLuminanceProgramBuilder(programFactory),
                context.buildFramebufferObject(settings.width, settings.height)
                    .addColorAttachment(ColorFormat.RGBA32F)
                    .addColorAttachment(ColorFormat.RGBA32F))
        )
        {

            // Estimate average color without polynomial terms as a fallback for numerically unstable points.
            try (
                FramebufferObject<ContextType> averageFBO = context.buildFramebufferObject(settings.width, settings.height)
                    .addColorAttachment(ColorFormat.RGBA32F) // color
                    .addColorAttachment(ColorFormat.RGBA32F) // color
                    .createFramebufferObject();
                Program<ContextType> averageProgram = getColorAverageProgramBuilder(programFactory).createProgram())
            {
                resources.setupShaderProgram(averageProgram);
                Drawable<ContextType> averageDrawable = resources.createDrawable(averageProgram);
                averageFBO.clearColorBuffer(0, 0, 0, 0, 0);
                averageFBO.clearColorBuffer(1, 0, 0, 0, 0);
                averageDrawable.draw(PrimitiveMode.TRIANGLES, averageFBO);
                averageColors = averageFBO.readFloatingPointColorBufferRGBA(0);
                unlitColors = averageFBO.readFloatingPointColorBufferRGBA(1);
            }

            resources.setupShaderProgram(luminanceStream.getProgram());
            System.out.println("Building weight fitting matrices...");
            PTMsolution solution = new PTMsolution(settings);

            mapBuilder.buildMatrices(luminanceStream.map(framebufferData ->
            {
//                try
//                {
//                    luminanceStream.getFramebuffer().saveColorBufferToFile(1, "PNG", new File(settings.outputDirectory, "test.png"));
//                }
//                catch (IOException e)
//                {
//                    e.printStackTrace();
//                }
                return new LuminanceData(framebufferData[0], framebufferData[1]);
            }), solution.getPTMmodel(), solution);

            System.out.println("Finished building matrices; solving now...");
            optimizeWeights(p -> settings.height * settings.width != 0, solution::setWeights);
            System.out.println("DONE!");

            // write out weight textures for debugging
            fillHoles(solution);
            solution.saveWeightMaps(outputDirectory);

            TangentSpaceWeightsToObjectSpace<ContextType> tsWeightsToOS = new TangentSpaceWeightsToObjectSpace<>(resources, settings);
            tsWeightsToOS.run(solution, getTangentToObjectSpaceProgram1Builder(programFactory), 0, 4, outputDirectory);
            tsWeightsToOS.run(solution, getTangentToObjectSpaceProgram2Builder(programFactory), 4, 6, outputDirectory);

            try (PTMReconstruction<ContextType> reconstruct = new PTMReconstruction<>(resources, settings))
            {
                reconstruct.reconstruct(solution, getReconstructionProgramBuilder(programFactory), outputDirectory, "reconstruction");
            }
        }

    }

    private void fillHoles(PTMsolution solution)
    {
        // Fill holes
        // TODO Quick hack; should be replaced with something more robust.
        System.out.println("Filling holes...");

        int texelCount = settings.width * settings.height;

        for (int i = 0; i < Math.max(settings.width, settings.height); i++)
        {
            Collection<Integer> filledPositions = new HashSet<>(256);
            for (int p = 0; p < texelCount; p++)
            {
                for (int channel = 0; channel < 3; channel++)
                {
                    if (!solution.areWeightsValid(p + channel * texelCount))
                    {
                        int left = (texelCount + p - 1) % texelCount + channel * texelCount;
                        int right = (p + 1) % texelCount + channel * texelCount;
                        int up = (texelCount + p - settings.width) % texelCount + channel * texelCount;
                        int down = (p + settings.width) % texelCount + channel * texelCount;

                        int count = 0;

                        for (int b = 0; b < solution.getPTMmodel().getBasisFunctionCount(); b++)
                        {
                            count = 0;
                            double sum = 0.0;

                            if (solution.areWeightsValid(left))
                            {
                                sum += solution.getWeights(left).get(b);
                                count++;
                            }

                            if (solution.areWeightsValid(right))
                            {
                                sum += solution.getWeights(right).get(b);
                                count++;
                            }

                            if (solution.areWeightsValid(up))
                            {
                                sum += solution.getWeights(up).get(b);
                                count++;
                            }

                            if (solution.areWeightsValid(down))
                            {
                                sum += solution.getWeights(down).get(b);
                                count++;
                            }

                            if (count > 0)
                            {
                                solution.getWeights(p + channel * texelCount).set(b, sum / count);
                            }
                        }

                        if (count > 0)
                        {
                            filledPositions.add(p + channel * texelCount);
                        }
                    }
                }
            }

            for (int p : filledPositions)
            {
                solution.setWeightsValidity(p, true);
            }
        }

        System.out.println("DONE!");
    }
    public void optimizeWeights(IntPredicate areWeightsValid, BiConsumer<Integer, SimpleMatrix> weightSolutionConsumer, double toleranceScale)
    {
        boolean suppressErrors = false;

        LeastSquaresMatrixBuilder matrixBuilder= mapBuilder.getMatrixBuilder();
        for (int p = 0; p < mapBuilder.getMatrixBuilder().systemCount; p++)
        {
            if (areWeightsValid.test(p))
            {
                // Solve the system.
                try
                {
                    int colorChannel = p / (settings.width * settings.height);
                    int pixelIndex = p % (settings.width * settings.height);

                    float albedo = averageColors[pixelIndex * 4 + colorChannel];
                    float unlit = unlitColors[pixelIndex * 4 + colorChannel];

                    // Technically just linear, not lambertian
                    SimpleMatrix lambertian = new SimpleMatrix(matrixBuilder.weightCount, 1);
                    lambertian.set(0, unlit);
                    lambertian.set(1, 0.0);
                    lambertian.set(2, 0.0);
                    lambertian.set(3, albedo); // normal direction term in tangent space

                    for (int i = 4; i < lambertian.getNumElements(); i++)
                    {
                        lambertian.set(i, 0.0);
                    }

                    // Scale the PTM solution by the determinant of the matrix and fill with the Lambertian solution as necessary.
                    double determinant = matrixBuilder.weightsQTQAugmented[p].determinant();

                    if (determinant > 0.0)  // Prevent singular matrix exceptions.
                    {
                        double alpha = Math.min(determinant, 1.0);
                        SimpleMatrix rawSolution = matrixBuilder.weightsQTQAugmented[p].solve(matrixBuilder.weightsQTrAugmented[p]);

                        // Once elements start to reach absolute values of 1 / PI start blending to the linear solution.
                        double scale = IntStream.range(0, rawSolution.getNumElements()).mapToDouble(i -> Math.abs(rawSolution.get(i))).max().orElse(0);
                        alpha = Math.min(alpha, Math.max(0, 10 * (1 - scale * Math.PI)));

                        SimpleMatrix blendedSolution = rawSolution.scale(alpha).plus(1 - alpha, lambertian);

                        weightSolutionConsumer.accept(p, blendedSolution);
                    }
                    else
                    {
                        weightSolutionConsumer.accept(p, lambertian);
                    }
                }
                catch (SingularMatrixException e)
                {
                    if (!suppressErrors)
                    {
                        e.printStackTrace();
                        suppressErrors = true;
                    }
                }
            }
        }
    }

    public void optimizeWeights(IntPredicate areWeightsValid, BiConsumer<Integer, SimpleMatrix> weightSolutionConsumer)
    {
        double DEFAULT_TOLERANCE_SCALE=0.000000000001;
        optimizeWeights(areWeightsValid, weightSolutionConsumer, DEFAULT_TOLERANCE_SCALE);
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getLuminanceProgramBuilder(PTMProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
                new File("shaders/common/texspace_noscale.vert"),
                new File("shaders/PTMfit/PTMShader.frag"));
    }

    ProgramBuilder<ContextType> getReconstructionProgramBuilder(PTMProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
                new File("shaders/common/imgspace.vert"),
                new File("shaders/PTMfit/PTMreconstruction.frag"));
    }

    ProgramBuilder<ContextType> getTangentToObjectSpaceProgram1Builder(PTMProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
                new File("shaders/common/texspace_noscale.vert"),
                new File("shaders/PTMfit/ptm_objectspace1.frag"));
    }

    ProgramBuilder<ContextType> getTangentToObjectSpaceProgram2Builder(PTMProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
                new File("shaders/common/texspace_noscale.vert"),
                new File("shaders/PTMfit/ptm_objectspace2.frag"));
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getColorAverageProgramBuilder(PTMProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
            new File("shaders/common/texspace_noscale.vert"),
            new File("shaders/PTMfit/colorAverage.frag"));
    }

}
