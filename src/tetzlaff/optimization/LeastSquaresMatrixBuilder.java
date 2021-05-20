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

import java.util.List;
import java.util.function.*;
import java.util.stream.IntStream;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.ibrelight.rendering.GraphicsStream;
import tetzlaff.util.Counter;

@SuppressWarnings("PublicField")
public class LeastSquaresMatrixBuilder
{
    public final SimpleMatrix[] weightsQTQAugmented;
    public final SimpleMatrix[] weightsQTrAugmented;

    public final int sampleCount;
    public final int weightCount;
    public final int constraintCount;

    public LeastSquaresMatrixBuilder(int sampleCount, int weightCount, List<IntToDoubleFunction> constraintWeights, List<Double> constraintsRHS)
    {
        this.sampleCount = sampleCount;
        this.weightCount = weightCount;
        this.constraintCount = constraintWeights.size();

        weightsQTQAugmented = IntStream.range(0, sampleCount)
            .mapToObj(p ->
            {
                SimpleMatrix mQTQAugmented = new SimpleMatrix(
                    weightCount + constraintWeights.size(),
                    weightCount + constraintWeights.size(),
                    DMatrixRMaj.class);

                // Set up LHS of constraints
                for (int i = 0; i < constraintWeights.size(); i++)
                {
                    for (int b = 0; b < weightCount; b++)
                    {
                        double constraintValue = constraintWeights.get(i).applyAsDouble(b);
                        mQTQAugmented.set(b, weightCount + i, constraintValue);
                        mQTQAugmented.set(weightCount + i, b, constraintValue);
                    }
                }

                return mQTQAugmented;
            })
            .toArray(SimpleMatrix[]::new);

        weightsQTrAugmented = IntStream.range(0, sampleCount)
            .mapToObj(p ->
            {
                SimpleMatrix mQTrAugmented = new SimpleMatrix(weightCount + constraintWeights.size(), 1, DMatrixRMaj.class);

                // Set up RHS of constraints
                for (int i = 0; i < constraintWeights.size(); i++)
                {
                    mQTrAugmented.set(weightCount + i, constraintsRHS.get(i));
                }
                return mQTrAugmented;
            })
            .toArray(SimpleMatrix[]::new);
    }

    public <S, T> void buildMatrices(GraphicsStream<S> viewStream, BiPredicate<S, Integer> validityTest,
        IntConsumer sampleValidator, ToDoubleBiFunction<S, Integer> weightFunction, BiFunction<S, Integer, T> sampler,
        BiFunction<S, Integer, IntFunction<T>> basisCalculator, ToDoubleBiFunction<T, T> innerProduct)
    {
        Counter counter = new Counter();

        viewStream.forEach(reflectanceData ->
        {
            // Update matrix for each pixel.
            IntStream.range(0, sampleCount).parallel().forEach(p ->
            {
                // Skip samples that aren't visible or are otherwise invalid.
                if (validityTest.test(reflectanceData, p))
                {
                    // Any time we have a visible, valid sample, mark that the corresponding texel is valid.
                    sampleValidator.accept(p);

                    double weight = weightFunction.applyAsDouble(reflectanceData, p);
                    double weightSquared = weight * weight;

                    // Evaluate sampler (get the ground truth value)
                    T fActual = sampler.apply(reflectanceData, p);

                    // Evaluate the "basisCalculator" to get another function that can provide the actual basis function values.
                    IntFunction<T> basisFunctions = basisCalculator.apply(reflectanceData, p);

                    for (int b1 = 0; b1 < weightCount; b1++)
                    {
                        // Evaluate the first basis function.
                        T f1 = basisFunctions.apply(b1);

                        // Store the weighted product of the basis function and the actual sample in the vector.
                        weightsQTrAugmented[p].set(b1, weightsQTrAugmented[p].get(b1) + weightSquared * innerProduct.applyAsDouble(f1, fActual));

                        for (int b2 = 0; b2 < weightCount; b2++)
                        {
                            T f2 = basisFunctions.apply(b2);

                            // Store the weighted product of the two basis functions in the matrix.
                            weightsQTQAugmented[p].set(b1, b2,
                                weightsQTQAugmented[p].get(b1, b2) + weightSquared * innerProduct.applyAsDouble(f1, f2));
                        }
                    }
                }
            });

            synchronized (counter)
            {
                System.out.println("Finished view " + counter.get() + '.');
                counter.increment();
            }
        });
    }
}
