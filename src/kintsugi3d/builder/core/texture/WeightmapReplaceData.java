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

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.resources.project.specular.TextureResources;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class WeightmapReplaceData extends ImageReplaceData
{
    private final int weightmapIndex;

    public WeightmapReplaceData(TextureResources<?> resources, int weightmapIndex, File currentImage)
    {
        super(resources);
        this.weightmapIndex = weightmapIndex;
        setCurrentImage(currentImage);
    }

    public int getWeightmapIndex()
    {
        return weightmapIndex;
    }

    @Override
    public void replace() throws IOException
    {
        getResources().getBasisWeightResources().replaceWeightMapWithSpecificFile(weightmapIndex, getNewImage());
    }

    @Override
    public void refreshCard()
    {
        // TODO switch to observable pattern for textures?
        WeightmapTextureInfo weightmapTextureInfo = new WeightmapTextureInfo(weightmapIndex);
        Global.state().getTabModels().getTab("Textures", TextureInfo.class).refreshCard(
            card -> Objects.equals(card.getInternalName(), weightmapTextureInfo.name),
            weightmapTextureInfo);
    }
}
