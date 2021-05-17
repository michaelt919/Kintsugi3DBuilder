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

package tetzlaff.ibrelight.core;

import java.io.File;
import java.util.Collections;
import java.util.List;

import tetzlaff.gl.core.ColorFormat;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Program;
import tetzlaff.ibrelight.rendering.IBRResources;

public interface TextureFitMethod
{
    default List<ColorFormat> getExtractionFBOAttachments()
    {
        return Collections.singletonList(ColorFormat.RGBA8);
    }

    <ContextType extends Context<ContextType>>
    Program<ContextType> createExtractionProgram();

    <ContextType extends Context<ContextType>>
    Program<ContextType> createErrorPRogram();

    <ContextType extends Context<ContextType>>
    Program<ContextType> createReconstructionProgram();

    <ContextType extends Context<ContextType>>
    TextureFitter initialize(IBRResources<ContextType> resources);
}
