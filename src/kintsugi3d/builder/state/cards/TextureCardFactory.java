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
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.core.TextureDetails;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.builder.state.scene.UserShader;
import kintsugi3d.gl.core.Texture2D;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.util.ImageFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class TextureCardFactory implements ProjectDataCardFactory
{
    private static final Logger LOG = LoggerFactory.getLogger(TextureCardFactory.class);

    private final ProjectInstance<?> instance;

    //ShaderCardFactory is the constructor for this class takes a ProjectInstance and
    //assigns it to private variable in class
    public TextureCardFactory(ProjectInstance<?> instance)
    {
        this.instance = instance;
    }

    /**
    createCard will use ProjectDataCard to create cards for the shaders, needs both
    the title of the shader and the file name. Returns ProjectDataCard of the shader
    (single card). This is also where View Shader and Send to Carousel button and
    code are located.
     @param texture
     @param details
     @return
    */
    private ProjectDataCard createSimpleTextureCard(Texture2D<?> texture, TextureDetails details)
    {
        //2. File name for the .pngs
        String fileName = TextureResources.getTextureFilename(details.name, "PNG");

        UserShader textureShader = new UserShader(details.friendlyName, "rendermodes/viewTextureSimple.frag",
            Map.of("VIEW_TEX", Optional.of(String.format("tex_%s", details.name))));

        return createProjectDataCard(fileName, textureShader);
    }

    private ProjectDataCard createWeightmapCard(TextureResources<?> resources, int weightmapIndex)
    {
        // File name for the .pngs
        String fileName = TextureResources.getUnpackedWeightMapFilename(weightmapIndex, "PNG");

        String friendlyName = String.format("Weight map %d", weightmapIndex);

        UserShader textureShader = new UserShader(friendlyName, "rendermodes/viewTextureWeights.frag",
            Map.of("WEIGHTMAP_INDEX", Optional.of(weightmapIndex)));

        return createProjectDataCard(fileName, textureShader);
    }

    private ProjectDataCard createProjectDataCard(String fileName, UserShader shader)
    {
        // Base Location where the .pngs and thumbnails folder are.
        File baseDirectory = instance.getActiveViewSet().getSupportingFilesDirectory();

        // thumbnails folder
        File thumbnailDestination = new File(baseDirectory, "thumbnails");

        // Where and how to save the new .pngs
        File newTextureImage = new File(thumbnailDestination, fileName);

        try
        {
            // .png File
            File textureImage = new File(baseDirectory, fileName);

            // TODO convert weightmap to grayscale
            ImageHelper.read(textureImage).saveAtResolution("PNG", newTextureImage,256,256);
        }
        catch (IOException|RuntimeException e)
        {
            LOG.error("Error loading texture card: {}", shader.getFriendlyName(), e);
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

        return new ShaderDataCard(shader, thumbnailPath, Map.of(), List.of(
            Map.of(
            "View Texture", () -> Global.state().getUserShaderModel().setUserShader(shader),
            "Send to Carousel", () -> Global.state().getCarouselModel().addToCarousel(shader)
            ),
            Map.of(
                "Refresh Texture", () -> {},
                "Replace Texture", () -> {}
            )
        ));
    }

    /**
    createAllCards will call createCard for all the shaders and will
    return them in a list. This method will also detect if the model
    is processed and if so will limit the shaders shown to the user.
     @param cardsModel
     @return
     */
    @Override
    public List<ProjectDataCard> createAllCards(CardsModel cardsModel)
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
                    TextureDetails key = entry.getKey();
                    Texture2D<?> texture = entry.getValue();
                    textureCards.add(createSimpleTextureCard(texture, key));
                }
            }

            BasisResources<?> basisResources = resources.getBasisResources();
            if (basisResources != null)
            {
                for (int i = 0; i < basisResources.getBasisCount(); i++)
                {
                    textureCards.add(createWeightmapCard(resources, i));
                }
            }
        }

        // If not yet initialized, return empty list.
        return textureCards;
    }

}
