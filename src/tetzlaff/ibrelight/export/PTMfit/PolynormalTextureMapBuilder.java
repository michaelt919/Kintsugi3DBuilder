package tetzlaff.ibrelight.export.PTMfit;

import java.util.Collections;
import java.util.function.*;
import java.util.stream.IntStream;

import tetzlaff.ibrelight.rendering.GraphicsStream;
import tetzlaff.optimization.LeastSquaresMatrixBuilder;
import tetzlaff.util.Counter;

public class PolynormalTextureMapBuilder {
    private final LeastSquaresMatrixBuilder matrixBuilder;
    private final int weightCount=6;
    private final int sampleCount;
    public PolynormalTextureMapBuilder(int width, int height){
        this.sampleCount=width*height;
        this.matrixBuilder = new LeastSquaresMatrixBuilder(sampleCount, weightCount,Collections.emptyList(), Collections.emptyList());
    }
    public void buildMatrices(GraphicsStream<LuminaceData> viewStream, PolynormalTextureMapModel PTMmodel)
    {
        IntConsumer sampleValidator = i -> {

        };
        matrixBuilder.buildMatrices(viewStream, PTMmodel, sampleValidator);

        //store color in ptm model
        Counter counter = new Counter();
        viewStream.forEach(reflectanceData ->
        {
            // store color for each pixal
            IntStream.range(0, this.sampleCount).parallel().forEach(p ->
            {
                PTMmodel.setRedchannel(p,reflectanceData.getLumin().getRed(p));
                PTMmodel.setGreenchannel(p,reflectanceData.getLumin().getGreen(p));
                PTMmodel.setBluechannel(p,reflectanceData.getLumin().getBlue(p));
            });
            synchronized (counter)
            {
                System.out.println("Finished store color of view " + counter.get() + '.');
                counter.increment();
            }
        });

    }

    public LeastSquaresMatrixBuilder getMatrixBuilder() {
        return matrixBuilder;
    }
}
