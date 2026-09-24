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

package kintsugi3d.builder.state.cards;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.fit.decomposition.BasisImageCreator;
import kintsugi3d.builder.fit.decomposition.BasisMaterialInfo;
import kintsugi3d.builder.fit.decomposition.ReadonlyBasisResources;
import kintsugi3d.builder.fit.decomposition.VisualizationShaders;
import kintsugi3d.builder.rendering.ImageBasedRenderable;
import kintsugi3d.builder.rendering.Rendering;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.builder.state.scene.ShaderInfo;
import kintsugi3d.builder.util.AppIcon;
import kintsugi3d.gl.core.Context;
import kintsugi3d.util.ImageFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public class MaterialCardFactory extends ProjectDataCardFactoryBase<BasisMaterialInfo>
{
    public MaterialCardFactory(ImageBasedRenderable<?> instance)
    {
        super(instance);
    }

    @Override
    public Class<BasisMaterialInfo> getDataClass()
    {
        return BasisMaterialInfo.class;
    }

    @Override
    public ProjectDataCard createCard(BasisMaterialInfo material)
    {
        String name = material.getName();

        String thumbnailPath;
        try
        {
            thumbnailPath = ImageFinder.getInstance().findImageFile(
                new File(getViewSet().getThumbnailImageDirectory(),
                    BasisImageCreator.getBasisImageFilename(name))).toString();
        }
        catch (FileNotFoundException e)
        {
            // Default to icon if thumbnail isn't found
            thumbnailPath = AppIcon.PATH;
        }

        ShaderInfo shader = VisualizationShaders.getForBasisMaterial(VisualizationShaders.BASIS_MATERIAL_WEIGHTED,
            material.getGPUIndex(), VisualizationShaders.FORMAT_PALETTE_MATERIAL);

        TextureResources<?> resources = getInstance().getResources().getTextureResources();

        return new ShaderDataCard(name, String.format("Material %s", name), shader, thumbnailPath, Map.of(),
            List.of(
                Map.of(
                    "Highlight Material", () ->
                    {
                        ShaderInfo prevShader = Global.state().getUserShaderModel().getActiveShader();
                        var defines = new HashMap<>(prevShader.getDefines());
                        var overlayMode = defines.get("OVERLAY_MODE");
                        var overlayWeightmapIndex = defines.get("OVERLAY_WEIGHTMAP_INDEX");

                        String subName;

                        if (overlayMode != null && overlayMode.isPresent() && overlayMode.get().equals("OVERLAY_MODE_WEIGHTMAP")
                            && overlayWeightmapIndex != null && overlayWeightmapIndex.isPresent()
                            && overlayWeightmapIndex.get().equals(material.getGPUIndex()))
                        {
                            // Overlay already active; toggle off
                            defines.remove("OVERLAY_MODE");
                            defines.remove("OVERLAY_WEIGHTMAP_INDEX");
                            subName = null;
                        }
                        else
                        {
                            defines.put("OVERLAY_MODE", Optional.of("OVERLAY_MODE_WEIGHTMAP"));
                            defines.put("OVERLAY_WEIGHTMAP_INDEX", Optional.of(material.getGPUIndex()));
                            subName = material.getFriendlyName();
                        }

                        Global.state().getUserShaderModel().setActiveShader(
                            new ShaderInfo(prevShader.getFriendlyName(), prevShader.getFilename(), defines, subName));
                    }),
                Map.of("Toggle Disabled", () ->
                    Rendering.runLater(() ->
                    {
                        resources.toggleBasisMaterial(material.getName());

                        // TODO setup listeners for materials
                        Global.state().getTabModels().getTab(TabsManager.MATERIALS, BasisMaterialInfo.class)
                            .refreshCards(card -> card.getInternalName().equals(name) ? material : null);
                    }),
                "Delete Material", () ->
                    Global.state().getProjectModel().confirm("Delete Material", "Delete Material?", "This will delete the material from the project.",
                        () -> Rendering.runLater(() -> // needs to run on graphics thread to replace GPU resources
                        {
                            try
                            {
                                resources.deleteBasisMaterial(material.getName());
                            }
                            finally // even if an exception is thrown, want to make sure we're in sync with the current state.
                            {
                                // TODO setup listeners for materials
                                Global.state().getTabModels().getTab(TabsManager.MATERIALS, BasisMaterialInfo.class)
                                    .deleteCards(card -> card.getInternalName().equals(name));
                            }
                        })))),
            !material.isEnabled());
    }

    @Override
    public List<ProjectDataCard> createAllCards()
    {
        ReadonlyBasisResources<? extends Context<?>> basisResources =
            getInstance().getResources().getTextureResources().getBasisResources();
        if (basisResources != null)
        {
            return basisResources.getBasis().getMaterials().stream()
                .map(this::createCard)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProjectDataCard::getInternalName))
                .collect(Collectors.toUnmodifiableList());
        }
        else
        {
            // If not yet initialized, return empty list.
            return List.of();
        }
    }
}
