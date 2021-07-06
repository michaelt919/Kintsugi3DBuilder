package tetzlaff.ibrelight.export.PTMfit;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.SingularMatrixException;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.ColorFormat;
import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.core.TextureFitSettings;

import tetzlaff.ibrelight.rendering.GraphicsStreamResource;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.optimization.LeastSquaresMatrixBuilder;
import tetzlaff.optimization.NonNegativeLeastSquares;

import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public class PTMOptimiztion <ContextType extends Context<ContextType>>{
    private TextureFitSettings settings;
    private PolynormalTextureMapBuilder mapbuilder;


    public PTMOptimiztion(TextureFitSettings setting){
        settings=setting;
        mapbuilder=new PolynormalTextureMapBuilder(settings.width,settings.height);

    }

    public <ContextType extends Context<ContextType>> void createFit(IBRResources<ContextType> resources)
            throws IOException{
        ContextType context = resources.context;
        context.getState().disableBackFaceCulling();

        PTMProgramFactory programFactory=new PTMProgramFactory(resources);
        try(
        GraphicsStreamResource<ContextType> LuminaceStream = resources.streamAsResource(
                getLuminaceProgramBuilder(programFactory),
                context.buildFramebufferObject(settings.width, settings.height)
                        .addColorAttachment(ColorFormat.RGBA32F)
                        .addColorAttachment(ColorFormat.RGBA32F));
        )
        {
            System.out.println("Building weight fitting matrices...");
//            PolynormalTextureMapModel solution=new PolynormalTextureMapModel();
            PTMsolution solution=new PTMsolution(settings);

            mapbuilder.buildMatrices(LuminaceStream.map(framebufferData -> new LuminaceData(framebufferData[0], framebufferData[1]))
                    ,solution.getPTMmodel());
            System.out.println("Finished building matrices; solving now...");

            optimizeWeights(p->settings.height * settings.width != 0,solution::setWeights);
            System.out.println("DONE!");

                // write out weight textures for debugging
            solution.saveWeightMaps();
        }

    }

    public void optimizeWeights(IntPredicate areWeightsValid, BiConsumer<Integer, SimpleMatrix> weightSolutionConsumer, double toleranceScale)
    {
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
                    e.printStackTrace();
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

}
