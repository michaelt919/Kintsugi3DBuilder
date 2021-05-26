package tetzlaff.optimization;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.ibrelight.export.PTMfit.PTMData;
import tetzlaff.optimization.LeastSquaresModel;

public class PolynormalTextureMapModel implements LeastSquaresModel<ReflectanceData, DoubleVector3>
{

    @Override
    public boolean isValid(PTMData sampleData, int systemIndex) {
        return sampleData.getVisibility(systemIndex) > 0;
    }

    @Override
    public double getSampleWeight(PTMData sampleData, int systemIndex) {
        return 0;
    }

    @Override
    public DoubleVector3 getSamples(PTMData sampleData, int systemIndex) {
        return sampleData.getColor(systemIndex).asDoublePrecision();
    }

    @Override
    public IntFunction<T> getBasisFunctions(PTMData sampleData, int systemIndex) {
        return null;
    }

    @Override
    public double innerProduct(DoubleVector3 t1, DoubleVector3 t2) {
        return t1.dot(t2);
    }
}
