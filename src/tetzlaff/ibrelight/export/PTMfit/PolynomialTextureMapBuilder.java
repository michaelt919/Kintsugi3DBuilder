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

package tetzlaff.ibrelight.export.PTMfit;

import java.util.Collections;
import java.util.function.*;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tetzlaff.ibrelight.rendering.resources.GraphicsStream;
import tetzlaff.optimization.LeastSquaresMatrixBuilder;
import tetzlaff.util.Counter;

public class PolynomialTextureMapBuilder
{
    private static final Logger log = LoggerFactory.getLogger(PolynomialTextureMapBuilder.class);
    private LeastSquaresMatrixBuilder matrixBuilder;
    private int weightCount;
    private final int sampleCount;
    public PolynomialTextureMapBuilder(int width, int height){
        this.sampleCount=width*height*3;
    }
    public void buildMatrices(GraphicsStream<LuminanceData> viewStream, PolynomialTextureMapModel PTMmodel, PTMsolution solution)
    {
        this.weightCount = PTMmodel.getBasisFunctionCount();
        this.matrixBuilder = new LeastSquaresMatrixBuilder(sampleCount, weightCount,Collections.emptyList(), Collections.emptyList());

        IntConsumer sampleValidator = i -> {solution.setWeightsValidity(i,true);};
        matrixBuilder.buildMatrices(viewStream, PTMmodel, sampleValidator);

        //store color in ptm model
        Counter counter = new Counter();
        viewStream.forEach(reflectanceData ->
        {
            // store color for each pixal
            IntStream.range(0, this.sampleCount).parallel().forEach(p ->
            {
                if(p<this.sampleCount/3) PTMmodel.setRedchannel(p%this.sampleCount/3,reflectanceData.getLumin().getRed(p%this.sampleCount/3));
                else if(this.sampleCount/3<=p && p<2*this.sampleCount/3)PTMmodel.setGreenchannel(p%this.sampleCount/3,reflectanceData.getLumin().getGreen(p%this.sampleCount/3));
                else PTMmodel.setBluechannel(p%this.sampleCount/3,reflectanceData.getLumin().getBlue(p%this.sampleCount/3));
            });
            synchronized (counter)
            {
                log.info("Finished storing color of view " + counter.get() + '.');
                counter.increment();
            }
        });

    }

    public LeastSquaresMatrixBuilder getMatrixBuilder() {
        return matrixBuilder;
    }
}
