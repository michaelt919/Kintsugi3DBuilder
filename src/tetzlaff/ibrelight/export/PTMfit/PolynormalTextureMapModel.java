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


    @Override
    public boolean isValid(LuminaceData sampleData, int systemIndex) {
        return sampleData.getLumin().getAlpha(systemIndex)!=0;
    }

    @Override
    public double getSampleWeight(LuminaceData sampleData, int systemIndex) {
//        return 0;
        return 0.5;
    }

    @Override
    public Float getSamples(LuminaceData sampleData, int systemIndex) {
        Vector4 lumindata=sampleData.getLumin().get(systemIndex);
        Float result=0.0f;
        if (isNaN(lumindata.x)) result+=lumindata.x;
        if (isNaN(lumindata.y)) result+=lumindata.y;
        if (isNaN(lumindata.z)) result+=lumindata.z;

        return result;
    }

    @Override
    //sampleDate: light dir
    public IntFunction<Float> getBasisFunctions(LuminaceData sampleData, int systemIndex) {
//
        // b :column of the p matrix, system index :row
        Float u=sampleData.getLightdir().getRed(systemIndex);
        Float v=sampleData.getLightdir().getGreen(systemIndex);
        Float w=sampleData.getLightdir().getBlue(systemIndex);
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


}
