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
import kintsugi3d.builder.fit.decomposition.BasisMaterialInfo;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.builder.rendering.ImageBasedRenderable;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.builder.state.scene.ShaderInfo;
import kintsugi3d.gl.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class WeightmapTextureInfo extends TextureInfo
{
    private static final Logger LOG = LoggerFactory.getLogger(WeightmapTextureInfo.class);

    private final BasisMaterialInfo material;

    public WeightmapTextureInfo(BasisMaterialInfo material)
    {
        super(BasisWeightResources.getUnpackedWeightMapName(material.getName()),
            material.getFriendlyName(),
            String.format("A grayscale map that determines where %s is used.  Black (0) means that material is not used at all, white (1) means it is used exclusively.",
                material.getFriendlyName()));
        this.material = material;
    }

    @Override
    public ShaderInfo getVisualizationShader()
    {
        return new ShaderInfo(friendlyName, "rendermodes/viewTextureWeights.frag",
            Map.of("WEIGHTMAP_INDEX", Optional.of(material.getGPUIndex())));
    }

    @Override
    public void refresh(ImageBasedRenderable<?> instance) throws IOException
    {
        TextureResources<? extends Context<?>> resources = instance.getResources().getTextureResources();
        resources.getBasisWeightResources().replaceWeightMapWithDefaultFile(
            material.getName(), instance.getViewSet().getSupportingFilesDirectory());
    }

    @Override
    public ImageReplacer getReplaceData(ImageBasedRenderable<?> instance)
    {
        File supportingFilesDirectory = Global.io().validateRenderable().getLoadedViewSet().getSupportingFilesDirectory();
        try
        {
            return new WeightmapReplacer(instance.getResources().getTextureResources(), material,
                BasisWeightResources.findWeightmap(
                    supportingFilesDirectory, material.getName()));
        }
        catch (FileNotFoundException e)
        {
            LOG.error("Could not find weightmap to be replaced: {}", material.getName(), e);

            // Return an object with the expected path anyways and attempt to recover.
            return new WeightmapReplacer(instance.getResources().getTextureResources(), material,
                new File(supportingFilesDirectory, BasisWeightResources.getUnpackedWeightMapFilename(material.getName())));
        }
    }
}
