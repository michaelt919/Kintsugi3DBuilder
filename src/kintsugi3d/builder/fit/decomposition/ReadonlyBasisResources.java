/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit.decomposition;

import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.ContextBound;
import kintsugi3d.gl.core.Program;

import java.io.File;

public interface ReadonlyBasisResources<ContextType extends Context<ContextType>> extends ContextBound<ContextType>
{
    MaterialBasis getBasis();

    /**
     * Gets the number of materials, some of which could be inactive.
     * @return
     */
    int getMaterialCount();

    /**
     * Gets the number of active materials, excluding inactive ones.
     * @return
     */
    int getActiveMaterialCount();

    int getBasisResolution();

    void save(File outputDirectory, String filenameOverride);

    /**
     * Saves basis function textures to the filesystem with a default filename.
     *
     * @param outputDirectory The directory in which to save the basis functions.
     */
    default void save(File outputDirectory)
    {
        save(outputDirectory, null);
    }

    void useWithShaderProgram(Program<ContextType> program);

    void refreshGraphicsResources();
}
