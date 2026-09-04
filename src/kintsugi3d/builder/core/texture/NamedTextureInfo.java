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
import kintsugi3d.builder.core.RenderableInstance;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.builder.state.scene.UserShader;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class NamedTextureInfo extends TextureInfo
{
    public NamedTextureInfo(String name, String friendlyName, String purpose)
    {
        super(name, friendlyName, purpose);
    }

    public NamedTextureInfo(String name)
    {
        super(name);
    }

    @Override
    public UserShader getVisualizationShader()
    {
        return new UserShader(friendlyName, "rendermodes/viewTextureSimple.frag",
            Map.of("VIEW_TEX", Optional.of(String.format("tex_%s", name))));
    }

    @Override
    public void refresh(RenderableInstance<?> instance) throws IOException
    {
        // TODO switch to observable pattern for textures?
        TextureResources<?> resources = instance.getResources().getTextureResources();
        resources.replaceTextureWithDefaultFile(this, instance.getViewSet().getSupportingFilesDirectory());
    }

    @Override
    public ImageReplaceData getReplaceData(RenderableInstance<?> instance)
    {
        return new NamedTextureReplaceData(instance.getResources().getTextureResources(), this,
            new File(Global.state().getIOModel().validateRenderable().getLoadedViewSet().getSupportingFilesDirectory(),
                TextureResources.getTextureFilename(name)));
    }
}
