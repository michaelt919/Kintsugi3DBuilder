package tetzlaff.optimization;

import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibrelight.export.PTMfit.CoefficientData;
import tetzlaff.ibrelight.export.PTMfit.LuminaceData;
//import tetzlaff.ibrelight.export.PTMfit.PTMData;

import java.util.function.IntFunction;

public class PolynormalTextureMapModel implements LeastSquaresModel<LuminaceData, Float>
{
//    private final CoefficientData coefficients;
//
//    public PolynormalTextureMapModel(CoefficientData coefficient) {
//        coefficients = coefficient;
//    }


    @Override
    public boolean isValid(LuminaceData sampleData, int systemIndex) {
        return sampleData.getLumin().getAlpha(systemIndex)!=0;
    }

    @Override
    public double getSampleWeight(LuminaceData sampleData, int systemIndex) {
        return 0;
    }

    @Override
    public Float getSamples(LuminaceData sampleData, int systemIndex) {
        Vector4 lumindata=sampleData.getLumin().get(systemIndex);
        return lumindata.x+lumindata.y+lumindata.z;
    }

    @Override
    //sampleDate: light dir
    public IntFunction<Float> getBasisFunctions(LuminaceData sampleData, int systemIndex) {
//
        // b :column of the p matrix, system index :row
        Float[] row=new Float[6];
        for(int i=0;i<6;i++){
            row[i]=sampleData.getLightdir()[6*systemIndex+i];
        }
        return b->
        {
            return row[b];
        };
    }

    @Override
    public double innerProduct(Float t1, Float t2) {
        return t1*t2;
    }
}
