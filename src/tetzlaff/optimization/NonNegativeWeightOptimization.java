/*
 *  Copyright (c) Michael Tetzlaff 2021
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.optimization;

import java.util.Collections;
import java.util.List;
import java.util.function.*;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;
import tetzlaff.ibrelight.rendering.GraphicsStream;

public class NonNegativeWeightOptimization
{
    private static final double DEFAULT_TOLERANCE_SCALE = 0.000000000001;

    private final LeastSquaresMatrixBuilder matrixBuilder;

    public NonNegativeWeightOptimization(int sampleCount, int weightCount, List<IntToDoubleFunction> constraintWeights, List<Double> constraintsRHS)
    {
        matrixBuilder = new LeastSquaresMatrixBuilder(sampleCount, weightCount, constraintWeights, constraintsRHS);
    }

    public NonNegativeWeightOptimization(int sampleCount, int weightCount, IntToDoubleFunction constraintWeights, double constraintRHS)
    {
        // Non-negative weight optimization with a single constraints.
        this(sampleCount, weightCount, Collections.singletonList(constraintWeights), Collections.singletonList(constraintRHS));
    }

    public NonNegativeWeightOptimization(int sampleCount, int weightCount)
    {
        // Non-negative weight optimization with no constraints.
        this(sampleCount, weightCount, Collections.emptyList(), Collections.emptyList());
    }

    public <S, T> void buildMatrices(GraphicsStream<S> viewStream, LeastSquaresModel<S, T> leastSquaresModel, IntConsumer sampleValidator)
    {
        matrixBuilder.buildMatrices(viewStream, leastSquaresModel, sampleValidator);
    }

    public void optimizeWeights(IntPredicate areWeightsValid, BiConsumer<Integer, SimpleMatrix> weightSolutionConsumer, double toleranceScale)
    {
        for (int p = 0; p < matrixBuilder.sampleCount; p++)
        {
            if (areWeightsValid.test(p))
            {
                // Find the median value in the RHS of the system to help calibrate the tolerance scale.
                double median = IntStream.range(0, matrixBuilder.weightsQTrAugmented[p].getNumElements())
                    .mapToDouble(matrixBuilder.weightsQTrAugmented[p]::get)
                    .sorted()
                    .skip(matrixBuilder.weightsQTrAugmented[p].getNumElements() / 2)
                    .filter(x -> x > 0)
                    .findFirst()
                    .orElse(1.0);

                // Solve the system.
                weightSolutionConsumer.accept(p, NonNegativeLeastSquares.solvePremultipliedWithEqualityConstraints(
                    matrixBuilder.weightsQTQAugmented[p], matrixBuilder.weightsQTrAugmented[p],
                    median * toleranceScale, matrixBuilder.constraintCount));
            }
        }
    }

    public void optimizeWeights(IntPredicate areWeightsValid, BiConsumer<Integer, SimpleMatrix> weightSolutionConsumer)
    {
        optimizeWeights(areWeightsValid, weightSolutionConsumer, DEFAULT_TOLERANCE_SCALE);
    }
}
