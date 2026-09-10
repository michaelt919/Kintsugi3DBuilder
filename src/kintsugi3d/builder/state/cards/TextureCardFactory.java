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
import kintsugi3d.builder.resources.project.GraphicsResources;
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
import java.util.stream.Collectors;

public class TextureCardFactory extends ProjectDataCardFactoryBase<TextureInfo>
{
    private static final Logger LOG = LoggerFactory.getLogger(TextureCardFactory.class);

    /**
     * TextureCardFactory is the constructor for this class takes a RenderableInstance and
     * assigns it to private variable in class
     * @param instance
     */
    public TextureCardFactory(RenderableInstance<?> instance)
    {
        super(instance);
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
    @Override
    public ProjectDataCard createCard(TextureInfo texture)
    {
        // Base Location where the .pngs and thumbnails folder are.
        File baseDirectory = getViewSet().getSupportingFilesDirectory();

        // thumbnails folder

        String fileName = TextureResources.getTextureFilename(texture.name);

        // Where and how to save the new .pngs
        try
        {
            // .png File
            File textureImage = new File(baseDirectory, fileName);

            if (textureImage.exists())
            {
                // Save thumbnail
                // TODO convert weightmap to grayscale
                File thumbnailDestination = new File(baseDirectory, "thumbnails");
                File newTextureImage = new File(thumbnailDestination, fileName);
                ImageHelper.read(textureImage).saveAtResolution("PNG", newTextureImage, 256, 256);

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

                IntVector2 dimensions = ImageHelper.dimensionsOf(textureImage);
                String res = String.format("%dx%d", dimensions.x, dimensions.y);

                return new ShaderDataCard(texture.name, texture.getVisualizationShader(), thumbnailPath,
                    new LinkedHashMap<>()
                    {{
                        put("File Name", textureImage.getName());
                        put("Resolution", res);
                        put("Size", (int) (((double) textureImage.length() / (1024.0 * 1024.0)) * 1000.0) + " KB");
                        put("Purpose", texture.purpose);
                    }},
                    List.of(Map.of(
                        "Refresh Texture", () -> refreshTexture(texture),
                        "Replace Texture...", () -> replaceTexture(texture)
                    )));
            }
            else
            {
                LOG.info("Texture not found: {}", texture);
                return null;
            }
        }
        catch (IOException|RuntimeException e)
        {
            LOG.error("Error loading texture card: {}", texture.friendlyName, e);
            return null;
        }
    }

    /**
     * createAllCards will call createCard for all the textures and will
     * return them in a list. If the model is not processed there will be
     * no textures at all shown to the user.
     * @return
     */
    @Override
    public List<ProjectDataCard> createAllCards()
    {
        List<ProjectDataCard> textureCards = new ArrayList<>(8);
        GraphicsResources<?> resources = getInstance().getResources();
        if (resources != null)
        {
            TextureResources<?> texResources = resources.getTextureResources();

            var textures = texResources.getTextures();
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

            BasisResources<?> basisResources = texResources.getBasisResources();
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

    private void refreshTexture(TextureInfo texture)
    {
        // Texture replacement must happen on graphics thread.
        Rendering.runLater(() ->
        {
            try
            {
                texture.refresh(getInstance());

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
            Global.state().getIOModel().getMainRenderable().invokeUserImageReplacement(texture.getReplaceData(getInstance())));
    }

}
