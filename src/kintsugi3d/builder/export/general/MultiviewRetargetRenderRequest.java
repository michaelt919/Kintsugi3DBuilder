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

package kintsugi3d.builder.export.general;

import kintsugi3d.builder.core.ObservableProjectGraphicsRequest;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.io.ViewSetReaderFromVSET;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace;
import kintsugi3d.gl.core.*;
import kintsugi3d.util.ImageFinder;

import java.io.File;
import java.text.MessageFormat;
import java.util.function.Consumer;

class MultiviewRetargetRenderRequest extends RenderRequestBase
{
    private final File targetViewSetFile;

    MultiviewRetargetRenderRequest(int width, int height,
        Consumer<Program<? extends Context<?>>> shaderSetupCallback,
        File targetViewSet, File vertexShader, File fragmentShader, File outputDirectory)
    {
        super(width, height, shaderSetupCallback, vertexShader, fragmentShader, outputDirectory);
        this.targetViewSetFile = targetViewSet;
    }

    static class Builder extends BuilderBase
    {
        private final File targetViewSet;

        Builder(File targetViewSet, File fragmentShader, File outputDirectory)
        {
            super(fragmentShader, outputDirectory);
            this.targetViewSet = targetViewSet;
        }

        @Override
        public ObservableProjectGraphicsRequest create()
        {
            return new MultiviewRetargetRenderRequest(getWidth(), getHeight(), getShaderSetupCallback(),
                targetViewSet, getVertexShader(), getFragmentShader(), getOutputDirectory());
        }
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(
        ProjectInstance<ContextType> renderable, ProgressMonitor monitor) throws Exception
    {
        ReadonlyViewSet targetViewSet = ViewSetReaderFromVSET.getInstance().readFromFile(targetViewSetFile).finish();

        GraphicsResourcesImageSpace<ContextType> resources = renderable.getResources();

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

                String fileName = ImageFinder.getInstance().getImageFileNameWithFormat(
                    renderable.getActiveViewSet().getImageFileName(i), "png");

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
