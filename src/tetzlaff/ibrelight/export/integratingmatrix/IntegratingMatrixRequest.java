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

package tetzlaff.ibrelight.export.integratingmatrix;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.*;
import tetzlaff.ibrelight.rendering.IBRResources;

public class IntegratingMatrixRequest implements IBRRequest
{
    private final int resampleWidth;
    private final int resampleHeight;
    private final File resampleVSETFile;
    private final File resampleExportPath;
    
    public IntegratingMatrixRequest(int width, int height, File targetVSETFile, File exportPath)
    {
        this.resampleWidth = width;
        this.resampleHeight = height;
        this.resampleVSETFile = targetVSETFile;
        this.resampleExportPath = exportPath;
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback) throws IOException
    {
        IBRResources<ContextType> resources = renderable.getResources();
        ViewSet targetViewSet = ViewSet.loadFromVSETFile(resampleVSETFile);

        resampleExportPath.mkdirs();

        Matrix4 modelView = targetViewSet.getCameraPose(targetViewSet.getPrimaryViewIndex());

        Matrix4 projection = targetViewSet.getCameraProjection(targetViewSet.getCameraProjectionIndex(targetViewSet.getPrimaryViewIndex()))
            .getProjectionMatrix(targetViewSet.getRecommendedNearPlane(), targetViewSet.getRecommendedFarPlane());

        try
        (
            FramebufferObject<ContextType> framebuffer = resources.context.buildFramebufferObject(resampleWidth, resampleHeight)
                .addColorAttachment()
                .addDepthAttachment()
                .createFramebufferObject();

            FramebufferObject<ContextType> depthFBO = resources.context.buildFramebufferObject(resampleWidth, resampleHeight)
                .addDepthAttachment()
                .createFramebufferObject()
        )
        {
            try(Program<ContextType> shadowProgram = resources.context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "depth.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "depth.frag"))
                .createProgram())
            {
                Drawable<ContextType> shadowDrawable = resources.context.createDrawable(shadowProgram);
                shadowDrawable.addVertexBuffer("position", resources.positionBuffer);

                shadowProgram.setUniform("projection", projection);
                shadowProgram.setUniform("model_view", modelView);
                shadowProgram.setUniform("viewPos", modelView.quickInverse(0.01f).getColumn(3).getXYZ());

                depthFBO.clearDepthBuffer();
                shadowDrawable.draw(PrimitiveMode.TRIANGLES, depthFBO);

                resources.context.flush();
            }

            int progress = 0;

            callback.setMaximum(130);

            for (int roughness = 5; roughness <= 50; roughness += 5)
            {
                framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
//                framebuffer.clearColorBuffer(0, 1.0f, 1.0f, 1.0f, 1.0f);
                framebuffer.clearDepthBuffer();

                try (Program<ContextType> program = resources.getIBRShaderProgramBuilder(RenderingMode.IMAGE_BASED_WITH_MATERIALS)
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
                    .define("ENVIRONMENT_TEXTURE_ENABLED", true)
                    .define("ANALYTIC_ROUGHNESS", roughness * 0.01)
                    .define("ANALYTIC_BUMP_HEIGHT", 0.0)
                    .addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
                    .addShader(ShaderType.FRAGMENT, new File("shaders/relight/relight.frag"))
                    .createProgram())
                {
                    drawInstance(resources, renderable.getEnvironmentMap().orElse(null), modelView, renderable.getEnvironmentMapMatrix(),
                        projection, renderable.getLightingModel().getAmbientLightColor(), framebuffer, depthFBO, program);
                }

                framebuffer.saveColorBufferToFile(0, "PNG",
                    new File(resampleExportPath,
                        String.format("r%02d-uv00_%s.png", roughness,
                            renderable.getActiveViewSet().getGeometryFile().getName().split("\\.")[0])));

                progress++;

                if (callback != null)
                {
                    callback.setProgress(progress);
                }

                for (int uvScale = 1; uvScale <= 12; uvScale++)
                {
                    framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
//                    framebuffer.clearColorBuffer(0, 1.0f, 1.0f, 1.0f, 1.0f);
                    framebuffer.clearDepthBuffer();

                    try (Program<ContextType> program = resources.getIBRShaderProgramBuilder(RenderingMode.IMAGE_BASED_WITH_MATERIALS)
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
                        .define("ENVIRONMENT_TEXTURE_ENABLED", true)
                        .define("ANALYTIC_ROUGHNESS", roughness * 0.01)
                        .define("ANALYTIC_UV_SCALE", uvScale * 1.0)
                        .addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
                        .addShader(ShaderType.FRAGMENT, new File("shaders/relight/relight.frag"))
                        .createProgram())
                    {
                        drawInstance(resources, renderable.getEnvironmentMap().orElse(null), modelView, renderable.getEnvironmentMapMatrix(),
                            projection, renderable.getLightingModel().getAmbientLightColor(), framebuffer, depthFBO, program);
                    }

                    framebuffer.saveColorBufferToFile(0, "PNG",
                        new File(resampleExportPath,
                            String.format("r%02d-uv%02d_%s.png", roughness, uvScale,
                                renderable.getActiveViewSet().getGeometryFile().getName().split("\\.")[0])));

                    progress++;

                    if (callback != null)
                    {
                        callback.setProgress(progress);
                    }
                }
            }
        }
    }

    private static <ContextType extends Context<ContextType>> void drawInstance(
        IBRResources<ContextType> resources, Cubemap<ContextType> environmentMap, Matrix4 modelView, Matrix4 environmentMatrix, Matrix4 projection,
        Vector3 ambientColor, FramebufferObject<ContextType> framebuffer, FramebufferObject<ContextType> depthFBO, Program<ContextType> program)
    {
        resources.setupShaderProgram(program);

        Drawable<ContextType> drawable = resources.context.createDrawable(program);

        drawable.addVertexBuffer("position", resources.positionBuffer);

        if (resources.normalBuffer != null)
        {
            drawable.addVertexBuffer("normal", resources.normalBuffer);
        }

        if (resources.texCoordBuffer != null)
        {
            drawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
        }

        if (resources.tangentBuffer != null)
        {
            drawable.addVertexBuffer("tangent", resources.tangentBuffer);
        }

        program.setUniform("renderGamma", 2.2f);
        program.setUniform("occlusionBias", 0.0025f);
        program.setTexture("screenSpaceDepthBuffer", depthFBO.getDepthAttachmentTexture());

        program.setUniform("projection", projection);
        program.setUniform("fullProjection", projection);
        program.setUniform("model_view", modelView);
        program.setUniform("viewPos", modelView.quickInverse(0.01f).getColumn(3).getXYZ());

        if (environmentMap == null)
        {
            program.setTexture("environmentMap", resources.context.getTextureFactory().getNullTexture(SamplerType.FLOAT_CUBE_MAP));
        }
        else
        {
            program.setUniform("useEnvironmentMap", true);
            program.setTexture("environmentMap", environmentMap);
            program.setUniform("environmentMipMapLevel",
                Math.max(0, Math.min(environmentMap.getMipmapLevelCount() - 1,
//                    lightingModel.getEnvironmentMapFilteringBias()
                        + (float)(0.5 *
                        Math.log(6 * (double)environmentMap.getFaceSize() * (double)environmentMap.getFaceSize()
                            / (double)resources.viewSet.getCameraPoseCount() )
                        / Math.log(2.0)))));
            program.setUniform("diffuseEnvironmentMipMapLevel", environmentMap.getMipmapLevelCount() - 1);

            program.setUniform("envMapMatrix", environmentMatrix);
        }

        program.setUniform("ambientColor", ambientColor);

        // Render to off-screen buffer
        drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

        // Flush to prevent timeout
        resources.context.flush();
    }
}
