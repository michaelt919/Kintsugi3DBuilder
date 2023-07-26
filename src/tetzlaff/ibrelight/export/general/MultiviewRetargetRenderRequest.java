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

package tetzlaff.ibrelight.export.general;

import java.io.File;
import java.util.function.Consumer;

import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.*;
import tetzlaff.ibrelight.io.ViewSetReaderFromVSET;
import tetzlaff.ibrelight.rendering.resources.IBRResourcesImageSpace;
import tetzlaff.models.ReadonlySettingsModel;

class MultiviewRetargetRenderRequest<ContextType extends Context<ContextType>> extends RenderRequestBase<ContextType>
{
    private final File targetViewSetFile;

    MultiviewRetargetRenderRequest(int width, int height, ReadonlySettingsModel settingsModel, Consumer<Program<ContextType>> shaderSetupCallback,
                                   File targetViewSet, File vertexShader, File fragmentShader, File outputDirectory)
    {
        super(width, height, settingsModel, shaderSetupCallback, vertexShader, fragmentShader, outputDirectory);
        this.targetViewSetFile = targetViewSet;
    }

    static class Builder<ContextType extends Context<ContextType>> extends BuilderBase<ContextType>
    {
        private final File targetViewSet;

        Builder(File targetViewSet, ReadonlySettingsModel settingsModel, File fragmentShader, File outputDirectory)
        {
            super(settingsModel, fragmentShader, outputDirectory);
            this.targetViewSet = targetViewSet;
        }

        @Override
        public IBRRequest<ContextType> create()
        {
            return new MultiviewRetargetRenderRequest<>(getWidth(), getHeight(), getSettingsModel(), getShaderSetupCallback(),
                targetViewSet, getVertexShader(), getFragmentShader(), getOutputDirectory());
        }
    }

    @Override
    public void executeRequest(IBRInstance<ContextType> renderable, LoadingMonitor callback) throws Exception
    {
        ReadonlyViewSet targetViewSet = ViewSetReaderFromVSET.getInstance().readFromFile(targetViewSetFile);

        IBRResourcesImageSpace<ContextType> resources = renderable.getIBRResources();

        try
        (
            ProgramObject<ContextType> program = createProgram(resources);
            FramebufferObject<ContextType> framebuffer = createFramebuffer(resources.getContext())
        )
        {
            Drawable<ContextType> drawable = createDrawable(program, resources);
            for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
            {
                program.setUniform("viewIndex", i);
                program.setUniform("model_view", targetViewSet.getCameraPose(i));
                program.setUniform("projection",
                    targetViewSet.getCameraProjection(targetViewSet.getCameraProjectionIndex(i))
                        .getProjectionMatrix(targetViewSet.getRecommendedNearPlane(), targetViewSet.getRecommendedFarPlane()));

                render(drawable, framebuffer);

                String fileName = targetViewSet.getImageFileName(i);

                if (!fileName.endsWith(".png"))
                {
                    String[] parts = fileName.split("\\.");
                    parts[parts.length - 1] = "png";
                    fileName = String.join(".", parts);
                }

                File exportFile = new File(getOutputDirectory(), fileName);
                getOutputDirectory().mkdirs();
                framebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", exportFile);

                if (callback != null)
                {
                    callback.setProgress((double) i / (double) resources.getViewSet().getCameraPoseCount());
                }
            }
        }
    }
}
