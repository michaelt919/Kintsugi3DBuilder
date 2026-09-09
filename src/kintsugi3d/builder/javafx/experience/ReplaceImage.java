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

package kintsugi3d.builder.javafx.experience;

import javafx.application.Platform;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.IOModel;
import kintsugi3d.builder.core.texture.ImageReplaceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ReplaceImage extends ExperienceBase
{
    private static final Logger LOG = LoggerFactory.getLogger(ReplaceImage.class);

    private ImageReplaceData currentData;

    public ReplaceImage()
    {
        Platform.runLater(this::initWithReattempt);
    }

    private void initWithReattempt()
    {
        IOModel ioModel = Global.state().getIOModel();
        if (ioModel.hasValidHandler()) // might not be valid immediately as the rendering thread is booting up
        {
            ioModel.addMainRenderableLoadCallback(instance ->
                instance.setUserImageReplaceHandler(replaceData ->
                {
                    this.currentData = replaceData;
                    this.tryOpen();
                }));
        }
        else // if not ready yet, try again on the next JavaFX tick.
        {
            Platform.runLater(this::initWithReattempt);
        }
    }

    @Override
    public String getName() { return "Replace Texture"; }

    @Override
    protected void open() throws IOException
    {
        this.buildPagedModal(currentData).then("/fxml/modals/workflow/ReplaceImage.fxml").finish();
    }
}
