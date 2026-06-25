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

package kintsugi3d.builder.javafx.multithread;

import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import kintsugi3d.builder.javafx.internal.CarouselModel;
import kintsugi3d.builder.state.scene.UserShader;
/*
This class is for the global state, so we can have a list of shaders in the carousel.
Has two methods, one to get an arraylist of the shaders currently in the carousel.
And another to add a shader to the carousel list.
 */
public class SynchronizedCarouselModel implements CarouselModel
{
    private final ObservableList<UserShader> carouselShaders = FXCollections.observableArrayList();

    /**
     * returns the list of shaders currently held in global carousel model.
     * @return
     */
    @Override
    public ObservableList<UserShader> getCarouselShaders()
    {
        return carouselShaders;
    }
    /**
     * Looks through the existing shaders if the parameter shader is already
     * in carousel it will not add the shader. If it is not it will add
     * shader to carouselShaders list.
     * @param shader
     */
    @Override
    public void addToCarousel(UserShader shader)
    {
        boolean alreadyInCarousel = false;

        //For loop looks through List for any that match the shader
        //that is trying to be sent to carousel
        for (UserShader element : carouselShaders)
        {
            if (shader.equals(element))
            {
                //if one matches:
                alreadyInCarousel = true;
            }
        }

        //Prevents duplicate shaders in carousel / if the shaders don't match
        //then shader is sent to carousel
        if (!alreadyInCarousel)
        {
            carouselShaders.add(shader);
        }
    }

    /**
     * Clears all the shaders in carouselShaders list.
     */
    @Override
    public void clearCarousel() { carouselShaders.clear(); }
}
