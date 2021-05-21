/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.core;

/**
 * Represents a function to be used for alpha blending.
 * The function must be a linear combination of a source color and a destination color, weighted using one of several predefined weighting function.
 * @author Michael Tetzlaff
 *
 */
public class AlphaBlendingFunction 
{
    /**
     * An enumeration of the possible ways that either a source or a destination color can contribute to the final color.
     * @author Michael Tetzlaff
     *
     */
    public enum Weight
    {
        /**
         * No contribution; i.e. multiplication by zero
         */
        ZERO,

        /**
         * Full contribution; i.e. multiplication by one
         */
        ONE,

        /**
         * Each color component is weighted independently by the strength of the corresponding component in the source color.
         * A color component with a source value of 0 contributes 0%; a color component with a source value of 1 contributes 100%.
         */
        SRC_COLOR,

        /**
         * Each color component is weighted independently by the negation of the corresponding component in the source color.
         * A color component with a source value of 1 contributes 100%; a color component with a source value of 0 contributes 0%.
         */
        ONE_MINUS_SRC_COLOR,

        /**
         * Each color component is weighted independently by the strength of the corresponding component in the destination color.
         * A color component with a destination value of 0 contributes 0%; a color component with a destination value of 1 contributes 100%.
         */
        DST_COLOR,

        /**
         * Each color component is weighted independently by the negation of the corresponding component in the destination color.
         * A color component with a destination value of 1 contributes 100%; a color component with a destination value of 0 contributes 0%.
         */
        ONE_MINUS_DST_COLOR,

        /**
         * The color will be weighted by the alpha value from the source color.
         * An alpha value of 0 results in 0% contribution; an alpha value of 1 results in 100% contribution.
         */
        SRC_ALPHA,

        /**
         * The color will be weighted by the negation of the alpha value from the source color.
         * An alpha value of 0 results in 100% contribution; an alpha value of 1 results in 0% contribution.
         */
        ONE_MINUS_SRC_ALPHA,

        /**
         * The color will be weighted by the alpha value from the destination color.
         * An alpha value of 0 results in 0% contribution; an alpha value of 1 results in 100% contribution.
         */
        DST_ALPHA,

        /**
         * The color will be weighted by the negation of the alpha value from the destination color.
         * An alpha value of 0 results in 100% contribution; an alpha value of 1 results in 0% contribution.
         */
        ONE_MINUS_DST_ALPHA
    }

    /**
     * The weighting function to be used for the source color.
     */
    public final Weight sourceWeightFunction;

    /**
     * The weighting function to be used for the destination color.
     */
    public final Weight destinationWeightFunction;

    /**
     * Creates a new alpha blending function.
     * @param source The weighting function to be used for the source color.
     * @param destination The weighting function to be used for the destination color.
     */
    public AlphaBlendingFunction(Weight source, Weight destination)
    {
        this.sourceWeightFunction = source;
        this.destinationWeightFunction = destination;
    }

}
