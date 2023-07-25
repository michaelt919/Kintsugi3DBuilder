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

package tetzlaff.ibrelight.rendering.components.scene;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.CameraViewport;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.core.StandardRenderingMode;
import tetzlaff.ibrelight.rendering.resources.IBRResourcesImageSpace;
import tetzlaff.ibrelight.rendering.resources.LightingResources;
import tetzlaff.ibrelight.rendering.SceneViewportModel;
import tetzlaff.ibrelight.rendering.StandardShader;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class GroundPlane<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(GroundPlane.class);
    private final ContextType context;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;
    private StandardShader<ContextType> groundPlaneStandardShader;

    private VertexBuffer<ContextType> rectangleVertices;
    private Drawable<ContextType> groundPlaneDrawable;

    public GroundPlane(IBRResourcesImageSpace<ContextType> resources, LightingResources<ContextType> lightingResources,
                       SceneModel sceneModel, SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.context = resources.getContext();
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
        this.groundPlaneStandardShader = new StandardShader<>(resources, lightingResources, sceneModel);
    }

    @Override
    public void initialize() throws FileNotFoundException
    {
        this.rectangleVertices = context.createRectangle();

        groundPlaneStandardShader.initialize(StandardRenderingMode.LAMBERTIAN_SHADED);
        groundPlaneDrawable = context.createDrawable(groundPlaneStandardShader.getProgram());
        groundPlaneDrawable.addVertexBuffer("position", rectangleVertices);
        groundPlaneDrawable.setVertexAttrib("normal", new Vector3(0, 0, 1));
    }

    @Override
    public void update() throws FileNotFoundException
    {
        Map<String, Optional<Object>> defineMap = groundPlaneStandardShader.getPreprocessorDefines();

        // Reloads shaders only if compiled settings have changed.
        if (defineMap.entrySet().stream().anyMatch(
                defineEntry -> !Objects.equals(groundPlaneDrawable.program().getDefine(defineEntry.getKey()), defineEntry.getValue())))
        {
            log.info("Updating compiled render settings.");
            reloadShaders();
        }
    }

    @Override
    public void reloadShaders() throws FileNotFoundException
    {
        groundPlaneStandardShader.reload(StandardRenderingMode.LAMBERTIAN_SHADED);
        groundPlaneDrawable = context.createDrawable(groundPlaneStandardShader.getProgram());
        groundPlaneDrawable.addVertexBuffer("position", rectangleVertices);
        groundPlaneDrawable.setVertexAttrib("normal", new Vector3(0, 0, 1));
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (sceneModel.getLightingModel().isGroundPlaneEnabled())
        {
            Matrix4 model = sceneModel.getUnscaledMatrix(Matrix4.translate(new Vector3(0, sceneModel.getLightingModel().getGroundPlaneHeight(), 0)))
                .times(Matrix4.rotateX(Math.PI / 2))
                .times(Matrix4.scale(sceneModel.getScale() * sceneModel.getLightingModel().getGroundPlaneSize()));

            groundPlaneStandardShader.setup(model);

            groundPlaneDrawable.program().setUniform("objectID", sceneViewportModel.lookupSceneObjectID("SceneObject"));
            groundPlaneDrawable.program().setUniform("defaultDiffuseColor",
                sceneModel.getLightingModel().getGroundPlaneColor().applyOperator(x -> Math.pow(x, 2.2)));
            groundPlaneDrawable.program().setUniform("projection", cameraViewport.getViewportProjection());
            groundPlaneDrawable.program().setUniform("fullProjection", cameraViewport.getFullProjection());

            // Set up camera for ground plane program.
            Matrix4 modelView = cameraViewport.getView().times(model);
            groundPlaneDrawable.program().setUniform("model_view", modelView);
            groundPlaneDrawable.program().setUniform("viewPos", modelView.quickInverse(0.01f).getColumn(3).getXYZ());

            // Disable back face culling since the plane is one-sided.
            context.getState().disableBackFaceCulling();

            // Do first pass at half resolution to off-screen buffer
            groundPlaneDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());

            // Re-enable back face culling
            context.getState().enableBackFaceCulling();
        }
    }

    @Override
    public void close()
    {
        if (rectangleVertices != null)
        {
            rectangleVertices.close();
            rectangleVertices = null;
        }

        if (groundPlaneStandardShader != null)
        {
            groundPlaneStandardShader.close();
            groundPlaneStandardShader = null;
        }
    }
}
