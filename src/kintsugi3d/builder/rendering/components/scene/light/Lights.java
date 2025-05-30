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

package kintsugi3d.builder.rendering.components.scene.light;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.ShaderComponent;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.core.BlendFunction.Weight;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.RadialTextureGenerator;

public class Lights<ContextType extends Context<ContextType>> extends ShaderComponent<ContextType>
{
    private final SceneViewportModel sceneViewportModel;
    private final SceneModel sceneModel;

    private Texture2D<ContextType> lightTexture;

    public Lights(ContextType context, SceneViewportModel sceneViewportModel, SceneModel sceneModel)
    {
        super(context);
        this.sceneViewportModel = sceneViewportModel;
        this.sceneModel = sceneModel;

        for (int i = 0; i < sceneModel.getLightingModel().getMaxLightCount(); i++)
        {
            sceneViewportModel.addSceneObjectType("Light." + i);
        }
    }

    @Override
    public void initialize()
    {
        super.initialize();

        this.lightTexture = resource(new RadialTextureGenerator<>(getContext())
            .buildBloomTexture(64)
            .setLinearFilteringEnabled(true)
            .setMipmapsEnabled(true)
            .createTexture());
    }

    @Override
    protected ProgramObject<ContextType> createProgram(ContextType context) throws IOException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
            .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "scene"), "grayscaleTexture.frag"))
            .createProgram();
    }

    @Override
    protected Map<String, VertexBuffer<ContextType>> createVertexBuffers(ContextType context)
    {
        return Map.of("position", context.createRectangle());
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (this.sceneModel.getSettingsModel().getBoolean("relightingEnabled")
            && this.sceneModel.getSettingsModel().getBoolean("visibleLightsEnabled"))
        {
            getContext().getState().disableDepthWrite();
            getContext().getState().enableDepthTest();
            getContext().getState().setBlendFunction(new BlendFunction(Weight.ONE, Weight.ONE));

            this.getDrawable().program().setUniform("projection", cameraViewport.getViewportProjection());
            this.getDrawable().program().setTexture("grayscaleTex", this.lightTexture);

            // Draw lights
            for (int i = 0; i < sceneModel.getLightingModel().getLightCount(); i++)
            {
                if (sceneModel.getLightingModel().isLightVisualizationEnabled(i))
                {
                    Matrix4 widgetTransformation = cameraViewport.getView().times(sceneModel.getInverseLightViewMatrix(i));

                    getContext().getState().setBlendFunction(new BlendFunction(Weight.ONE, Weight.ONE));
                    this.getDrawable().program().setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i));
                    this.getDrawable().program().setUniform("color", sceneModel.getLightingModel().getLightPrototype(i).getColor().times((float) Math.PI));

                    Vector3 lightPosition = widgetTransformation.getColumn(3).getXYZ();

                    this.getDrawable().program().setUniform("model_view",
                        Matrix4.translate(lightPosition)
                            .times(Matrix4.scale(-lightPosition.z / 32.0f, -lightPosition.z / 32.0f, 1.0f)));
                    this.getDrawable().draw(PrimitiveMode.TRIANGLE_FAN, cameraViewport.ofFramebuffer(framebuffer));
                }
            }

            getContext().getState().disableBlending();
            getContext().getState().enableDepthWrite();
            getContext().getState().enableDepthTest();
        }
    }
}
