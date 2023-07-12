/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.ibrelight.export.specularfit;

import tetzlaff.ibrelight.core.ReadonlyViewSet;
import tetzlaff.ibrelight.core.ViewSet;

public class ReconstructionSettings
{
    ReadonlyViewSet reconstructionViewSet = null;
    boolean reconstructAll = false;

    public ReconstructionSettings()
    {
    }

    /**
     * Should every image in the view set be reconstructed for validation, or just one key view?
     * Reconstructing all uses a lot more hard drive space.
     *
     * @return true if all views should be reconstructed; false if only one view should be reconstructed.
     */
    public boolean shouldReconstructAll()
    {
        return reconstructAll;
    }

    /**
     * Sets whether every image in the view set be reconstructed for validation, or just one key view?
     * Reconstructing all uses a lot more hard drive space.
     *
     * @param reconstructAll true if all views should be reconstructed; false if only one view should be reconstructed.
     */
    public void setReconstructAll(boolean reconstructAll)
    {
        this.reconstructAll = reconstructAll;
    }

    /**
     * Gets the view set used to create the reconstructed images for manually evaluating the effectiveness of the fit.
     *
     * @return
     */
    public ReadonlyViewSet getReconstructionViewSet()
    {
        return reconstructionViewSet;
    }

    /**
     * Sets the view set used to create the reconstructed images for manually evaluating the effectiveness of the fit.
     *
     * @return
     */
    public void setReconstructionViewSet(ReadonlyViewSet reconstructionViewSet)
    {
        this.reconstructionViewSet = reconstructionViewSet;
    }
}