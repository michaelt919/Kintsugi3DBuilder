/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.internal;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import kintsugi3d.builder.state.scene.UserShader;
import kintsugi3d.builder.state.scene.UserShaderModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public class ObservableUserShaderModel implements UserShaderModel
{
    private final ObjectProperty<UserShader> userShader = new SimpleObjectProperty<>();

    private final Collection<Consumer<UserShader>> shaderHandlers = new ArrayList<>(1);

    public ObjectProperty<UserShader> getUserShaderProperty()
    {
        return userShader;
    }

    @Override
    public UserShader getUserShader()
    {
        return userShader.get();
    }

    @Override
    public void registerHandler(Consumer<UserShader> shaderHandler)
    {
        shaderHandlers.add(shaderHandler);
    }

    @Override
    public void setUserShader(UserShader userShader)
    {
        this.userShader.set(userShader);

        for (var shaderHandler : shaderHandlers)
        {
            shaderHandler.accept(userShader);
        }
    }
}
