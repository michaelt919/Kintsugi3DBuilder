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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.ShaderComponent;
import kintsugi3d.builder.rendering.components.snap.ViewSelection;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.core.BlendFunction.Weight;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.RadialTextureGenerator;

/**
 * Class for drawing the 3D light representations and manipulation widgets.
 * @param <ContextType>
 */
public class LightCalibrationVisual<ContextType extends Context<ContextType>> extends ShaderComponent<ContextType>
{
    private final SceneModel sceneModel;

    private ViewSelection viewSelection;
    private Texture2D<ContextType> lightTexture;

    public LightCalibrationVisual(ContextType context, SceneViewportModel sceneViewportModel, SceneModel sceneModel)
    {
        super(context, sceneViewportModel, "LightCalibVisual");
        this.sceneModel = sceneModel;
    }

    @Override
    public void initialize()
    {
        super.initialize();

        this.lightTexture = resource(new RadialTextureGenerator<>(getContext()).buildBloomTexture(64)
            .setInternalFormat(ColorFormat.R8)
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
        FramebufferSize size = framebuffer.getSize();

        this.getContext().getState().disableDepthWrite();

        this.getContext().getState().setBlendFunction(new BlendFunction(Weight.ONE, Weight.ONE));
        this.getContext().getState().enableDepthTest();

        this.getContext().getState().setBlendFunction(new BlendFunction(Weight.ONE, Weight.ONE));
        this.getDrawable().program().setUniform("color", new Vector3((float)Math.PI));

        // Calculate world space light position.
        Matrix4 snapView = viewSelection.getSelectedView();
        int primaryLightIndex = viewSelection.getViewSet().getLightIndex(viewSelection.getViewSet().getPrimaryViewIndex());
        Vector3 lightPosition = sceneModel.getSettingsModel().get("currentLightCalibration", Vector2.class).asVector3()
            .plus(viewSelection.getViewSet().getLightPosition(primaryLightIndex));
        Matrix4 lightTransform = Matrix4.translate(lightPosition.negated());
        Matrix4 lightView = lightTransform.times(snapView);
        Vector3 lightPosWorldSpace = lightView.getUpperLeft3x3().transpose().times(lightView.getColumn(3).getXYZ().negated());
        Vector3 lightPosCamSpace = cameraViewport.getView().times(lightPosWorldSpace.asPosition()).getXYZ();

        this.getDrawable().program().setUniform("model_view", Matrix4.translate(lightPosCamSpace)
                .times(Matrix4.scale(-lightPosCamSpace.z / 32.0f, -lightPosCamSpace.z / 32.0f, 1.0f)));
        this.getDrawable().program().setUniform("projection", cameraViewport.getViewportProjection());
        this.getDrawable().program().setTexture("grayscaleTex", this.lightTexture);
        this.getDrawable().draw(PrimitiveMode.TRIANGLE_FAN, cameraViewport.ofFramebuffer(framebuffer));

        this.getContext().getState().disableBlending();
        this.getContext().getState().enableDepthWrite();
        this.getContext().getState().enableDepthTest();
    }

    public ViewSelection getViewSelection()
    {
        return viewSelection;
    }

    public void setViewSelection(ViewSelection viewSelection)
    {
        this.viewSelection = viewSelection;
    }
}
