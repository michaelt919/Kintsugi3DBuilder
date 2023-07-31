/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.resources;

import java.io.File;
import java.io.IOException;

import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.geometry.GeometryResources;
import kintsugi3d.gl.geometry.ReadonlyVertexGeometry;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.builder.core.ReadonlyLoadOptionsModel;
import kintsugi3d.builder.core.ReadonlyViewSet;

/**
 * For use i.e. with projtex_single.frag
 */
public class SingleCalibratedImageResource<ContextType extends Context<ContextType>> implements Resource
{
    private final ContextType context;
    private final ReadonlyViewSet viewSet;
    private final int viewIndex;
    private GeometryResources<ContextType> geometryResources;
    private final boolean geometryResourcesOwned;
    private Texture2D<ContextType> colorTexture;
    private Texture2D<ContextType> depthTexture;
    private Texture2D<ContextType> shadowTexture;
    private final Matrix4 shadowMatrix;

    /**
     * Creates a calibrated image resource, loading the geometry onto the GPU as a new resource owned by this resource (if and only if geometry is not null).
     * @param context
     * @param viewSet
     * @param viewIndex
     * @param imageFile
     * @param geometry
     * @param loadOptions
     * @throws IOException
     */
    SingleCalibratedImageResource(ContextType context, ReadonlyViewSet viewSet, int viewIndex, File imageFile, ReadonlyVertexGeometry geometry,
        ReadonlyLoadOptionsModel loadOptions) throws IOException
    {
        this(context, viewSet, viewIndex, imageFile,
            geometry == null ? GeometryResources.createNullResources() : geometry.createGraphicsResources(context), true,
            loadOptions);
    }

    /**
     *
     * Creates a calibrated image resource, using pre-loaded geometry resources (which will not be managed by this resource
     * @param context
     * @param viewSet
     * @param viewIndex
     * @param imageFile
     * @param geometryResources
     * @param loadOptions
     * @throws IOException
     */
    private SingleCalibratedImageResource(ContextType context, ReadonlyViewSet viewSet, int viewIndex, File imageFile,
        GeometryResources<ContextType> geometryResources, ReadonlyLoadOptionsModel loadOptions) throws IOException
    {
        this(context, viewSet, viewIndex, imageFile, geometryResources, false, loadOptions);
    }

    private SingleCalibratedImageResource(ContextType context, ReadonlyViewSet viewSet, int viewIndex, File imageFile,
        GeometryResources<ContextType> geometryResources, boolean geometryResourcesOwned, ReadonlyLoadOptionsModel loadOptions) throws IOException
    {
        this.context = context;
        this.viewSet = viewSet;
        this.viewIndex = viewIndex;
        this.geometryResources = geometryResources;
        this.geometryResourcesOwned = geometryResourcesOwned;

        // Read the images from a file
        if (loadOptions.areColorImagesRequested() && imageFile != null && viewIndex < viewSet.getCameraPoseCount())
        {
            var colorTextureBuilder =
                context.getTextureFactory().build2DColorTextureFromFile(imageFile, true);
            loadOptions.configureColorTextureBuilder(colorTextureBuilder);
            colorTexture = colorTextureBuilder.createTexture();
        }
        else
        {
            colorTexture = null;
        }

        if (geometryResources.isNull())
        {
            this.depthTexture = null;
            this.shadowMatrix = null;
        }
        else
        {
            if (loadOptions.areDepthImagesRequested())
            {
                try
                    (
                        // Don't automatically generate any texture attachments for this framebuffer object
                        FramebufferObject<ContextType> depthRenderingFBO =
                            context.buildFramebufferObject(colorTexture.getWidth(), colorTexture.getHeight())
                                .createFramebufferObject();
                        DepthMapGenerator<ContextType> depthMapGenerator = DepthMapGenerator.createFromGeometryResources(geometryResources)
                    )
                {

                    // Build depth texture
                    this.depthTexture = context.getTextureFactory()
                        .build2DDepthTexture(colorTexture.getWidth(), colorTexture.getHeight())
                        .createTexture();

                    depthRenderingFBO.setDepthAttachment(depthTexture);
                    depthMapGenerator.generateDepthMap(viewSet, viewIndex, depthRenderingFBO);

                    // Build shadow texture
                    this.shadowTexture = context.getTextureFactory()
                        .build2DDepthTexture(colorTexture.getWidth(), colorTexture.getHeight())
                        .createTexture();

                    depthRenderingFBO.setDepthAttachment(shadowTexture);
                    this.shadowMatrix = depthMapGenerator.generateShadowMap(viewSet, viewIndex, depthRenderingFBO);
                }
            }
            else
            {
                this.depthTexture = null;
                this.shadowMatrix = null;
            }
        }
    }

    public ProgramBuilder<ContextType> getShaderProgramBuilder()
    {
        return context.getShaderProgramBuilder()
            .define("INFINITE_LIGHT_SOURCE", this.viewSet.areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", this.depthTexture != null)
            .define("SHADOW_TEST_ENABLED", this.shadowTexture != null);
    }

    public static <ContextType extends Context<ContextType>> ProgramBuilder<ContextType> getShaderProgramBuilder(
        ContextType context, ReadonlyViewSet viewSet, ReadonlyLoadOptionsModel loadOptions)
    {
        return context.getShaderProgramBuilder()
            .define("INFINITE_LIGHT_SOURCE", viewSet.areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", viewSet.getGeometryFile() != null && loadOptions.areDepthImagesRequested())
            .define("SHADOW_TEST_ENABLED", viewSet.getGeometryFile() != null && loadOptions.areDepthImagesRequested());
    }

    public void setupShaderProgram(Program<ContextType> program)
    {
        if (this.colorTexture != null)
        {
            program.setTexture("viewImage", colorTexture);
        }

        if (this.depthTexture != null)
        {
            program.setTexture("depthImage", depthTexture);
            program.setUniform("occlusionBias", 0.002f);

            if (this.shadowTexture != null)
            {
                program.setTexture("shadowImage", shadowTexture);
                program.setUniform("shadowMatrix", this.shadowMatrix);
            }
        }

        program.setUniform("cameraPose", viewSet.getCameraPose(viewIndex));
        program.setUniform("cameraProjection", viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex))
            .getProjectionMatrix(viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
        program.setUniform("lightPosition", viewSet.getLightPosition(viewSet.getLightIndex(viewIndex)));
        program.setUniform("lightIntensity", viewSet.getLightIntensity(viewSet.getLightIndex(viewIndex)));
    }

    /**
     * Creates a Drawable using this instance's geometry resources, and the specified shader program.
     * @param program The program to use to construct the Drawable.
     * @return A Drawable for rendering this instance using the specified shader program.
     */
    public Drawable<ContextType> createDrawable(Program<ContextType> program)
    {
        return geometryResources.createDrawable(program);
    }

    @Override
    public void close()
    {
        if (geometryResources != null && geometryResourcesOwned)
        {
            geometryResources.close();
            geometryResources = null;
        }

        if (colorTexture != null)
        {
            colorTexture.close();
            colorTexture = null;
        }

        if (depthTexture != null)
        {
            depthTexture.close();
            depthTexture = null;
        }

        if (shadowTexture != null)
        {
            shadowTexture.close();
            shadowTexture = null;
        }
    }
}
