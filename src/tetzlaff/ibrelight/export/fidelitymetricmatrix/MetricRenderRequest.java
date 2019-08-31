/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.fidelitymetricmatrix;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.RenderingMode;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.ibrelight.rendering.IBRResources;

class MetricRenderRequest
{

    private final int roughness;
    private final int uvScale;
    private final int width;
    private final int height;
    private final File targetViewSetFile;
    private final File outputDirectory;

    MetricRenderRequest(int roughness, int uvScale, int width, int height, File targetViewSetFile, File outputDirectory)
    {
        this.roughness = roughness;
        this.uvScale = uvScale;
        this.width = width;
        this.height = height;
        this.targetViewSetFile = targetViewSetFile;
        this.outputDirectory = outputDirectory;
    }

    private <ContextType extends Context<ContextType>> FramebufferObject<ContextType> createFramebuffer(ContextType context)
    {
        return context.buildFramebufferObject(width, height)
            .addColorAttachment()
            .addDepthAttachment()
            .createFramebufferObject();
    }

    private static <ContextType extends Context<ContextType>> Drawable<ContextType>
        createDrawable(Program<ContextType> program, IBRResources<ContextType> resources)
    {
        Drawable<ContextType> drawable = program.getContext().createDrawable(program);
        drawable.addVertexBuffer("position", resources.positionBuffer);
        drawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
        drawable.addVertexBuffer("normal", resources.normalBuffer);
        drawable.addVertexBuffer("tangent", resources.tangentBuffer);
        return drawable;
    }

    private static <ContextType extends Context<ContextType>> void render(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        drawable.getContext().getState().disableBackFaceCulling();
        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
        framebuffer.clearDepthBuffer();
        drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
    }

    public <ContextType extends Context<ContextType>>
        void executeRequest(IBRRenderable<ContextType> renderable) throws IOException
    {
        ViewSet targetViewSet = ViewSet.loadFromVSETFile(this.targetViewSetFile);

        IBRResources<ContextType> resources = renderable.getResources();

        try
        (
            Program<ContextType> program =
                resources.getIBRShaderProgramBuilder(RenderingMode.IMAGE_BASED_WITH_MATERIALS)
                    .define("MATERIAL_EXPLORATION_MODE", 1)
                    .define("RAY_DEPTH_GRADIENT", 0.2 * renderable.getActiveGeometry().getBoundingRadius())
                    .define("RAY_POSITION_JITTER", 0.02 * renderable.getActiveGeometry().getBoundingRadius())
                    .define("PHYSICALLY_BASED_MASKING_SHADOWING", true)
                    .define("FRESNEL_EFFECT_ENABLED", true)
                    .define("SHADOWS_ENABLED", true)
                    .define("BUEHLER_ALGORITHM", true)
                    .define("SORTING_SAMPLE_COUNT", 5)
                    .define("RELIGHTING_ENABLED", true)
                    .define("VISIBILITY_TEST_ENABLED", true)
                    .define("VIRTUAL_LIGHT_COUNT", 0)
                    .define("ENVIRONMENT_ILLUMINATION_ENABLED", true)
                    .define("ENVIRONMENT_TEXTURE_ENABLED", false)
                    .define("ANALYTIC_ROUGHNESS", roughness * 0.01)
                    .define("ANALYTIC_UV_SCALE", uvScale * 1.0)
                    .define("ANALYTIC_BUMP_HEIGHT", uvScale > 0 ? 4.0 : 0.0)
                    .addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
                    .addShader(ShaderType.FRAGMENT, new File("shaders/relight/newFidelity.frag"))
                    .createProgram();
            FramebufferObject<ContextType> framebuffer = createFramebuffer(resources.context)
        )
        {
            resources.setupShaderProgram(program);

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

                File exportFile = new File(outputDirectory, fileName);
                outputDirectory.mkdirs();
                framebuffer.saveColorBufferToFile(0, "PNG", exportFile);
            }
        }
    }
}
