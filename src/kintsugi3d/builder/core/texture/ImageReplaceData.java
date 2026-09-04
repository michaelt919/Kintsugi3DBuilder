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

package kintsugi3d.builder.core.texture;

import kintsugi3d.builder.resources.project.specular.TextureResources;

import java.io.File;
import java.io.IOException;

public abstract class ImageReplaceData
{
    private final TextureResources<?> resources;
    private File currentImage;
    private File newImage;

    protected ImageReplaceData()
    {
        this.resources = null;
        this.currentImage = null;
        this.newImage = null;
    }

    protected ImageReplaceData(TextureResources<?> resources)
    {
        this.resources = resources;
        this.currentImage = null;
        this.newImage = null;
    }

    public abstract void replace() throws IOException;

    public abstract void refreshCard();

    public final TextureResources<?> getResources()
    {
        return resources;
    }

    public final File getCurrentImage()
    {
        return currentImage;
    }

    final void setCurrentImage(File currentTexture)
    {
        this.currentImage = currentTexture;

        if (this.newImage == null)
        {
            this.newImage = currentTexture;
        }
    }

    public final File getNewImage()
    {
        return newImage;
    }

    public final void setNewImage(File newImage)
    {
        this.newImage = newImage;
    }
}
