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

package kintsugi3d.builder.rendering.components.lit;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;
import kintsugi3d.gl.core.VertexBuffer;

public class LitRoot<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final ContextType context;
    private LightingResources<ContextType> lightingResources;
    private LitContent<ContextType> litContentRoot;

    public LitRoot(ContextType context, SceneModel sceneModel)
    {
        this.context = context;
        this.lightingResources = new LightingResources<>(context, sceneModel);
    }

    public LightingResources<ContextType> getLightingResources()
    {
        return lightingResources;
    }

    @Override
    public void initialize()
    {
        // i.e. shadow map, environment map, etc.
        lightingResources.initialize();
        litContentRoot.initialize();
    }

    @Override
    public void reloadShaders()
    {
        lightingResources.reloadShadowShader();
        litContentRoot.reloadShaders();
    }

    @Override
    public void update()
    {
        litContentRoot.update();
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        setupForDraw(cameraViewport);
        litContentRoot.draw(framebuffer, cameraViewport);
    }

    @Override
    public void drawInSubdivisions(FramebufferObject<ContextType> framebuffer, int subdivWidth, int subdivHeight,
                                   CameraViewport cameraViewport)
    {
        setupForDraw(cameraViewport);
        litContentRoot.drawInSubdivisions(framebuffer, subdivWidth, subdivHeight, cameraViewport);
    }

    private void setupForDraw(CameraViewport cameraViewport)
    {
        lightingResources.refreshShadowMaps();

        // Screen space depth buffer for specular shadows
        lightingResources.refreshScreenSpaceDepthFBO(cameraViewport.getView(), cameraViewport.getFullProjection());

        context.flush();
    }

    @Override
    public void close()
    {
        if (lightingResources != null)
        {
            lightingResources.close();
            lightingResources = null;
        }

        if (litContentRoot != null)
        {
            litContentRoot.close();
            litContentRoot = null;
        }
    }

    public RenderedComponent<ContextType> getLitContentRoot()
    {
        return litContentRoot;
    }

    /**
     * Transfers ownership of the lit content to this object
     * Must be called before initialize
     * @param litContentRoot
     */
    public void takeLitContentRoot(LitContent<ContextType> litContentRoot)
    {
        this.litContentRoot = litContentRoot;
        litContentRoot.initWithLightingResources(this.lightingResources);
    }

    /**
     * Must be called after initialize
     * @param shadowCaster
     */
    public void setShadowCaster(VertexBuffer<ContextType> shadowCaster)
    {
        lightingResources.setShadowCastingPositionBuffer(shadowCaster);
    }
}
