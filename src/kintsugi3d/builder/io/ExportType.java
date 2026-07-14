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

package kintsugi3d.builder.io;

import kintsugi3d.builder.io.gltf.MaterialExporterFactory;
import kintsugi3d.builder.io.gltf.kintsugi3dviewer.Kintsugi3DViewerExporterFactory;
import kintsugi3d.builder.io.usdz.USDZMetallicExporterFactory;
import kintsugi3d.builder.io.usdz.USDZSpecularExporterFactory;

public enum ExportType
{
    GLTF(Kintsugi3DViewerExporterFactory.getInstance()),
    USDZ_SPECULAR(USDZSpecularExporterFactory.getInstance()),
    USDZ_METALLIC(USDZMetallicExporterFactory.getInstance());
    GLTF("glTF", Kintsugi3DViewerExporterFactory.getInstance()),
    USDZ("USDZ", USDZExporterFactory.getInstance()),
    BLENDER_EEVEE("Blender (EEVEE)", BlenderExporterFactory.getInstance()),
    BLENDER_CYCLES("Blender (Cycles)", BlenderExporterFactory.getInstance());

    private final String friendlyName;
    private final MaterialExporterFactory factory;

    ExportType(String friendlyName, MaterialExporterFactory factory)
    {
        this.friendlyName = friendlyName;
        this.factory = factory;
    }

    public MaterialExporterFactory getFactory()
    {
        return factory;
    }
}
