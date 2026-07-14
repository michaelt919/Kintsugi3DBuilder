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

import javafx.stage.FileChooser.ExtensionFilter;
import kintsugi3d.builder.io.blender.BlenderExporterFactory;
import kintsugi3d.builder.io.gltf.MaterialExporterFactory;
import kintsugi3d.builder.io.gltf.kintsugi3dviewer.Kintsugi3DViewerExporterFactory;
import kintsugi3d.builder.io.usdz.USDZMetallicExporterFactory;
import kintsugi3d.builder.io.usdz.USDZSpecularExporterFactory;

public enum ExportType
{
    GLTF("glTF", Kintsugi3DViewerExporterFactory.getInstance(), new ExtensionFilter("glTF file", "*.glb")),
    USDZ_SPECULAR("USDZ (Specular)", USDZSpecularExporterFactory.getInstance(), new ExtensionFilter("USDZ file", "*.usdz")),
    USDZ_METALLIC("USDZ (Metallic)", USDZMetallicExporterFactory.getInstance(), new ExtensionFilter("USDZ file", "*.usdz")),
    BLENDER_EEVEE("Blender (EEVEE)", BlenderExporterFactory.getInstance(), new ExtensionFilter("glTF file", "*.glb")),
    BLENDER_CYCLES("Blender (Cycles)", BlenderExporterFactory.getInstance(), new ExtensionFilter("glTF file", "*.glb"));

    private final String friendlyName;
    private final MaterialExporterFactory factory;
    private final ExtensionFilter filter;

    ExportType(String friendlyName, MaterialExporterFactory factory, ExtensionFilter filter)
    {
        this.friendlyName = friendlyName;
        this.factory = factory;
        this.filter = filter;
    }

    public MaterialExporterFactory getFactory()
    {
        return factory;
    }

    public ExtensionFilter getFilter()
    {
        return filter;
    }
}
