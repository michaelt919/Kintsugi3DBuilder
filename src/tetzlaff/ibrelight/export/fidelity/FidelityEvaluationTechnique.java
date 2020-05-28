/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.fidelity;

import java.io.File;
import java.io.IOException;
import java.util.List;

import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

public interface FidelityEvaluationTechnique<ContextType extends Context<ContextType>> extends AutoCloseable
{
    boolean isGuaranteedMonotonic();
    boolean isGuaranteedInterpolating();
    void initialize(IBRResources<ContextType> resources, ReadonlySettingsModel settings, int size) throws IOException;
    void setMask(File maskFile) throws IOException;
    void updateActiveViewIndexList(List<Integer> activeViewIndexList);
    double evaluateBaselineError(int targetViewIndex, File debugFile);
    double evaluateError(int targetViewIndex, File debugFile);

    default double evaluateBaselineError(int targetViewIndex)
    {
        return evaluateBaselineError(targetViewIndex, null);
    }

    default double evaluateError(int targetViewIndex)
    {
        return evaluateError(targetViewIndex, null);
    }
}
