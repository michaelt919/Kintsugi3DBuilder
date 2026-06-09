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

import de.javagl.jgltf.model.io.IO;
import javafx.collections.ObservableList;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.fit.decomposition.BasisImageCreator;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.VisualizationShaders;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.builder.resources.project.specular.SpecularTextures;
import kintsugi3d.builder.state.scene.UserShader;
import kintsugi3d.gl.core.Texture;
import kintsugi3d.gl.core.Texture2D;
import kintsugi3d.gl.core.TextureFactory;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.gl.vecmath.IntVector2;
import kintsugi3d.util.ImageFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TextureCardFactory implements ProjectDataCardFactory{
    private final ProjectInstance<?> instance;

    private int cardNum;

    private boolean fail;

    //ShaderCardFactory is the constructor for this class takes a ProjectInstance and
    //assigns it to private variable in class
    public TextureCardFactory(ProjectInstance<?> instance)
    {
        this.instance = instance;
    }

    /*
    createCard will use ProjectDataCard to create cards for the shaders, needs both
    the title of the shader and the file name. Returns ProjectDataCard of the shader
    (single card). This is also where View Shader and Send to Carousel button and
    code are located.
    */
    public ProjectDataCard createCard(CardsModel cardsModel, Texture2D<?> texture, String title, TextureResources<?> test){
        String thumbnailPath;

        //1. Base Location where the .pngs and thumbnails folder are.
        File baseDirectory = instance.getActiveViewSet().getSupportingFilesDirectory();

        //2. File name for the .pngs
        String fileName = TextureResources.getTextureFilename(title, "PNG");

        //3. .png File
        File textureImage = new File(baseDirectory, fileName);

        //4. thumbnails folder
        File thumbnailDestination = new File(baseDirectory, "thumbnails");

        //5. Where and how to save the new .pngs
        File newTextureImage = new File(thumbnailDestination, fileName);

        try{
            ImageHelper.read(textureImage).saveAtResolution("PNG", newTextureImage,256,256);
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }
        try
        {
            thumbnailPath = ImageFinder.getInstance().findImageFile(newTextureImage).toString();
        }
        catch (FileNotFoundException e)
        {
            // Default to icon if thumbnail isn't found
            thumbnailPath = MainApplication.ICON_PATH;
        }
        return new ProjectDataCard(title, thumbnailPath, Map.of(), Map.of("View Texture", ()->{
            //Todo: Put code for view texture
            //UserShader textureShader = new UserShader(title, "rendermodes/maps/roughnessMap.frag");
            //Global.state().getUserShaderModel().setUserShader(textureShader);
        },"Send to Carousel", ()-> {
            //Todo: out code for send to carousel
        }));
    }
    /*
    createAllCards will call createCard for all the shaders and will
    return them in a list. This method will also detect if the model
    is processed and if so will limit the shaders shown to the user.
     */

    public List<ProjectDataCard> createAllCards(CardsModel cardsModel)
    {
        ArrayList<ProjectDataCard> textures = new ArrayList<>();
        if (instance.getResources() != null)
        {
            TextureResources<?> resources = instance.getResources().getTextureResources();
            var basisResources = resources.getTextures();
            if (basisResources != null)
            {
                for (var entry : basisResources.entrySet()) {
                    String key = entry.getKey();
                    Texture2D<?> texture = entry.getValue();
                    textures.add(createCard(cardsModel, texture, key, resources));
                }
            }
        }

        // If not yet initialized, return empty list.
        return textures;
    }

}
