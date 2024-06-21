/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.optimization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.*;
import java.util.stream.IntStream;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import kintsugi3d.builder.resources.ibr.stream.GraphicsStream;
import kintsugi3d.util.Counter;

/**
 * When working with image-based data, it is common to need to solve multiple least squares systems in parallel.
 * For example, when solving for certain kinds of texture maps, each pixel of the texture map may be its own least squares system.
 * This class contains a generic implementation of this kind of system.
 * It is set up to support taking input from a generic graphics stream
 * (typically data obtained from a sequence of GPU draw calls into an offscreen framebuffer),
 * and interprets that data as a set of samples that span all of the systems that need to be solved.
 * These samples are used to build the matrices for the actual systems that need to be solved to obtain a least squares solution.
 */
@SuppressWarnings("PublicField")
public class LeastSquaresMatrixBuilder
{
    public final SimpleMatrix[] weightsQTQAugmented;
    public final SimpleMatrix[] weightsQTrAugmented;

    public final int systemCount;
    public final int weightCount;
    public final int constraintCount;

    private int viewCount;

    /**
     * Construct a least squares matrix builder which may have constraints.
     * This does not actually build the matrices, just allocates space and sets the initial settings.
     * @param systemCount The number of systems that are being solved at once. Typically each pixel/texel is its own
     *                    system.  Sometimes, each red / green / blue channel at each pixel may also be its own system.
     * @param weightCount The number of basis function / weight pairs in the solution.
     * @param constraintWeights A list of constraints on the weights.  These constraints will be appended to the least
     *                          squares system.  If a given entry in constraintWeights maps (0, 1, 2) to (c0, c1, c2),
     *                          the constraint equation is c0 * w0 + c1 * w1 + c2 * w2 = b.
     *                          The "b" values are set in the subsequent parameter, constraintsRHS.
     *                          An empty list can be used here if no constraints are required.
     * @param constraintsRHS The RHS of the equation for each constraints (the "b" coefficients).
     *                       The length of this list must match the length of constraintWeights.
     */
    public LeastSquaresMatrixBuilder(int systemCount, int weightCount,
                                     List<IntToDoubleFunction> constraintWeights, List<Double> constraintsRHS)
    {
        this.systemCount = systemCount;
        this.weightCount = weightCount;
        this.constraintCount = constraintWeights.size();

        weightsQTQAugmented = IntStream.range(0, systemCount)
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

        weightsQTrAugmented = IntStream.range(0, systemCount)
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

    /**
     * Construct a least squares matrix builder with no constraints.
     * This does not actually build the matrices, just allocates space and sets the initial settings.
     * @param systemCount The number of systems that are being solved at once. Typically each pixel/texel is its own
     *                    system.  Sometimes, each red / green / blue channel at each pixel may also be its own system.
     * @param weightCount The number of basis function / weight pairs in the solution.
     */
    public LeastSquaresMatrixBuilder(int systemCount, int weightCount)
    {
        this(systemCount, weightCount, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Build the matrices that can be used to solve the system.
     * @param viewStream A stream of data from the GPU, which typically takes the form of a sequence of framebuffer data
     *                   from each view in the view set.
     * @param leastSquaresModel The least squares model that defines the basis functions that are being fit to as well
     *                          as the method for extracting the "ground truth" data from the view stream.
     * @param sampleValidator A callback that is invoked whenever a valid sample is encountered.
     *                        This can be used to track the number of (or even merely the existence of) valid samples
     *                        found for each system.
     * @param <S> The type of the data bundles coming from the graphics stream.
     *            Each bundle should contain no more than one valid sample for each system to be solved.
     *            For instance, this is typically the case when each bundle comes from a single view
     *            and each system is a unique position on a 3D surface.
     * @param <T> The type of a sample (ground truth data) after being processed and the evaluated basis functions.
     *            This type may be a multi-dimensional entity; for instance, a color with red, green, and blue components.
     *            When a sample is a multi-dimensional entity, it is assumed that a single solution will apply to all
     *            components.  Alternatively, if it is desired for each dimension to be solved for separately, each one
     *            should be its own system and the definition of a "system index" can be redefined accordingly.
     */
    public <S, T> void buildMatrices(GraphicsStream<S> viewStream, LeastSquaresModel<S, T> leastSquaresModel,
                                     IntConsumer sampleValidator)
    {
        buildMatrices(viewStream, leastSquaresModel, sampleValidator, 0, systemCount);
    }

    public <S, T> void buildMatrices(GraphicsStream<S> viewStream, LeastSquaresModel<S, T> leastSquaresModel,
        IntConsumer sampleValidator, int rangeStart, int rangeEnd)
    {
        Counter counter = new Counter();

        // Zero out all the matrices, except for the constraints.
        for (int p = 0; p < weightsQTQAugmented.length; p++)
        {
            for (int i = 0; i < weightCount; i++)
            {
                for (int j = 0; j < weightCount; j++)
                {
                    weightsQTQAugmented[p].set(i, j, 0);
                }

                weightsQTrAugmented[p].set(i, 0);
            }
        }

        viewCount = viewStream.getCount();

        viewStream.forEach(reflectanceData ->
        {
            // Update matrix for each pixel.
            // TODO: optimize performance by only rasterizing the pixels we're actually using?
            IntStream.range(rangeStart, rangeEnd).parallel().forEach(p ->
            {
                // Skip samples that aren't visible or are otherwise invalid.
                if (leastSquaresModel.isValid(reflectanceData, p))
                {
                    // Any time we have a visible, valid sample, mark that the corresponding texel is valid.
                    sampleValidator.accept(p);

                    double weight = leastSquaresModel.getSampleWeight(reflectanceData, p);

                    // Evaluate sampler (get the ground truth value)
                    T fActual = leastSquaresModel.getSamples(reflectanceData, p);

                    // Evaluate the "basisCalculator" to get another function that can provide the actual basis function values.
                    IntFunction<T> basisFunctions = leastSquaresModel.getBasisFunctions(reflectanceData, p);

                    ArrayList<T> basisEval = new ArrayList<T>(weightCount);

                    for (int b = 0; b < weightCount; b++)
                    {
                        // Evaluate the basis function.
                        basisEval.add(basisFunctions.apply(b));
                    }

                    for (int b1 = 0; b1 < weightCount; b1++)
                    {
                        T f1 = basisEval.get(b1);

                        // Store the weighted product of the basis function and the actual sample in the vector.
                        weightsQTrAugmented[p - rangeStart].set(b1, weightsQTrAugmented[p - rangeStart].get(b1) + weight * leastSquaresModel.innerProduct(f1, fActual));

                        for (int b2 = 0; b2 < weightCount; b2++)
                        {
                            T f2 = basisEval.get(b2);

                            // Store the weighted product of the two basis functions in the matrix.
                            weightsQTQAugmented[p - rangeStart].set(b1, b2,
                                weightsQTQAugmented[p - rangeStart].get(b1, b2) + weight * leastSquaresModel.innerProduct(f1, f2));
                        }
                    }
                }
            });

            synchronized (counter)
            {
//                System.out.println("Finished view " + counter.get() + '.');
                counter.increment();
            }
        });
    }

    public int getViewCount()
    {
        return viewCount;
    }
}
