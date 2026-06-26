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
            "View Shader", viewShader,
            "Send to Carousel", sendToCarousel);
    }

    public ShaderDataCard(String title, UserShader shader, String imagePath, Map<String, String> textFields,
                          Collection<? extends Map<String, Runnable>> actionGroups)
    {
        super(title, imagePath, textFields,
            Stream.concat(Stream.of(getActionMap(shader)), actionGroups.stream()).collect(Collectors.toList()));
        this.shader = shader;
    }

    public ShaderDataCard(UserShader shader, String imagePath, Map<String, String> textFields,
                          Collection<? extends Map<String, Runnable>> actionGroups)
    {
        this(shader.getFriendlyName(), shader, imagePath, textFields, actionGroups);
    }

    public ShaderDataCard(UserShader shader, String imagePath, Map<String, String> textFields, Map<String, Runnable> actions)
    {
        super(shader.getFriendlyName(), imagePath, textFields, List.of(getActionMap(shader), actions));
        this.shader = shader;
    }

    public ShaderDataCard(UserShader shader, String imagePath, Map<String, String> textFields)
    {
        super(shader.getFriendlyName(), imagePath, textFields, getActionMap(shader));
        this.shader = shader;
    }

    public ShaderDataCard(UserShader shader, String imagePath)
    {
        super(shader.getFriendlyName(), imagePath, Map.of(), getActionMap(shader));
        this.shader = shader;
    }

    public UserShader getShader()
    {
        return shader;
    }
}
