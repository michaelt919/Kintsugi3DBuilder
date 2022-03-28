package tetzlaff.ibrelight.export.PTMfit;

import tetzlaff.optimization.LeastSquaresModel;
//import tetzlaff.ibrelight.export.PTMfit.PTMData;

import java.util.function.IntFunction;

import static java.lang.Float.isNaN;

public class PolynomialTextureMapModel implements LeastSquaresModel<LuminanceData, Float>
{
//    private final CoefficientData coefficients;
//
//    public PolynomialTextureMapModel(CoefficientData coefficient) {
//        coefficients = coefficient;
//    }
    private int width;
    private int length;
    private float[] redchannel;
    private float[] greenchannel;
    private float[] bluechannel;
    public PolynomialTextureMapModel(int Width,int Length){
        width=Width;
        length=Length;
        redchannel=new float[width*length];
        greenchannel=new float[width*length];
        bluechannel=new float[width*length];

    }
    @Override
    public boolean isValid(LuminanceData sampleData, int systemIndex) {
        int pixelIndex = systemIndex % (width * length);
        return sampleData.getLumin().getAlpha(pixelIndex) > 0.99
            && !isNaN(sampleData.getLumin().getRed(pixelIndex))
            && !isNaN(sampleData.getLumin().getGreen(pixelIndex))
            && !isNaN(sampleData.getLumin().getBlue(pixelIndex));
//        if(systemIndex<width*length) return sampleData.getLumin().getRed(systemIndex)!=0;
//        else if(systemIndex<2*width*length) return sampleData.getLumin().getGreen(systemIndex%(width*length))!=0;
//        else return sampleData.getLumin().getBlue(systemIndex%(width*length))!=0;
    }

    @Override
    public double getSampleWeight(LuminanceData sampleData, int systemIndex) {
//        return 0;
        return 0.5;
    }

    @Override
    public Float getSamples(LuminanceData sampleData, int systemIndex) {
//        Vector4 lumindata=sampleData.getLumin().get(systemIndex%(width*length));
//        Float result=0.0f;

//        if (!isNaN(lumindata.x)) result+=lumindata.x;
//
//        if (!isNaN(lumindata.y)) result+=lumindata.y;
//
//        if (!isNaN(lumindata.z)) result+=lumindata.z;
        if(systemIndex<width*length) {
            return sampleData.getLumin().getRed(systemIndex);
        }
        else if(systemIndex<2*width*length){
            return sampleData.getLumin().getGreen(systemIndex%(width*length));
        }
        else{
            return sampleData.getLumin().getBlue(systemIndex%(width*length));
        }

    }

    @Override
    //sampleDate: light dir
    public IntFunction<Float> getBasisFunctions(LuminanceData sampleData, int systemIndex) {
//
        // b :column of the p matrix, system index :row
        Float u=sampleData.getLightdir().getRed(systemIndex%(width*length));
        Float v=sampleData.getLightdir().getGreen(systemIndex%(width*length));
        Float w=sampleData.getLightdir().getBlue(systemIndex%(width*length));
        Float[] row={1.0f,u,v,w,u*u,v*v, w*w, v*u, w*u, v*w};

        return b->
        {
            return row[b%row.length];
        };
    }

    @Override
    public int getBasisFunctionCount()
    {
        return 10;
    }

    @Override
    public double innerProduct(Float t1, Float t2) {
        return t1*t2;
    }

    public void setBluechannel(int index, float value) {
        this.bluechannel[index]=value;
    }
    public void setRedchannel(int index, float value) {
        this.redchannel[index]=value;
    }
    public void setGreenchannel(int index, float value) { this.greenchannel[index]=value; }
}
