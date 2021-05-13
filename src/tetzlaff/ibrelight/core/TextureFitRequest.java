/*
 *  Copyright (c) Michael Tetzlaff 2021
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.core;

import java.io.File;

import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

public abstract class TextureFitRequest implements IBRRequest
{
    protected final int width;
    protected final int height;
    protected final File outputDirectory;
    protected final ReadonlySettingsModel settingsModel;

    public TextureFitRequest(int width, int height, File outputDirectory, ReadonlySettingsModel settingsModel)
    {
        this.width = width;
        this.height = height;
        this.outputDirectory = outputDirectory;
        this.settingsModel = settingsModel;
    }

//    @Override
//    public <ContextType extends Context<ContextType>> void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback)
//    {
//        IBRResources<ContextType> resources = renderable.getResources();
//        resources.context.getState().disableBackFaceCulling();
//
//        // Calculate reasonable image resolution for reconstructed images (supplemental output)
//        Projection defaultProj = resources.viewSet.getCameraProjection(resources.viewSet.getCameraProjectionIndex(
//            resources.viewSet.getPrimaryViewIndex()));
//
//        int imageWidth;
//        int imageHeight;
//
//        if (defaultProj.getAspectRatio() < 1.0)
//        {
//            imageWidth = width;
//            imageHeight = Math.round(imageWidth / defaultProj.getAspectRatio());
//        }
//        else
//        {
//            imageHeight = height;
//            imageWidth = Math.round(imageHeight * defaultProj.getAspectRatio());
//        }
//    }
}
