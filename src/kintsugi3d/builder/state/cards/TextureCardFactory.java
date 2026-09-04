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

import javafx.application.Platform;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.RenderableInstance;
import kintsugi3d.builder.core.texture.TextureInfo;
import kintsugi3d.builder.core.texture.WeightmapTextureInfo;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.gl.vecmath.IntVector2;
import kintsugi3d.util.ImageFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TextureCardFactory implements ProjectDataCardFactory<TextureInfo>
{
    private static final Logger LOG = LoggerFactory.getLogger(TextureCardFactory.class);

    private final RenderableInstance<?> instance;

    private File textureImage;
    /**
     * TextureCardFactory is the constructor for this class takes a RenderableInstance and
     * assigns it to private variable in class
     * @param instance
     */
    public TextureCardFactory(RenderableInstance<?> instance)
    {
        this.instance = instance;
    }

    @Override
    public Class<TextureInfo> getDataClass()
    {
        return TextureInfo.class;
    }

    /**
     * finds the base directory for the textures and creates thumbnails from those images
     * in the thumbnails folder. returns a project data card with a list of details,
     * image, view texture button, and send to carousel button.
     * @param texture
     * @return projectDataCard
     */
    private ProjectDataCard createCard(TextureInfo texture)
    {
        // Base Location where the .pngs and thumbnails folder are.
        File baseDirectory = instance.getViewSet().getSupportingFilesDirectory();

        // thumbnails folder
        File thumbnailDestination = new File(baseDirectory, "thumbnails");

        String fileName = TextureResources.getTextureFilename(texture.name);

        // Where and how to save the new .pngs
        File newTextureImage = new File(thumbnailDestination, fileName);
        try
        {
            // .png File
            textureImage = new File(baseDirectory, fileName);

            // TODO convert weightmap to grayscale
            ImageHelper.read(textureImage).saveAtResolution("PNG", newTextureImage,256,256);
        }
        catch (IOException|RuntimeException e)
        {
            LOG.error("Error loading texture card: {}", texture.friendlyName, e);
        }

        String thumbnailPath;

        try
        {
            thumbnailPath = ImageFinder.getInstance().findImageFile(newTextureImage).toString();
        }
        catch (FileNotFoundException e)
        {
            // Default to icon if thumbnail isn't found
            thumbnailPath = MainApplication.ICON_PATH;
        }

        try
        {
            IntVector2 dimensions = ImageHelper.dimensionsOf(textureImage);
            String res = String.format("%dx%d", dimensions.x, dimensions.y);

            return new ShaderDataCard(texture.name, texture.getVisualizationShader(), thumbnailPath, new LinkedHashMap<>()
            {{
                put("File Name", textureImage.getName());
                put("Resolution", res);
                put("Size", (int) (((double) textureImage.length() / (1024.0 * 1024.0))*1000.0) + " KB");
                put("Purpose", texture.purpose);
            }}
            , List.of(
                Map.of(
                    "Refresh Texture", () -> refreshTexture(texture),
                    "Replace Texture...", () -> replaceTexture(texture)
                )));
        }
        catch (IOException|RuntimeException e)
        {
            LOG.error("Error creating card: {}", texture.friendlyName, e);
            return null;
        }
    }

    /**
     * createAllCards will call createCard for all the textures and will
     * return them in a list. If the model is not processed there will be
     * no textures at all shown to the user.
     * @param cardsModel
     * @return
     */
    @Override
    public List<ProjectDataCard> createAllCards(CardsModel<TextureInfo> cardsModel)
    {
        List<ProjectDataCard> textureCards = new ArrayList<>(8);
        if (instance.getResources() != null)
        {
            TextureResources<?> resources = instance.getResources().getTextureResources();

            var textures = resources.getTextures();
            if (textures != null)
            {
                for (var entry : textures.entrySet().stream().sorted(Comparator.comparing(Entry::getKey)).collect(Collectors.toList()))
                {
                    ProjectDataCard card = createCard(entry.getKey());
                    if (card != null)
                    {
                        textureCards.add(card);
                    }
                }
            }

            BasisResources<?> basisResources = resources.getBasisResources();
            if (basisResources != null)
            {
                for (int i = 0; i < basisResources.getBasisCount(); i++)
                {
                    ProjectDataCard card = createCard(new WeightmapTextureInfo(i));
                    if (card != null)
                    {
                        textureCards.add(card);
                    }
                }
            }
        }
        // If not yet initialized, return empty list.
        return textureCards;
    }

    @Override
    public Map<ProjectDataCard, ProjectDataCard> createRefreshedCards(CardsModel<TextureInfo> cardsModel, Function<ProjectDataCard, TextureInfo> refreshedData)
    {
        Map<ProjectDataCard, ProjectDataCard> changes = new HashMap<>(1);

        List<ProjectDataCard> cardsList = cardsModel.getCardList();
        Iterable<TextureInfo> details = new ArrayList<>(instance.getResources().getTextureResources().getTextures().keySet());

        for (ProjectDataCard card : cardsList)
        {
            TextureInfo key = refreshedData.apply(card);
            if (key != null) // Check whether the card is in the filter
            {
                changes.put(card, createCard(key));
            }
        }

        return changes;
    }

    private void refreshTexture(TextureInfo texture)
    {
        // Texture replacement must happen on graphics thread.
        Rendering.runLater(() ->
        {
            TextureResources<?> resources = instance.getResources().getTextureResources();

            try
            {
                texture.refresh(instance);

                // TODO switch to observable pattern for textures?
                Global.state().getTabModels().getTab("Textures", TextureInfo.class)
                    .refreshCard(card -> Objects.equals(card.getInternalName(), texture.name), texture);
            }
            catch (IOException | RuntimeException e)
            {
                ExceptionHandling.error("Error refreshing texture", e);
            }
        });
    }

    private void replaceTexture(TextureInfo texture)
    {
        Platform.runLater(() ->
            Global.state().getIOModel().getMainRenderable().invokeUserImageReplacement(texture.getReplaceData(instance)));
    }

}
