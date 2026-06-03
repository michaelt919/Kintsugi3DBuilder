/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state.cards;

import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.fit.decomposition.BasisImageCreator;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.VisualizationShaders;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.util.ImageFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MaterialCardFactory implements ProjectDataCardFactory
{
    private final ProjectInstance<?> instance;

    public MaterialCardFactory(ProjectInstance<?> instance)
    {
        this.instance = instance;
    }

    public ProjectDataCard createCard(CardsModel cardsModel, TextureResources<?> resources, int cardIndex)
    {
        String thumbnailPath;
        try
        {
            thumbnailPath = ImageFinder.getInstance().findImageFile(
                new File(instance.getActiveViewSet().getThumbnailImageDirectory(),
                    BasisImageCreator.getBasisImageFilename(cardIndex))).toString();
        }
        catch (FileNotFoundException e)
        {
            // Default to icon if thumbnail isn't found
            thumbnailPath = MainApplication.ICON_PATH;
        }

        return new ProjectDataCard("Material " + cardIndex, thumbnailPath, Map.of(),
            List.of(
                new LinkedHashMap<>()
                {{
                    put("View Material ", () ->
                        Global.state().getUserShaderModel().setUserShader(
                            VisualizationShaders.getForBasisMaterial(VisualizationShaders.BASIS_MATERIAL, cardIndex)));
                    put("View Weight Map (Grayscale)", () ->
                        Global.state().getUserShaderModel().setUserShader(
                            VisualizationShaders.getForBasisMaterial(VisualizationShaders.WEIGHT_MAP_GRAYSCALE, cardIndex)));
                    put("View Weight Map (Superimposed)", () ->
                        Global.state().getUserShaderModel().setUserShader(
                            VisualizationShaders.getForBasisMaterial(VisualizationShaders.WEIGHT_MAP_SUPERIMPOSED, cardIndex)));
                    put("View Material X Weight Map", () ->
                        Global.state().getUserShaderModel().setUserShader(
                            VisualizationShaders.getForBasisMaterial(VisualizationShaders.BASIS_MATERIAL_WEIGHTED, cardIndex)));
                }},
                Map.of("Delete Material", () ->
                    cardsModel.confirm("Delete Material", "Delete Material?", "This will delete the material from the project.",
                        () -> Rendering.runLater(() -> // needs to run on graphics thread to replace GPU resources
                        {
                            try
                            {
                                resources.deleteBasisMaterial(cardIndex);
                            }
                            finally // even if an exception is thrown, want to make sure we're in sync with the current state.
                            {
                                // hard reset of cards list to re-number, etc.
                                cardsModel.setCardList(createAllCards(cardsModel));
                            }
                        })))));
    }

    @Override
    public List<ProjectDataCard> createAllCards(CardsModel cardsModel)
    {
        if (instance.getResources() != null)
        {
            TextureResources<?> resources = instance.getResources().getTextureResources();
            BasisResources<?> basisResources = resources.getBasisResources();
            if (basisResources != null)
            {
                return IntStream.range(0, basisResources.getBasisCount())
                    .mapToObj(i -> createCard(cardsModel, resources, i))
                    .collect(Collectors.toUnmodifiableList());
            }
        }

        // If not yet initialized, return empty list.
        return List.of();
    }
}
