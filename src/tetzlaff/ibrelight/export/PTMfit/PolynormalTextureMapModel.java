package tetzlaff.ibrelight.export.PTMfit;

import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibrelight.export.PTMfit.CoefficientData;
import tetzlaff.ibrelight.export.PTMfit.LuminaceData;
import tetzlaff.optimization.LeastSquaresModel;
//import tetzlaff.ibrelight.export.PTMfit.PTMData;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.IntFunction;

import static java.lang.Float.NaN;
import static java.lang.Float.isNaN;

public class PolynormalTextureMapModel implements LeastSquaresModel<LuminaceData, Float>
{
//    private final CoefficientData coefficients;
//
//    public PolynormalTextureMapModel(CoefficientData coefficient) {
//        coefficients = coefficient;
//    }
    private int width;
    private int length;
    private float[] redchannel;
    private float[] greenchannel;
    private float[] bluechannel;
    public PolynormalTextureMapModel(int Width,int Length){
        width=Width;
        length=Length;
        redchannel=new float[width*length];
        greenchannel=new float[width*length];
        bluechannel=new float[width*length];

    }
    @Override
    public boolean isValid(LuminaceData sampleData, int systemIndex) {
        if(systemIndex<width*length) return sampleData.getLumin().getRed(systemIndex)!=0;
        else if(systemIndex>=width*length && systemIndex<width*length) return sampleData.getLumin().getGreen(systemIndex%(width*length))!=0;
        else return sampleData.getLumin().getBlue(systemIndex%(width*length))!=0;
    }

    @Override
    public double getSampleWeight(LuminaceData sampleData, int systemIndex) {
//        return 0;
        return 0.5;
    }

    @Override
    public Float getSamples(LuminaceData sampleData, int systemIndex) {
        Vector4 lumindata=sampleData.getLumin().get(systemIndex%(width*length));
        Float result=0.0f;
        if (!isNaN(lumindata.x)) result+=lumindata.x;
        if (!isNaN(lumindata.y)) result+=lumindata.y;
        if (!isNaN(lumindata.z)) result+=lumindata.z;

        return result;
    }

    @Override
    //sampleDate: light dir
    public IntFunction<Float> getBasisFunctions(LuminaceData sampleData, int systemIndex) {
//
        // b :column of the p matrix, system index :row
        Float u=sampleData.getLightdir().getRed(systemIndex%(width*length));
        Float v=sampleData.getLightdir().getGreen(systemIndex%(width*length));
        Float w=sampleData.getLightdir().getBlue(systemIndex%(width*length));
        Float[] row={1.0f,u,v,w,u*u,v*u};

        return b->
        {
            return row[b%6];
        };
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
