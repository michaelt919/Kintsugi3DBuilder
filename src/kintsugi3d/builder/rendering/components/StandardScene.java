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

package kintsugi3d.builder.rendering.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.resources.IBRResourcesImageSpace;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.lit.LitContent;
import kintsugi3d.builder.rendering.components.scene.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class StandardScene<ContextType extends Context<ContextType>> extends LitContent<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(StandardScene.class);
    private final ContextType context;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;
    private final IBRResourcesImageSpace<ContextType> resources;

    private IBRSubject<ContextType> ibrSubject;
    private LightVisuals<ContextType> lightVisuals;
    private final List<RenderedComponent<ContextType>> otherComponents = new ArrayList<>();

    public StandardScene(IBRResourcesImageSpace<ContextType> resources, SceneModel sceneModel, SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.context = resources.getContext();
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
        this.resources = resources;
    }

    @Override
    public void initialize() throws Exception
    {
        LightingResources<ContextType> lightingResources = getLightingResources();

        // the actual subject for image-based rendering
        ibrSubject = new IBRSubject<>(resources, lightingResources, sceneModel, sceneViewportModel);
        ibrSubject.initialize();

        // graphics resources for depicting the on-screen representation of lights
        lightVisuals = new LightVisuals<>(context, sceneModel, sceneViewportModel);
        lightVisuals.initialize();

        // Backplate and environment must be first since they aren't depth tested.
        otherComponents.add(new Backplate<>(context, lightingResources, sceneModel));
        otherComponents.add(new Environment<>(context, lightingResources, sceneModel, sceneViewportModel));

        // Foreground components that will be depth tested
        otherComponents.add(new Grid<>(context, sceneModel));
        otherComponents.add(new GroundPlane<>(resources, lightingResources, sceneModel, sceneViewportModel));

        // Run initialization for each additional component
        for (RenderedComponent<ContextType> component : otherComponents)
        {
            component.initialize();
        }
    }

    @Override
    public void reloadShaders() throws Exception
    {
        ibrSubject.reloadShaders();
        lightVisuals.reloadShaders();

        for (RenderedComponent<ContextType> otherComponent : otherComponents)
        {
            otherComponent.reloadShaders();
        }
    }

    @Override
    public void update() throws Exception
    {
        ibrSubject.update();
        lightVisuals.update();

        for (RenderedComponent<ContextType> component : otherComponents)
        {
            component.update();
        }
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        drawInSubdivisions(framebuffer, cameraViewport.getWidth(), cameraViewport.getHeight(), cameraViewport);
    }

    @Override
    public void drawInSubdivisions(FramebufferObject<ContextType> framebuffer, int subdivWidth, int subdivHeight, CameraViewport cameraViewport)
    {
        // Draw "other components" first, which includes things that ignore the depth test first
        // (environment or backplate)
        otherComponents.forEach(component -> component.draw(framebuffer, cameraViewport));

        // Hole fill color depends on whether in light calibration mode or not.
        ibrSubject.getProgram().setUniform("holeFillColor", new Vector3(0.0f));

        // Draw the actual object with the model transformation
        ibrSubject.drawInSubdivisions(framebuffer, subdivWidth, subdivHeight, cameraViewport);

        if (!sceneModel.getLightingModel().areLightWidgetsEthereal()
                && IntStream.range(0, sceneModel.getLightingModel().getLightCount()).anyMatch(sceneModel.getLightingModel()::isLightWidgetEnabled))
        {
            context.flush();

            // Read buffers here if light widgets are ethereal (i.e. they cannot be clicked and should not be in the ID buffer)
            sceneViewportModel.refreshBuffers(cameraViewport.getFullProjection(), framebuffer);
        }

        lightVisuals.draw(framebuffer, cameraViewport);

        // Finish drawing
        context.flush();

        if (!sceneModel.getLightingModel().areLightWidgetsEthereal()
                && IntStream.range(0, sceneModel.getLightingModel().getLightCount()).anyMatch(sceneModel.getLightingModel()::isLightWidgetEnabled))
        {
            // Read buffers here if light widgets are not ethereal (i.e. they can be clicked and should be in the ID buffer)
            sceneViewportModel.refreshBuffers(cameraViewport.getFullProjection(), framebuffer);
        }
    }

    @Override
    public void close()
    {
        if (ibrSubject != null)
        {
            ibrSubject.close();
        }

        if (lightVisuals != null)
        {
            lightVisuals.close();
        }

        for (RenderedComponent<ContextType> otherComponent : otherComponents)
        {
            try
            {
                otherComponent.close();
            }
            catch (Exception e)
            {
                log.error("Error occurred while closing scene:", e);
            }
        }
    }
}
