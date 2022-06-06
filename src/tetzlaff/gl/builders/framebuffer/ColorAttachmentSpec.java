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

package tetzlaff.gl.builders.framebuffer;

import tetzlaff.gl.core.ColorFormat;

public final class ColorAttachmentSpec extends AttachmentSpec
{
    public final ColorFormat internalFormat;

    private ColorAttachmentSpec(ColorFormat internalFormat)
    {
        this.internalFormat = internalFormat;
    }

    public static ColorAttachmentSpec createWithInternalFormat(ColorFormat internalFormat)
    {
        return new ColorAttachmentSpec(internalFormat);
    }

    @Override
    public ColorAttachmentSpec setMultisamples(int samples, boolean fixedSampleLocations)
    {
        super.setMultisamples(samples, fixedSampleLocations);
        return this;
    }

    @Override
    public ColorAttachmentSpec setMipmapsEnabled(boolean enabled)
    {
        super.setMipmapsEnabled(enabled);
        return this;
    }

    @Override
    public ColorAttachmentSpec setLinearFilteringEnabled(boolean enabled)
    {
        super.setLinearFilteringEnabled(enabled);
        return this;
    }
}
