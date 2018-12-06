/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.material;

import umn.gl.vecmath.Vector3;

public class MaterialTextureMap 
{
    private String mapName;

    private boolean clampingRequired;
    private float base;
    private float gain;
    private Vector3 offset;
    private Vector3 scale;

    public MaterialTextureMap()
    {
        clampingRequired = false;
        base = 0.0f;
        gain = 1.0f;
        offset = new Vector3(0.0f);
        scale = new Vector3(1.0f);
    }

    public String getMapName()
    {
        return mapName;
    }

    public void setMapName(String mapName)
    {
        this.mapName = mapName;
    }

    public boolean isClampingRequired()
    {
        return clampingRequired;
    }

    public void setClampingRequired(boolean clampingEnabled)
    {
        this.clampingRequired = clampingEnabled;
    }

    public float getBase()
    {
        return base;
    }

    public void setBase(float base)
    {
        this.base = base;
    }

    public float getGain()
    {
        return gain;
    }

    public void setGain(float gain)
    {
        this.gain = gain;
    }

    public Vector3 getOffset()
    {
        return offset;
    }

    public void setOffset(Vector3 offset)
    {
        this.offset = offset;
    }

    public Vector3 getScale()
    {
        return scale;
    }

    public void setScale(Vector3 scale)
    {
        this.scale = scale;
    }
}
