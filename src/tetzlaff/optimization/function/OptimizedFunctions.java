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

package tetzlaff.optimization.function;

import org.ejml.simple.SimpleMatrix;
import tetzlaff.optimization.MatrixSystem;

import java.util.function.ObjIntConsumer;
import java.util.stream.IntStream;

/**
 * Class to store the results of optimizing a small set of functions using a larger library of basis functions.
 */
public class OptimizedFunctions
{
    /**
     * The library of basis function
     */
    private BasisFunctions basisFunctions;

    /**
     * The solution vectors
     */
    private SimpleMatrix[] solutions;

    /**
     * Construct using solution vectors
     * @param basisFunctions the basis functions
     * @param solutions the solution vectors
     */
    private OptimizedFunctions(BasisFunctions basisFunctions, SimpleMatrix... solutions)
    {
        this.basisFunctions = basisFunctions;
        this.solutions = solutions;
    }

    /**
     * Create a results object by actually solving the matrix system.
     * This system should have been constructed using the MatrixBuilder class.
     * @param basisFunctions The basis functions used to construct the system which are necessary
     *                       to interpret the solution vector.
     * @param system The system to solve.
     * @return The solution to the system.
     */
    public static OptimizedFunctions solveSystem(BasisFunctions basisFunctions, MatrixSystem system)
    {
        SimpleMatrix[] solutions = new SimpleMatrix[system.rhs.length];
        for (int i = 0; i < solutions.length; i++)
        {
            solutions[i] = system.solve(i);
        }

        return new OptimizedFunctions(basisFunctions, solutions);
    }

    /**
     * Create a results object by actually solving the matrix system.
     * In this version, the weights on the basis functions are constrained to be non-negative.
     * This system should have been constructed using the MatrixBuilder class.
     * @param basisFunctions The basis functions used to construct the system which are necessary
     *                       to interpret the solution vector.
     * @param system The system to solve.
     * @return The solution to the system.
     */
    public static OptimizedFunctions solveSystemNonNegative(
            BasisFunctions basisFunctions, MatrixSystem system, double toleranceScale)
    {
        SimpleMatrix[] solutions = new SimpleMatrix[system.rhs.length];
        for (int i = 0; i < solutions.length; i++)
        {
            solutions[i] = system.solveNonNegative(i, toleranceScale);
        }

        return new OptimizedFunctions(basisFunctions, solutions);
    }

    /**
     * Test if a particular solution instance has any non-zero elements.
     * @param instanceIndex the index of the instance to check.
     * @return true if the instance has any non-zero elements; false if all elements are zero.
     */
    public boolean isInstanceNonZero(int instanceIndex)
    {
        // vector length = instance count * (library size + 1)
        int instanceCount = solutions[0].getNumElements() / (basisFunctions.getFunctionCount() + 1);

        return IntStream.range(0, basisFunctions.getFunctionCount() + 1).anyMatch(
                i -> solutions[0].get(instanceIndex + instanceCount * i) > 0
                        || solutions[1].get(instanceIndex + instanceCount * i) > 0
                        || solutions[2].get(instanceIndex + instanceCount * i) > 0);
    }

    /**
     * Gets the coefficient of the truly constant term, by instance and channel.
     * This term is the constant term from the solution multiplied by (1-metallicity).
     * @param instanceIndex The index of the solution instance being evaluated.
     * @param channelIndex The index of the channel (i.e. red, green, blue for color).
     * @return The coefficient of the constant term.
     */
    public double getTrueConstantTerm(int instanceIndex, int channelIndex)
    {
        // First B elements (B = # of instances) of the solution vector are the constant terms.
        return solutions[channelIndex].get(instanceIndex) * (1 - basisFunctions.getMetallicity());
    }

    /**
     * Evaluate one of the optimized functions.
     * @param instanceIndex The index of the solution instance being evaluated.
     * @param channelIndex The index of the channel (i.e. red, green, blue for color).
     * @param functionConsumer An object that consumes the evaluated function.
     *                         Each element in the domain will be evaluated, but no particular order is guaranteed.
     *                         The integer passed to the consumer (the second parameter of the lambda) will be index of
     *                         the element being evaluated.  The constant term is not counted when determining this index.
     */
    public void evaluateNonConstantSolution(int instanceIndex, int channelIndex, ObjIntConsumer<Double> functionConsumer)
    {
        // vector length = instance count * (library size + 1)
        int instanceCount = solutions[0].getNumElements() / (basisFunctions.getFunctionCount() + 1);

        basisFunctions.evaluateSolution(solutions[channelIndex].get(instanceIndex),
                m -> solutions[channelIndex].get((m + 1) * instanceCount + instanceIndex), functionConsumer);
    }
}
