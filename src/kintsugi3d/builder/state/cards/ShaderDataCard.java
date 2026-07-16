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
import kintsugi3d.builder.state.scene.UserShader;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a workspace card which will automatically have two buttons: "Add to Carousel" and "Send to Main View."
 * "Add to Carousel" will send the shader to a carousel for easier use for user.
 * "Send to Main View" will apply the shader to the model.
 */
public class ShaderDataCard extends ProjectDataCard
{
    private final UserShader shader;

    /**
     * Used in initialization
     * @param shader
     */
    private static Map<String, Runnable> getActionMap(UserShader shader)
    {
        Runnable viewShader = () ->
        {
            // Sets the model to the shader
            Global.state().getUserShaderModel().setUserShader(shader);
        };

        Runnable sendToCarousel = () ->
        {
            // Adds the shader to the carousel
            Global.state().getCarouselModel().addToCarousel(shader);
        };

        return Map.of(
            "Send to Main View", viewShader,
            "Send to Carousel", sendToCarousel);
    }

    public ShaderDataCard(String internalName, String title, UserShader shader, String imagePath, String filePath, Map<String, String> textFields,
                          Collection<? extends Map<String, Runnable>> actionGroups)
    {
        super(internalName, title, imagePath, textFields,
            Stream.concat(Stream.of(getActionMap(shader)), actionGroups.stream()).collect(Collectors.toList()), filePath);
        this.shader = shader;
    }

    public ShaderDataCard(String internalName, UserShader shader, String imagePath, String filePath, Map<String, String> textFields,
                          Collection<? extends Map<String, Runnable>> actionGroups)
    {
        // FIX: Swapped 'filePath' position to match the primary constructor above
        this(internalName, shader.getFriendlyName(), shader, imagePath, filePath, textFields, actionGroups);
    }

    public ShaderDataCard(String internalName, UserShader shader, String imagePath, String filePath, Map<String, String> textFields, Map<String, Runnable> actions)
    {
        super(internalName, shader.getFriendlyName(), imagePath, textFields, List.of(getActionMap(shader), actions), filePath);
        this.shader = shader;
    }

    public ShaderDataCard(String internalName, UserShader shader, String imagePath, String filePath, Map<String, String> textFields)
    {
        // FIX: Wrapped getActionMap in List.of() to match List<? extends Map> signature
        super(internalName, shader.getFriendlyName(), imagePath, textFields, List.of(getActionMap(shader)), filePath);
        this.shader = shader;
    }

    public ShaderDataCard(String internalName, UserShader shader, String imagePath, String filePath)
    {
        // FIX: Wrapped getActionMap in List.of() to match List<? extends Map> signature
        super(internalName, shader.getFriendlyName(), imagePath, Map.of(), List.of(getActionMap(shader)), filePath);
        this.shader = shader;
    }

    public UserShader getShader()
    {
        return shader;
    }
}
