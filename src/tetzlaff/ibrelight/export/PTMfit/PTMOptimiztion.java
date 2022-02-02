package tetzlaff.ibrelight.export.PTMfit;

import org.ejml.data.SingularMatrixException;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.ColorFormat;
import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.core.TextureFitSettings;

import tetzlaff.ibrelight.rendering.GraphicsStreamResource;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.optimization.LeastSquaresMatrixBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public class PTMOptimiztion <ContextType extends Context<ContextType>>{
    private TextureFitSettings settings;
    private PolynomialTextureMapBuilder mapbuilder;


    public PTMOptimiztion(TextureFitSettings setting){
        settings=setting;
        mapbuilder=new PolynomialTextureMapBuilder(settings.width,settings.height);

    }

    public void createFit(IBRResources<ContextType> resources)
            throws IOException{
        ContextType context = resources.context;
        context.getState().disableBackFaceCulling();

        PTMProgramFactory<ContextType> programFactory=new PTMProgramFactory<>(resources);
        try(
        GraphicsStreamResource<ContextType> LuminaceStream = resources.streamAsResource(
                getLuminaceProgramBuilder(programFactory),
                context.buildFramebufferObject(settings.width, settings.height)
                        .addColorAttachment(ColorFormat.RGBA32F)
                        .addColorAttachment(ColorFormat.RGBA32F));
        )
        {
            resources.setupShaderProgram(LuminaceStream.getProgram());
            System.out.println("Building weight fitting matrices...");
//            PolynomialTextureMapModel solution=new PolynomialTextureMapModel();
            PTMsolution solution=new PTMsolution(settings);

            mapbuilder.buildMatrices(LuminaceStream.map(framebufferData -> {
                try {
                    LuminaceStream.getFramebuffer().saveColorBufferToFile(1, "PNG", new File(settings.outputDirectory, "test.png"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return new LuminanceData(framebufferData[0], framebufferData[1]);
            }), solution.getPTMmodel(),solution);
//            int index=0;
//            LuminaceStream.forEach(Lumin->{
//                ColorList[] colorlist = Lumin.clone();
//                solution.getPTMmodel().setRedchannel(index,colorlist[0].getRed(index));
//                solution.getPTMmodel().setGreenchannel(index,colorlist[0].getGreen(index));
//                solution.getPTMmodel().setBluechannel(index,colorlist[0].getBlue(index));
//
//            });
            System.out.println("Finished building matrices; solving now...");
            optimizeWeights(p->settings.height * settings.width != 0,solution::setWeights);
            System.out.println("DONE!");

                // write out weight textures for debugging
            fillHoles(solution);
            solution.saveWeightMaps();

            PTMReconstruction reconstruct=new PTMReconstruction(resources,settings);
            reconstruct.reconstruct(solution,getReconstructionProgramBuilder(programFactory),"reconstruction");

//            fillHoles(solution);


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

                        for (int b = 0; b < 6; b++)
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

                            if (sum > 0.0)
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

        LeastSquaresMatrixBuilder matrixBuilder=mapbuilder.getMatrixBuilder();
        for (int p = 0; p < mapbuilder.getMatrixBuilder().systemCount; p++)
        {
            if (areWeightsValid.test(p))
            {
                // Find the median value in the RHS of the system to help calibrate the tolerance scale.
                double median = IntStream.range(0, matrixBuilder.weightsQTrAugmented[p].getNumElements())
                        .mapToDouble(matrixBuilder.weightsQTrAugmented[p]::get)
                        .sorted()
                        .skip(matrixBuilder.weightsQTrAugmented[p].getNumElements() / 2)
                        .filter(x -> x > 0)
                        .findFirst()
                        .orElse(1.0);

                // Solve the system.
                try{
                    weightSolutionConsumer.accept(p, matrixBuilder.weightsQTQAugmented[p].solve(matrixBuilder.weightsQTrAugmented[p]));
                }
                catch (SingularMatrixException e){
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
    ProgramBuilder<ContextType> getLuminaceProgramBuilder(PTMProgramFactory<ContextType> programFactory)
    {
        return programFactory.getShaderProgramBuilder(
                new File("shaders/common/texspace_noscale.vert"),
                new File("shaders/PTMfit/PTMShader.frag"));
    }
    ProgramBuilder<ContextType> getReconstructionProgramBuilder(PTMProgramFactory<ContextType> programFactory){
        return programFactory.getShaderProgramBuilder(
                new File("shaders/common/imgspace.vert"),
                new File("shaders/PTMfit/PTMreconstruction.frag"));
    }

}
