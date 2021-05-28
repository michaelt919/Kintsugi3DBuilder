package tetzlaff.optimization;

import tetzlaff.ibrelight.export.PTMfit.CoefficientData;
import tetzlaff.ibrelight.export.PTMfit.LuminaceData;
import tetzlaff.ibrelight.export.PTMfit.PTMData;

import java.util.function.IntFunction;

public class PolynormalTextureMapModel implements LeastSquaresModel<PTMData, LuminaceData>
{
    private final CoefficientData coefficients;

    public PolynormalTextureMapModel(CoefficientData coefficient) {
        coefficients = coefficient;
    }


    @Override
    public boolean isValid(PTMData sampleData, int systemIndex) {
        return sampleData.getRownumber()>=systemIndex;
    }

    @Override
    public double getSampleWeight(PTMData sampleData, int systemIndex) {
        return 0;
    }

    @Override
    public LuminaceData getSamples(PTMData sampleData, int systemIndex) {
        return sampleData.getRealLumin();
    }

    @Override
    public IntFunction<LuminaceData> getBasisFunctions(PTMData sampleData, int systemIndex) {
        int index= sampleData.getRownumber();
        return b->
        {
            LuminaceData Luminance= new LuminaceData(sampleData.getRownumber(),coefficients.getX(),coefficients.getY());
            double[] p_i= sampleData.getProw(systemIndex);
            double[] c_xy=coefficients.getCoeff();
            for(int i=0;i<6;i++){
                Luminance.setLumin(index,Luminance.getLumin(index)+p_i[i]*c_xy[i]);
            }
            return Luminance;
        };
    }

    @Override
    public double innerProduct(LuminaceData t1, LuminaceData t2) {
        return t1.dot(t2);
    }
}
