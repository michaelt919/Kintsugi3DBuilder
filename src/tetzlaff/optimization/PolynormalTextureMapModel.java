package tetzlaff.optimization;

import tetzlaff.ibrelight.export.PTMfit.CoefficientData;
import tetzlaff.ibrelight.export.PTMfit.LuminaceData;
//import tetzlaff.ibrelight.export.PTMfit.PTMData;

import java.util.function.IntFunction;

public class PolynormalTextureMapModel implements LeastSquaresModel<LuminaceData, Float[]>
{
//    private final CoefficientData coefficients;
//
//    public PolynormalTextureMapModel(CoefficientData coefficient) {
//        coefficients = coefficient;
//    }


    @Override
    public boolean isValid(LuminaceData sampleData, int systemIndex) {
        return systemIndex*6+5<sampleData.getLightdir().length;
    }

    @Override
    public double getSampleWeight(LuminaceData sampleData, int systemIndex) {
        return 0;
    }

    @Override
    public Float[] getSamples(LuminaceData sampleData, int systemIndex) {
        return sampleData.getLumin();
    }

    @Override
    //sampleDate: light dir
    public IntFunction<Float[]> getBasisFunctions(LuminaceData sampleData, int systemIndex) {
//        int index= sampleData.getRownumber();
        // b :column of the p matrix, system index :row
        return b->
        {
            Float[] res=new Float[sampleData.getsize()];
            for(int i=0;i<sampleData.getsize();i++){
                res[i]=sampleData.getLightdir()[6*i+systemIndex];
            }
            return res;
        };
    }

    @Override
    public double innerProduct(Float[] t1, Float[] t2) {
        double res=0;
        for(int i=0;i<t1.length;i++){
            res+=t1[i]*t2[i];
        }
        return res;
    }
}
