/*
 *  Copyright (c) Zhangchi (Josh) Lyu, Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.export.PTMfit;

import kintsugi3d.optimization.LeastSquaresModel;

import java.util.function.IntFunction;

import static java.lang.Float.isNaN;

public class PolynomialTextureMapModel implements LeastSquaresModel<LuminanceData, Float>
{
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
    }

    @Override
    public double getSampleWeight(LuminanceData sampleData, int systemIndex) {
        return 0.5;
    }

    @Override
    public Float getSamples(LuminanceData sampleData, int systemIndex) {
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
        Float[] row={1.0f,u,v,w,v*u, u*u + v*v};

        return b->
        {
            return row[b%row.length];
        };
    }

    @Override
    public int getBasisFunctionCount()
    {
        return 6;
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
