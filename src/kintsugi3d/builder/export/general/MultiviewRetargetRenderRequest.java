/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.export.general;

import java.io.File;
import java.text.MessageFormat;
import java.util.function.Consumer;

import kintsugi3d.builder.core.IBRInstance;
import kintsugi3d.builder.core.ObservableIBRRequest;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.io.ViewSetReaderFromVSET;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.builder.state.ReadonlySettingsModel;
import kintsugi3d.gl.core.*;

class MultiviewRetargetRenderRequest extends RenderRequestBase
{
    private final File targetViewSetFile;

    MultiviewRetargetRenderRequest(int width, int height, ReadonlySettingsModel settingsModel,
        Consumer<Program<? extends Context<?>>> shaderSetupCallback,
        File targetViewSet, File vertexShader, File fragmentShader, File outputDirectory)
    {
        super(width, height, settingsModel, shaderSetupCallback, vertexShader, fragmentShader, outputDirectory);
        this.targetViewSetFile = targetViewSet;
    }

    static class Builder extends BuilderBase
    {
        private final File targetViewSet;

        Builder(File targetViewSet, ReadonlySettingsModel settingsModel, File fragmentShader, File outputDirectory)
        {
            super(settingsModel, fragmentShader, outputDirectory);
            this.targetViewSet = targetViewSet;
        }

        @Override
        public ObservableIBRRequest create()
        {
            return new MultiviewRetargetRenderRequest(getWidth(), getHeight(), getSettingsModel(), getShaderSetupCallback(),
                targetViewSet, getVertexShader(), getFragmentShader(), getOutputDirectory());
        }
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(
        IBRInstance<ContextType> renderable, ProgressMonitor monitor) throws Exception
    {
        ReadonlyViewSet targetViewSet = ViewSetReaderFromVSET.getInstance().readFromFile(targetViewSetFile);

        IBRResourcesImageSpace<ContextType> resources = renderable.getIBRResources();

        try
        (
            ProgramObject<ContextType> program = createProgram(resources);
            FramebufferObject<ContextType> framebuffer = createFramebuffer(resources.getContext());
            Drawable<ContextType> drawable = createDrawable(program, resources)
        )
        {
            for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
            {
                if (monitor != null)
                {
                    monitor.allowUserCancellation();
                }

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
                    if (parts.length == 1){
                        fileName = fileName + ".png";
                    }
                    else{
                        parts[parts.length - 1] = "png";
                        fileName = String.join(".", parts);
                    }
                }

                File exportFile = new File(getOutputDirectory(), fileName);
                getOutputDirectory().mkdirs();
                framebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", exportFile);

                if (monitor != null)
                {
                    monitor.setProgress((double) i / (double) resources.getViewSet().getCameraPoseCount(),
                        MessageFormat.format("{0} ({1}/{2})", resources.getViewSet().getImageFileName(i), i+1, resources.getViewSet().getCameraPoseCount()));
                }
            }
        }
    }
}
