/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.export.specular.gltf;

import java.util.HashMap;
import java.util.Map;

public class GltfTextureExtras
{

    private Integer baseRes = null;

    private Map<Integer, Integer> lods = new HashMap<Integer, Integer>();

    public void setLodImageIndex(int resolution, int index)
    {
        lods.put(resolution, index);
    }

    public Integer getLodImageIndex(int resolution)
    {
        return lods.get(resolution);
    }

    public Map<Integer, Integer> getLods()
    {
        return lods;
    }

    public Integer getBaseRes()
    {
        return baseRes;
    }

    public void setBaseRes(Integer baseRes)
    {
        this.baseRes = baseRes;
    }
}
