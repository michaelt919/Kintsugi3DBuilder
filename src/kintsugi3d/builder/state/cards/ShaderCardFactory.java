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

import javafx.collections.ObservableList;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.builder.state.scene.UserShader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
ShaderCardFactory will create cards/boxes in the UI for the shaders that are applicable to
the current model of the project. When the model is not processed the shaders
available to the user will be limited, but when the model is processed all
shaders will be available for the user to use. On the shader cards there are two buttons
"Add to Carousel" and "View Shader," Add to carousel will send the shader to a carousel
for easier use for user. View Shader will apply the shader to the model.
 */
public class ShaderCardFactory implements ProjectDataCardFactory
{

    private final ProjectInstance<?> instance;

    /**
     * ShaderCardFactory is the constructor for this class takes a ProjectInstance and
     * assigns it to private variable in class
     * @param instance
     */
    public ShaderCardFactory(ProjectInstance<?> instance)
    {
        this.instance = instance;
    }

    /**
    createCard will use ProjectDataCard to create cards for the shaders, needs both
    the title of the shader and the file name. Returns ProjectDataCard of the shader
    (single card). This is also where View Shader and Send to Carousel button and
    code are located.
     @param title
     @param fileName
     @return
    */
    public static ProjectDataCard createCard(String title, String fileName)
    {
        //Creates shader with given title and filename
        UserShader shader = new UserShader(title, fileName);

        return new ShaderDataCard(shader, MainApplication.ICON_PATH, Map.of(), Map.of(
            "View Shader", () ->
            {
                //Sets the model to the shader
                Global.state().getUserShaderModel().setUserShader(shader);
            },
            "Send to Carousel", () ->
            {
                //Creates new shader with title and filename
                UserShader newCarouselShader = new UserShader(title, fileName);
                //Adds the shader to the carousel
                Global.state().getCarouselModel().addToCarousel(newCarouselShader);
            }));
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
        // shaderDataCards is an arraylist that will hold all shaders user can use
        List<ProjectDataCard> shaderDataCards = new ArrayList<>(9);
        shaderDataCards.add(createCard("Simple specular", "rendermodes/simpleSpecular.frag"));
        shaderDataCards.add(createCard("Normal mapped", "rendermodes/normalMapped.frag"));
        shaderDataCards.add(createCard("Textured Lambertian", "rendermodes/texturedLambertian.frag"));

        //if model is not processed these shaders are not shown
       if (Global.state().getProjectModel().isProjectProcessed())
       {
            shaderDataCards.add(createCard("Material (metallicity)", "rendermodes/texturedORMMaterial.frag"));
            shaderDataCards.add(createCard("Material (reflectivity)", "rendermodes/texturedMaterial.frag"));
            shaderDataCards.add(createCard("Material (basis)", "rendermodes/basisMaterial.frag"));
        }

        shaderDataCards.add(createCard("Image-based", "rendermodes/ibrUntextured.frag"));

        //if model is not processed these shaders are not shown
        if (Global.state().getProjectModel().isProjectProcessed())
        {
            shaderDataCards.add(createCard("Image-based with textures", "rendermodes/ibrTextured.frag"));
            shaderDataCards.add(createCard("Weight maps (combined)", "rendermodes/weightmaps/weightmapCombination.frag"));
        }
        return shaderDataCards;
    }
}
