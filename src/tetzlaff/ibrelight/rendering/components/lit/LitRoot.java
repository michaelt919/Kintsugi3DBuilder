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

package tetzlaff.ibrelight.rendering.components.lit;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.FramebufferObject;
import tetzlaff.gl.core.VertexBuffer;
import tetzlaff.ibrelight.core.CameraViewport;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.rendering.resources.LightingResources;

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
    public void initialize() throws Exception
    {
        // i.e. shadow map, environment map, etc.
        lightingResources.initialize();
        litContentRoot.initialize();
    }

    @Override
    public void reloadShaders() throws Exception
    {
        // TODO reload shadow program?

        litContentRoot.reloadShaders();
    }

    @Override
    public void update() throws Exception
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
    public void close() throws Exception
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
        litContentRoot.setLightingResources(this.lightingResources);
    }

    /**
     * Must be called after initialize
     * @param shadowCaster
     */
    public void setShadowCaster(VertexBuffer<ContextType> shadowCaster)
    {
        lightingResources.setPositionBuffer(shadowCaster);
    }
}
