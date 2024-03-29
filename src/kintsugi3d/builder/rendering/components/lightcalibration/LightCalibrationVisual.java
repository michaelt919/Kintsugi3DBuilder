/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.rendering.components.lightcalibration;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.snap.ViewSnappable;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.*;
import kintsugi3d.util.RadialTextureGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Class for drawing the 3D light representations and manipulation widgets.
 * @param <ContextType>
 */
public class LightCalibrationVisual<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(LightCalibrationVisual.class);
    private final ContextType context;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;

    private ViewSnappable viewSnappable;

    private ProgramObject<ContextType> lightProgram;
    private VertexBuffer<ContextType> rectangleVertices;
    private Texture2D<ContextType> lightTexture;
    private Drawable<ContextType> lightDrawable;

    public LightCalibrationVisual(ContextType context, SceneModel sceneModel, SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.context = context;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
        sceneViewportModel.addSceneObjectType("LightCalibVisual");
    }

    @Override
    public void initialize()
    {
        this.rectangleVertices = context.createRectangle();

        this.lightTexture = new RadialTextureGenerator<>(context).buildBloomTexture(64)
                .setInternalFormat(ColorFormat.R8)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(true)
                .createTexture();

        try
        {
            this.lightProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "scene"), "light.frag"))
                .createProgram();
            this.lightDrawable = context.createDrawable(this.lightProgram);
            this.lightDrawable.addVertexBuffer("position", rectangleVertices);
        }
        catch (FileNotFoundException|RuntimeException e)
        {
            log.error("Failed to load shader.", e);
        }
    }

    @Override
    public void reloadShaders()
    {
        try
        {
            ProgramObject<ContextType> newLightProgram = context.getShaderProgramBuilder()
                    .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                    .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "scene"), "light.frag"))
                    .createProgram();

            if (this.lightProgram != null)
            {
                this.lightProgram.close();
            }

            this.lightProgram = newLightProgram;
            this.lightDrawable = context.createDrawable(this.lightProgram);
            this.lightDrawable.addVertexBuffer("position", rectangleVertices);
        }
        catch (FileNotFoundException|RuntimeException e)
        {
            log.error("Failed to load shader.", e);
        }
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        FramebufferSize size = framebuffer.getSize();

        this.context.getState().disableDepthWrite();

        this.context.getState().setBlendFunction(new BlendFunction(BlendFunction.Weight.ONE, BlendFunction.Weight.ONE));
        this.context.getState().enableDepthTest();

        this.context.getState().setBlendFunction(new BlendFunction(BlendFunction.Weight.ONE, BlendFunction.Weight.ONE));
        this.lightProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("LightCalibVisual"));
        this.lightProgram.setUniform("color", new Vector3((float)Math.PI));

        // Calculate world space light position.
        Matrix4 snapView = viewSnappable.getSnapView();
        int primaryLightIndex = viewSnappable.getViewSet().getLightIndex(viewSnappable.getViewSet().getPrimaryViewIndex());
        Vector3 lightPosition = sceneModel.getSettingsModel().get("currentLightCalibration", Vector2.class).asVector3()
            .plus(viewSnappable.getViewSet().getLightPosition(primaryLightIndex));
        Matrix4 lightTransform = Matrix4.translate(lightPosition.negated());
        Matrix4 lightView = lightTransform.times(snapView);
        Vector3 lightPosWorldSpace = lightView.getUpperLeft3x3().transpose().times(lightView.getColumn(3).getXYZ().negated());
        Vector3 lightPosCamSpace = cameraViewport.getView().times(lightPosWorldSpace.asPosition()).getXYZ();

        this.lightProgram.setUniform("model_view", Matrix4.translate(lightPosCamSpace)
                .times(Matrix4.scale(-lightPosCamSpace.z / 32.0f, -lightPosCamSpace.z / 32.0f, 1.0f)));
        this.lightProgram.setUniform("projection", cameraViewport.getViewportProjection());
        this.lightProgram.setTexture("lightTexture", this.lightTexture);
        this.lightDrawable.draw(PrimitiveMode.TRIANGLE_FAN, cameraViewport.ofFramebuffer(framebuffer));

        context.getState().disableBlending();
        context.getState().enableDepthWrite();
        this.context.getState().enableDepthTest();
    }

    @Override
    public void close()
    {
        if (rectangleVertices != null)
        {
            rectangleVertices.close();
            rectangleVertices = null;
        }

        if (lightProgram != null)
        {
            lightProgram.close();
            lightProgram = null;
        }

        if (lightDrawable != null)
        {
            lightDrawable.close();
            lightDrawable = null;
        }

        if (lightTexture != null)
        {
            lightTexture.close();
            lightTexture = null;
        }
    }

    public ViewSnappable getViewSnappable()
    {
        return viewSnappable;
    }

    public void setViewSnappable(ViewSnappable viewSnappable)
    {
        this.viewSnappable = viewSnappable;
    }
}
