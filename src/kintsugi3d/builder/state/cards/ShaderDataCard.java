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
import kintsugi3d.builder.state.shader.ShaderInfo;

import java.util.Collection;
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
    private final ShaderInfo shader;

    /**
     * Used in initialization
     * @param shader
     */
    private static Map<String, Runnable> getActionMap(ShaderInfo shader)
    {
        Runnable viewShader = () ->
        {
            // Sets the model to the shader
            Global.state().getUserShaderModel().setActiveShader(shader);
        };

        Runnable sendToCarousel = () ->
        {
            // Adds the shader to the carousel
            Global.state().getCarouselModel().addToCarousel(shader);
        };

        Runnable sendToSplitView = () ->
        {
            //TODO: Add code for sending shader to split view
            //Global.state().getSplitCanvasModel().setCanvas();
        };

        return Map.of(
            "Send to Main View", viewShader,
            "Send to Carousel", sendToCarousel/*,
            "Send to Split View", sendToSplitView*/);
    }

    /**
     * Overrides the shader's friendly name with a specified title.
     * @param internalName
     * @param title
     * @param shader
     * @param thumbnailPath
     * @param fullResImageFilePath
     * @param textFields
     * @param actionGroups
     * @param isEnabled
     */
    public ShaderDataCard(
        String internalName, String title, ShaderInfo shader, String thumbnailPath, String fullResImageFilePath,
        Map<String, String> textFields, Collection<? extends Map<String, Runnable>> actionGroups, boolean isEnabled)
    {
        super(internalName, title, fullResImageFilePath, thumbnailPath, textFields,
            Stream.concat(Stream.of(getActionMap(shader)), actionGroups.stream()).collect(Collectors.toList()),
            isEnabled);
        this.shader = shader;
    }

    /**
     * Uses the shader's friendly name as the title.
     * @param internalName
     * @param shader
     * @param thumbnailPath
     * @param fullResImageFilePath
     * @param textFields
     * @param actionGroups
     * @param isEnabled
     */
    public ShaderDataCard(
        String internalName, ShaderInfo shader, String thumbnailPath, String fullResImageFilePath,
        Map<String, String> textFields, Collection<? extends Map<String, Runnable>> actionGroups, boolean isEnabled)
    {
        this(internalName, shader.getFriendlyName(), shader, thumbnailPath, fullResImageFilePath, textFields,
            actionGroups, isEnabled);
    }

    public ShaderInfo getShader()
    {
        return shader;
    }
}
