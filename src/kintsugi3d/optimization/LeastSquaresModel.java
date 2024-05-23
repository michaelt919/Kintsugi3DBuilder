/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.optimization;

import java.util.function.IntFunction;

/**
 * When working with image-based data, it is common to need to solve multiple least squares systems in parallel.
 * For example, when solving for certain kinds of texture maps, each pixel of the texture map may be its own least squares system.
 * This class is an abstraction of all the information that needs to be known about the model that is being fit to
 * in order to use the generic LeastSquaresMatrixBuilder class.
 * @param <S> The type of the data bundles coming from the graphics stream.
 *            Each bundle should contain no more than one valid sample for each system to be solved.
 *            For instance, this is typically the case when each bundle comes from a single view and each system is a unique position on a 3D surface.
 * @param <T> The type of a sample (ground truth data) after being processed and the evaluated basis functions.
 *            This type may be a multi-dimensional entity; for instance, a color with red, green, and blue components.
 *            When a sample is a multi-dimensional entity, it is assumed that a single solution will apply to all components.
 *            Alternatively, if it is desired for each dimension to be solved for separately, each one should be its own system
 *            and the definition of a "system index" can be redefined accordingly.
 */
public interface LeastSquaresModel<S, T>
{
    /**
     * Some system indices may correspond to hypothetical systems that are not valid in practice for a particular data set
     * (for instance, a texture map may contain lots of pixels that don't actually map onto the surface of the 3D object).
     * This function takes as input the bundled data for a set of samples (no more than one sample for each system)
     * and tests if it contains a valid sample for a particular system (thus ensuring that system has at least one valid sample).
     * @param sampleData The bundled input data
     * @param systemIndex The index of the system to check for a valid sample.
     * @return True if a valid sample was found for that system; false otherwise.
     */
    boolean isValid(S sampleData, int systemIndex);

    /**
     * Often, least squares problems are weighted.  This function takes as input the bundled data for a set of samples
     * (no more than one sample for each system) and returns a weight that should be used for the corresponding sample for that system.
     * It can be assumed that if this function is being evaluated, isValid(sampleData, systemIndex) is true.
     * @param sampleData The bundled input data
     * @param systemIndex The index of the system for which to evaluate the weight function.
     * @return The weight for all of the samples in the current data bundle for the specified system.
     */
    double getSampleWeight(S sampleData, int systemIndex);

    /**
     * This function actually retrieves the sample from the data bundle (which may be multi-dimensional) for a particular system.
     * It can be assumed that if this function is being evaluated, isValid(sampleData, systemIndex) is true.
     * @param sampleData The bundled input data
     * @param systemIndex The index of the system for which to process the samples.
     * @return The set of samples that were produced for the specified system.
     */
    T getSamples(S sampleData, int systemIndex);

    /**
     * This function takes as input the bundled data for a set of samples (no more than one sample for each system) and provides an object that is
     * capable of mapping the indices of the basis functions to the actual value of the basis function for the sample of a particular system.
     * This object should pre-process as much as possible immediately so that the mapping function (which will be evaluated multiple times)
     * is as efficient as possible.  This can often be accomplished using a closure facilitated by a lambda expression.
     * It can be assumed that if this function is being evaluated, isValid(sampleData, systemIndex) is true.
     * @param sampleData The bundled input data
     * @param systemIndex The index of the system for which to evaluate basis functions.
     * @return A mapping from the weight / basis function indices to the evaluated basis functions.
     */
    IntFunction<T> getBasisFunctions(S sampleData, int systemIndex);

    /**
     * Gets the number of basis functions
     * @return
     */
    int getBasisFunctionCount();

    /**
     * An inner product function that takes two evaluated basis functions, or a sample and a single evaluated basis function
     * (each of which may be multi-dimensional), multiplies the corresponding components and adds them together.
     * This is necessary because, in practice, samples (and thus, basis functions as well) contain multiple dimensions of data,
     * for example, red, green, and blue components for color.
     * When this is the case, it is assumed that the system must consider each component as a separate data sample for the same solution.
     * The inner product function reduces these multi-dimensional entities to a single floating-point number to store in the matrix or vector.
     * It is expected that this operation is commutative.
     * @param t1 The left-hand operand of the inner product.
     * @param t2 The right-hand operand of the inner product.
     * @return The inner product between the operands.
     */
    double innerProduct(T t1, T t2);



}
