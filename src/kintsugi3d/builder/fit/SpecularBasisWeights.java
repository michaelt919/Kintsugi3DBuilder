/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.fit;

public interface SpecularBasisWeights
{
    /**
     * Gets the weight for a basis function at a particular sample p.
     * @param b The basis function for which to retrieve a weight.
     * @param p The sample index for which to retrieve a weight.
     * @return The weight for basis function b at sample p.
     */
    double getWeight(int b, int p);

    /**
     * Gets whether a particular sample p has valid weights.
     * @param p The sample index for which to check if weights are valid.
     * @return true if all weights are valid; false if no weights are valid at sample p.
     */
    boolean areWeightsValid(int p);
}
