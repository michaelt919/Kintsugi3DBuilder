/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.optimization.function;

import java.util.function.DoubleUnaryOperator;

/**
 * A basis function library that uses a smoothstep rather than a hard step.
 */
public class GeneralizedSmoothStepBasis extends AbstractBasisFunctions
{
    private final int resolution;
    private final int minSmoothstepWidth;
    private final int maxSmoothstepWidth;
    private final DoubleUnaryOperator smoothstep;

    /**
     * Constructs a "library" of smoothstep basis functions.
     * The actual smoothstep function can be customized.
     * The library will consist of a set of smoothsteps that evaluate to:
     *   1.0 when the parameter is 0,
     *   0.0 when the parameter is at the end of the optimized domain.
     *   some value between 0.0 and 1.0 when the parameter is within the optimzed domain.
     * Each smoothstep function in the library will start and end at a different location.
     * The endpoints will be spaced out evenly across the domain.
     * The starting point will generally be a fixed distance from the endpoint, but may be compressed when the
     * endpoint is close to 0.
     * @param resolution The number of discrete elements in the domain of the function to be optimized,
     *                   which determines the number of functions in the library.
     *                   An exception will be thrown if this number is less than 1.
     * @param metallicity The assumed "metallicity" of the function being optimized.
     *                    This value will be clamped between 0 and 1.
     * @param minSmoothstepWidth The minimum distance between the start and end of each smoothstep function.
     *                           This is used to determine the width of functions ending closest to the starting point.
     *                           Functions that would be narrower will be omitted from the basis; thus, the size of the
     *                           optimization domain (the number of functions in the library) is resolution - minSmoothstepWidth + 1.
     *                           A min width of 1 will include all possible functions in the optimization domain.
     *                           The min width will be clamped to 1 if set lower than 1.
     * @param maxSmoothstepWidth The typical distance between the start and end of each smoothstep function in the library.
     *                        Each smoothstep function will end (with a value of 0.0) at a different location
     *                        (spaced evenly across the domain) and will start (with a value of 1.0) at
     *                        endpoint - maxSmoothstepWidth.  If that difference comes out to be negative, the starting
     *                        point will be clamped to zero.  The effect of this is that some basis functions with a
     *                        smaller width be included in the library, up to minSmoothstepWidth.
     *                        This is helpful when optimizing a BRDF, for instance, which may have a steeper gradient
     *                        close to the specular peak.
     *                        A max width of 1 would correspond to a hard step function.
     *                        The max width will be clamped to minSmoothstepWidth if set lower than minSmoothstepWidth.
     * @param smoothstep The actual smoothstep function to be used.  This function should have accept a domain between
     *                   0.0 and 1.0 and map the values to a range that is also between 0.0 and 1.0, where an input of
     *                   0.0 evaluates to a result of 0.0, and an input of 1.0 evaluates to a result of 1.0.
     *                   It is intended for this function to be monotonically increasing.
     *                   Note that this function will effectively be flipped in practice since the basis functions are
     *                   assumed to evaluate to 1.0 for an input parameter of 0 and decresse down to 1.0 as that
     *                   parameter increases.
     */
    public GeneralizedSmoothStepBasis(int resolution, double metallicity, int minSmoothstepWidth,
                                      int maxSmoothstepWidth, DoubleUnaryOperator smoothstep)
    {
        super(metallicity);

        if (resolution < 1)
        {
            throw new IllegalArgumentException("Resolution must be greater than zero.");
        }

        this.resolution = resolution;
        this.minSmoothstepWidth = Math.max(1, minSmoothstepWidth);
        this.maxSmoothstepWidth = Math.max(minSmoothstepWidth, maxSmoothstepWidth);
        this.smoothstep = smoothstep;
    }

    @Override
    protected int getFirstFunctionIndexForDomainValue(int value)
    {
        return Math.max(0, value - minSmoothstepWidth);
    }

    @Override
    protected int getLastFunctionIndexForDomainValue(int value)
    {
        // For a particular value, value-minSmoothstepWidth is the last basis function that evaluates to 0.0 @ value,
        // so value - minSmoothstepWidth + maxSmoothstepWidth is the first basis function that evaluates to 1.0 @ value.
        return Math.min(value - minSmoothstepWidth + maxSmoothstepWidth, getFunctionCount());
    }

    @Override
    public double evaluate(int functionIndex, int value)
    {
        // The function at index i always reaches 0.0 when value = i + minSmoothstepWidth.
        // If i + minSmoothstepWidth < maxSmoothstepWidth, then the smoothstep starts right away at m=0.
        // Otherwise, the smoothstep starts at m = i + minSmoothstepWidth - maxSmoothstepWidth.
        if (value < functionIndex + minSmoothstepWidth) // <=> functionIndex + minSmoothstepWidth - value > 0
        {
            // Assuming minSmoothstepWidth is 1:
            // functionIndex = 0: range of 1; [0, 1)
            // functionIndex = 1: range of 2; [0, 2)
            // etc.
            // functionIndex = maxSmoothstepWidth: use maxSmoothstepWidth; [0, maxSmoothstepWidth)
            // functionIndex = maxSmoothstepWidth + 1: [1, maxSmoothstepWidth + 1)
            // etc.
            int effectiveWidth = Math.min(maxSmoothstepWidth, functionIndex + minSmoothstepWidth);

            int domainIndex = minSmoothstepWidth + functionIndex - value;
            if (domainIndex < effectiveWidth)
            // <=> value > functionIndex + minSmoothstepWidth - effectiveWidth [left bound of the range]
            {
                // Value is within range, use smoothstep function.
                return smoothstep.applyAsDouble((double) domainIndex / (double) effectiveWidth);
            }
            else
            {
                // Value is beyond range to the left
                return 1.0;
            }
        }
        else
        {
            // Value is beyond range to the right
            return 0.0;
        }
    }

    @Override
    public int getFunctionCount()
    {
        return resolution - minSmoothstepWidth + 1;
    }

    @Override
    public int getOptimizedDomainSize()
    {
        return resolution;
    }
}
