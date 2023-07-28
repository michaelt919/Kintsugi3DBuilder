/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.export.general;

import java.io.File;

import kintsugi3d.gl.core.Context;
import kintsugi3d.builder.state.ReadonlySettingsModel;

public final class RequestFactoryImplementation implements RequestFactory
{
    private final ReadonlySettingsModel settingsModel;

    private RequestFactoryImplementation(ReadonlySettingsModel settingsModel)
    {
        this.settingsModel = settingsModel;
    }

    public static RequestFactoryImplementation create(ReadonlySettingsModel settingsModel)
    {
        return new RequestFactoryImplementation(settingsModel);
    }

    @Override
    public <ContextType extends Context<ContextType>>  RenderRequestBuilder<ContextType> buildSingleFrameRenderRequest(
            File fragmentShader, File outputDirectory, String outputImageName)
    {
        //noinspection UnnecessarilyQualifiedInnerClassAccess
        return new SingleFrameRenderRequest.Builder<>(outputImageName, settingsModel, fragmentShader, outputDirectory);
    }

    @Override
    public <ContextType extends Context<ContextType>>  RenderRequestBuilder<ContextType> buildMultiframeRenderRequest(
            File fragmentShader, File outputDirectory, int frameCount)
    {
        //noinspection UnnecessarilyQualifiedInnerClassAccess
        return new MultiframeRenderRequest.Builder<>(frameCount, settingsModel, fragmentShader, outputDirectory);
    }

    @Override
    public <ContextType extends Context<ContextType>> RenderRequestBuilder<ContextType> buildMultiviewRenderRequest(
            File fragmentShader, File outputDirectory)
    {
        //noinspection UnnecessarilyQualifiedInnerClassAccess
        return new MultiviewRenderRequest.Builder<>(settingsModel, fragmentShader, outputDirectory);
    }

    @Override
    public <ContextType extends Context<ContextType>> RenderRequestBuilder<ContextType> buildMultiviewRetargetRenderRequest(
            File fragmentShader, File outputDirectory, File retargetViewSet)
    {
        //noinspection UnnecessarilyQualifiedInnerClassAccess
        return new MultiviewRetargetRenderRequest.Builder<>(retargetViewSet, settingsModel, fragmentShader, outputDirectory);
    }
}
