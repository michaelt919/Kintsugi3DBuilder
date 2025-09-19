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

package kintsugi3d.builder.rendering.components;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.lit.LitContent;
import kintsugi3d.builder.rendering.components.scene.Backplate;
import kintsugi3d.builder.rendering.components.scene.Environment;
import kintsugi3d.builder.rendering.components.scene.Grid;
import kintsugi3d.builder.rendering.components.scene.GroundPlane;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;
import kintsugi3d.gl.vecmath.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class BaseScene<ContextType extends Context<ContextType>> extends LitContent<ContextType>
{
    private static final Logger LOG = LoggerFactory.getLogger(BaseScene.class);

    protected final ContextType context;
    protected final SceneModel sceneModel;
    protected final SceneViewportModel sceneViewportModel;
    protected final List<RenderedComponent<ContextType>> components = new ArrayList<>();
    protected final GraphicsResourcesImageSpace<ContextType> resources;
    private RenderingSubject<ContextType> renderingSubject;

    public BaseScene(GraphicsResourcesImageSpace<ContextType> resources, SceneModel sceneModel, SceneViewportModel sceneViewportModel)
    {
        this.context = resources.getContext();
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
        this.resources = resources;
    }

    @Override
    protected void addLitComponents(LightingResources<ContextType> lightingResources)
    {
        // Backplate and environment must be first since they aren't depth tested.
        components.add(new Backplate<>(context, lightingResources, sceneModel));
        components.add(new Environment<>(context, sceneViewportModel, lightingResources, sceneModel));

        // Foreground components that will be depth tested
        components.add(new Grid<>(context, sceneModel));
        components.add(new GroundPlane<>(resources, lightingResources, sceneModel, sceneViewportModel));

        // the actual subject for image-based rendering
        // Draw after "other components", which includes things that ignore the depth test first (environment or backplate)
        renderingSubject = new RenderingSubject<>(resources, sceneViewportModel, sceneModel, lightingResources);
        components.add(renderingSubject);
    }

    @Override
    public void initialize()
    {
        // Run initialization for each additional component
        for (RenderedComponent<ContextType> component : components)
        {
            component.initialize();
        }
    }

    public RenderingSubject<ContextType> getSubject()
    {
        return renderingSubject;
    }

    @Override
    public void reloadShaders()
    {
        for (RenderedComponent<ContextType> component : components)
        {
            component.reloadShaders();
        }
    }

    @Override
    public void update()
    {
        for (RenderedComponent<ContextType> component : components)
        {
            component.update();
        }
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (renderingSubject.getProgram() != null)
        {
            // Hole fill color depends on whether in light calibration mode or not.
            renderingSubject.getProgram().setUniform("holeFillColor", new Vector3(0.0f));

            // Draw each component
            components.forEach(component -> component.draw(framebuffer, cameraViewport));

            // Finish drawing
            context.flush();

            if (!sceneModel.getLightingModel().areLightWidgetsEthereal()
                && IntStream.range(0, sceneModel.getLightingModel().getLightCount()).anyMatch(sceneModel.getLightingModel()::isLightWidgetEnabled))
            {
                // Read buffers here if light widgets are not ethereal (i.e. they can be clicked and should be in the ID buffer)
                sceneViewportModel.refreshBuffers(cameraViewport.getFullProjection(), framebuffer);
            }
        }
    }

    @Override
    public void close()
    {
        if (renderingSubject != null)
        {
            renderingSubject.close();
        }

        for (RenderedComponent<ContextType> otherComponent : components)
        {
            try
            {
                otherComponent.close();
            }
            catch (Exception e)
            {
                LOG.error("Error occurred while closing scene:", e);
            }
        }

        components.clear();
    }
}
