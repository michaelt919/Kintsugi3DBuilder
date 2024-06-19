/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.rendering.components.scene;

import java.io.File;
import java.util.Map;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.StandardShaderComponent;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroundPlane<ContextType extends Context<ContextType>> extends StandardShaderComponent<ContextType>
{
    private static final Logger LOG = LoggerFactory.getLogger(GroundPlane.class);

    public GroundPlane(IBRResourcesImageSpace<ContextType> resources, LightingResources<ContextType> lightingResources,
                       SceneModel sceneModel, SceneViewportModel sceneViewportModel)
    {
        super(resources, sceneViewportModel, "SceneObject", sceneModel, lightingResources,
            new File(new File("shaders", "rendermodes"), "simpleSpecular.frag"));
    }

    @Override
    protected Map<String, VertexBuffer<ContextType>> createVertexBuffers(ContextType context)
    {
        return Map.of("position", context.createRectangle());
    }

    @Override
    protected Drawable<ContextType> createDrawable(Program<ContextType> program)
    {
        Drawable<ContextType> drawable = super.createDrawable(program);
        drawable.setVertexAttrib("normal", new Vector3(0, 0, 1));
        return drawable;
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (sceneModel.getLightingModel().isGroundPlaneEnabled())
        {
            Matrix4 model = sceneModel.getUnscaledMatrix(Matrix4.translate(new Vector3(0, sceneModel.getLightingModel().getGroundPlaneHeight(), 0)))
                .times(Matrix4.rotateX(Math.PI / 2))
                .times(Matrix4.scale(sceneModel.getScale() * sceneModel.getLightingModel().getGroundPlaneSize()));

            setupShader(cameraViewport, model);

            getDrawable().program().setUniform("defaultDiffuseColor",
                sceneModel.getLightingModel().getGroundPlaneColor().applyOperator(x -> Math.pow(x, 2.2)));

            // Disable back face culling since the plane is one-sided.
            getContext().getState().disableBackFaceCulling();

            getDrawable().draw(PrimitiveMode.TRIANGLE_FAN, cameraViewport.ofFramebuffer(framebuffer));

            // Re-enable back face culling
            getContext().getState().enableBackFaceCulling();
        }
    }
}
