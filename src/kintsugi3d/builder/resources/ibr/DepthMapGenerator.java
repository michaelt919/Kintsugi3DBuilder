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

package kintsugi3d.builder.resources.ibr;

import java.io.FileNotFoundException;
import java.io.IOException;

import kintsugi3d.gl.core.*;
import kintsugi3d.gl.geometry.GeometryResources;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.builder.core.ReadonlyViewSet;

/**
 * Encapsulates the process of generating a depth map for occlusion / shadow culling with image-based rendering
 * @param <ContextType>
 */
public class DepthMapGenerator<ContextType extends Context<ContextType>> implements Resource
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
        depthRenderingProgram = IBRResourcesImageSpace.getDepthMapProgramBuilder(geometryResources.positionBuffer.getContext()).createProgram();
        depthDrawable = depthRenderingProgram.getContext().createDrawable(depthRenderingProgram);
        depthDrawable.addVertexBuffer("position", geometryResources.positionBuffer);
    }

    public void generateDepthMap(ReadonlyViewSet viewSet, int viewIndex, Framebuffer<ContextType> framebuffer)
    {
        framebuffer.clearDepthBuffer();

        depthRenderingProgram.setUniform("model_view", viewSet.getCameraPose(viewIndex));
        depthRenderingProgram.setUniform("projection",
            viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex))
                .getProjectionMatrix(
                    viewSet.getRecommendedNearPlane(),
                    viewSet.getRecommendedFarPlane()
                )
        );

        depthDrawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
    }

    /**
     *
     * @param viewSet
     * @param viewIndex
     * @param framebuffer
     * @return The shadow matrix
     */
    public Matrix4 generateShadowMap(ReadonlyViewSet viewSet, int viewIndex, Framebuffer<ContextType> framebuffer)
    {
        framebuffer.clearDepthBuffer();

        Matrix4 modelView = Matrix4.lookAt(
            viewSet.getCameraPoseInverse(viewIndex).times(viewSet.getLightPosition(0).asPosition()).getXYZ(),
            geometryResources.geometry.getCentroid(),
            new Vector3(0, 1, 0));
        depthRenderingProgram.setUniform("model_view", modelView);

        Matrix4 projection = viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex))
            .getProjectionMatrix(
                viewSet.getRecommendedNearPlane(),
                viewSet.getRecommendedFarPlane() * 2 // double it for good measure
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
