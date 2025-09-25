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

package kintsugi3d.builder.javafx.multithread;

import javafx.application.Platform;
import kintsugi3d.builder.state.scene.UserShader;
import kintsugi3d.builder.state.scene.UserShaderModel;

import java.util.function.Consumer;

public class SynchronizedUserShaderModel implements UserShaderModel
{
    private final UserShaderModel base;

    private final SynchronizedValue<UserShader> userShader;

    public SynchronizedUserShaderModel(UserShaderModel base)
    {
        this.base = base;
        this.userShader = SynchronizedValue.createFromFunctions(base::getUserShader, base::setUserShader);
    }

    @Override
    public UserShader getUserShader()
    {
        return userShader.getValue();
    }

    @Override
    public void registerHandler(Consumer<UserShader> shaderHandler)
    {
        Platform.runLater(() -> base.registerHandler(shaderHandler));
    }

    @Override
    public void setUserShader(UserShader userShader)
    {
        this.userShader.setValue(userShader);
    }
}
