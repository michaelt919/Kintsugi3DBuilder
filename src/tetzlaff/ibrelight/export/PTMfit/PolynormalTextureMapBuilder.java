package tetzlaff.ibrelight.export.PTMfit;

import java.util.Collections;
import java.util.List;
import java.util.function.*;
import java.util.stream.IntStream;
import tetzlaff.ibrelight.export.PTMfit.PolynormalTextureMapModel;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.ibrelight.rendering.GraphicsStream;
import tetzlaff.optimization.LeastSquaresMatrixBuilder;
import tetzlaff.optimization.LeastSquaresModel;

public class PolynormalTextureMapBuilder {
    private final LeastSquaresMatrixBuilder matrixBuilder;
    private final int weightCount=6;
    private final int sampleCount;
    public PolynormalTextureMapBuilder(int width, int height){
        this.sampleCount=width*height;
        this.matrixBuilder = new LeastSquaresMatrixBuilder(sampleCount, weightCount,Collections.emptyList(), Collections.emptyList());
    }
    public <S, T> void buildMatrices(GraphicsStream<LuminaceData> viewStream, PolynormalTextureMapModel PTMmodel)
    {
        IntConsumer sampleValidator = i -> {};
        matrixBuilder.buildMatrices(viewStream, PTMmodel, sampleValidator);
    }
}
