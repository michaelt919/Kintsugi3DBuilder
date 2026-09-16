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

package kintsugi3d.builder.resources.project;

import kintsugi3d.builder.core.viewset.View;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.geometry.GeometryResources;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Encapsulates the process of generating a depth map for occlusion / shadow culling with image-based rendering
 * @param <ContextType>
 */
public final class DepthMapGenerator<ContextType extends Context<ContextType>> implements Resource
{
    private final ProgramObject<ContextType> depthRenderingProgram;
    private final Drawable<ContextType> depthDrawable;
    private final GeometryResources<ContextType> geometryResources;

    /**
     *
     * @param geometryResources
     * @return
     * @throws FileNotFoundException Thrown if the depth map shader cannot be loaded
     */
    public static <ContextType extends Context<ContextType>>DepthMapGenerator<ContextType> createFromGeometryResources(
        GeometryResources<ContextType> geometryResources) throws IOException
    {
        return new DepthMapGenerator<>(geometryResources);
    }

    private DepthMapGenerator(GeometryResources<ContextType> geometryResources) throws IOException
    {
        this.geometryResources = geometryResources;
        depthRenderingProgram = getDepthMapProgramBuilder(geometryResources.positionBuffer.getContext()).createProgram();
        depthDrawable = depthRenderingProgram.getContext().createDrawable(depthRenderingProgram);
        depthDrawable.addVertexBuffer("position", geometryResources.positionBuffer);
    }

    static <ContextType extends Context<ContextType>> ProgramBuilder<ContextType> getDepthMapProgramBuilder(ContextType context)
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"));
    }

    public void generateDepthMap(View view, Framebuffer<ContextType> framebuffer)
    {
        framebuffer.clearDepthBuffer();

        depthRenderingProgram.setUniform("model_view", view.getCameraPose());
        depthRenderingProgram.setUniform("projection", view.getProjectionMatrix());

        depthDrawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
    }

    /**
     *
     * @param view
     * @param framebuffer
     * @return The shadow matrix
     */
    public Matrix4 generateShadowMap(View view, Framebuffer<ContextType> framebuffer)
    {
        framebuffer.clearDepthBuffer();

        Matrix4 modelView = Matrix4.lookAt(
            view.getCameraPoseInverse().times(view.getLightPosition().asPosition()).getXYZ(),
            geometryResources.geometry.getCentroid(),
            new Vector3(0, 1, 0));
        depthRenderingProgram.setUniform("model_view", modelView);

        Matrix4 projection = view.getCameraProjection()
            .getProjectionMatrix(
                view.getContainingViewSet().getRecommendedNearPlane(),
                view.getContainingViewSet().getRecommendedFarPlane() * 2 // double it for good measure
            );
        depthRenderingProgram.setUniform("projection", projection);

        depthDrawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

        return projection.times(modelView);
    }

    @Override
    public void close()
    {
        depthRenderingProgram.close();
        depthDrawable.close();
    }
}
