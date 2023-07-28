/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.optimization.function;

import kintsugi3d.optimization.MatrixSystem;

import java.util.function.IntToDoubleFunction;
import java.util.function.ObjIntConsumer;

public interface BasisFunctions
{
    /**
     * Evaluates a particular function in this "library" of basis functions at a particular integer value.
     * @param functionIndex The index of the function to evaluate.
     * @param value The value at which to evaluate the function.
     * @return The result of the evaluated function.
     */
    double evaluate(int functionIndex, int value);

    /**
     * How many basis functions are in this library.
     * @return The number of basis functions.
     */
    int getFunctionCount();

    /**
     * The number of discrete elements in the domain of the function to be optimized.
     * @return The number of elements in the domain.
     */
    int getOptimizedDomainSize();

    /**
     * Gets the assumed "metallicity" of the function being optimized.  This has to do with the constant term
     * that is optimized alongside the weights for the basis functions (i.e. diffuse for a BRDF).  A function that is
     * 100% "metallic" uses the same analytic factor for the constant term as for the basis functions.
     * A function that is 0% metallicy uses no analytic factor for the constant term (just a simple constant).
     * @return The metallicity being assumed.
     */
    double getMetallicity();

    /**
     * Add the contibutions of samples with a particular value to a fitting system.
     * IMPORTANT: It is assumed that the samples have been pre-sorted by value.
     * This function should only be called immediately before a new value is visited
     * (so the previous value is less than the current value).
     * @param valueCurrent The value of the current sample (which must be less than the value of the next sample).
     * @param valueNext The value of the next sample.
     * @param instanceCount The number of "instances" being optimized in the fitting system.
     * @param sums Running totals for all samples with the previous value that should be incorporated
     *             into the fitting matrix at this time.
     * @param fittingSystem The matrix system for the fitting problem being solved.
     */
    void contributeToFittingSystem(int valueCurrent, int valueNext, int instanceCount,
                                   MatrixBuilderSums sums, MatrixSystem fittingSystem);

    /**
     * Evaluate a solution vector using this set of basis functions and interpret it as a function.
     * @param constantTerm The optimized value of the constant term.  (This is used when metallicity > 0).
     * @param nonConstantSolution The optimized non-constant solution.  This should be a function that takes the index
     *                            of a function in this "basis library" and returns the best weight for that function.
     * @param functionConsumer An object that consumes the evaluated function.
     *                         Each element in the domain will be evaluated, but no particular order is guaranteed.
     *                         The integer passed to the consumer will be the index of the element being evaluated.
     */
    void evaluateSolution(double constantTerm, IntToDoubleFunction nonConstantSolution, ObjIntConsumer<Double> functionConsumer);
}
