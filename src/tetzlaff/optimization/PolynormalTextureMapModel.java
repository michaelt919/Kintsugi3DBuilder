package tetzlaff.optimization;
import java.util.function.IntFunction;

import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.ibrelight.export.specularfit.ReflectanceData;

public class PolynormalTextureMapModel implements LeastSquaresModel<ReflectanceData, DoubleVector3>
{

    @Override
    public boolean isValid(ReflectanceData sampleData, int systemIndex) {
        return sampleData.getVisibility(systemIndex) > 0;
    }

    @Override
    public double getSampleWeight(ReflectanceData sampleData, int systemIndex) {
        return 0;
    }

    @Override
    public DoubleVector3 getSamples(ReflectanceData sampleData, int systemIndex) {
        return sampleData.getColor(systemIndex).asDoublePrecision();
    }

    @Override
    public IntFunction<DoubleVector3> getBasisFunctions(ReflectanceData sampleData, int systemIndex) {
        return null;
    }

    @Override
    public double innerProduct(DoubleVector3 t1, DoubleVector3 t2) {
        return t1.dot(t2);
    }
}
