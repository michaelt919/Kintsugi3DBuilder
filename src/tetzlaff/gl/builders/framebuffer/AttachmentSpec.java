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

package tetzlaff.gl.builders.framebuffer;

public abstract class AttachmentSpec
{
    private int multisamples = 1;
    private boolean fixedMultisampleLocations = true;
    private boolean mipmapsEnabled = false;
    private boolean linearFilteringEnabled = false;

    public int getMultisamples()
    {
        return multisamples;
    }

    public boolean areMultisampleLocationsFixed()
    {
        return fixedMultisampleLocations;
    }

    public boolean areMipmapsEnabled()
    {
        return mipmapsEnabled;
    }

    public boolean isLinearFilteringEnabled()
    {
        return linearFilteringEnabled;
    }

    public AttachmentSpec setMultisamples(int samples, boolean fixedSampleLocations)
    {
        // TODO should this be a property of the framebuffer object builder?
        // Having different number of samples per attachment is not allowed.
        multisamples = samples;
        fixedMultisampleLocations = fixedSampleLocations;
        return this;
    }

    public AttachmentSpec setMipmapsEnabled(boolean enabled)
    {
        mipmapsEnabled = enabled;
        return this;
    }

    public AttachmentSpec setLinearFilteringEnabled(boolean enabled)
    {
        linearFilteringEnabled = enabled;
        return this;
    }
}
