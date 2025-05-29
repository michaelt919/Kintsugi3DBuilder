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

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.core.BlendFunction.Weight;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;

public class LightWidgetLines<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final ContextType context;
    private Program<ContextType> solidProgram;
    private final SceneViewportModel sceneViewportModel;
    private final SceneModel sceneModel;

    private LightWidgetInfo[] widgetInfo;

    public LightWidgetLines(ContextType context, SceneViewportModel sceneViewportModel, SceneModel sceneModel)
    {
        this.context = context;
        this.sceneViewportModel = sceneViewportModel;
        this.sceneModel = sceneModel;
    }

    @Override
    public void initialize()
    {
    }

    @Override
    public void reloadShaders()
    {
    }

    public void refreshWidgetInfo(LightWidgetInfo[] widgetInfo)
    {
        this.widgetInfo = widgetInfo;
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (this.sceneModel.getSettingsModel().getBoolean("relightingEnabled")
            && this.sceneModel.getSettingsModel().getBoolean("visibleLightsEnabled")
            && sceneModel.getSettingsModel().getBoolean("lightWidgetsEnabled"))
        {
            FramebufferSize size = framebuffer.getSize();

            context.getState().disableDepthWrite();
            context.getState().disableDepthTest();
            context.getState().setBlendFunction(new BlendFunction(Weight.ONE, Weight.ONE));

            solidProgram.setUniform("projection", cameraViewport.getViewportProjection());

            // Draw light widget lines
            for (int i = 0; i < sceneModel.getLightingModel().getLightCount(); i++)
            {
                Matrix4 widgetTransformation = cameraViewport.getView().times(sceneModel.getInverseLightViewMatrix(i));

                if (sceneModel.getLightingModel().isLightWidgetEnabled(i))
                {
                    if (sceneModel.getLightingModel().getLightWidgetModel(i).isDistanceWidgetVisible() || sceneModel.getLightingModel().getLightWidgetModel(i).isCenterWidgetVisible())
                    {
                        Vector3 lineEndpoint = widgetInfo[i].widgetPosition.minus(widgetInfo[i].lightCenter)
                            .times(0.5f / widgetInfo[i].widgetPosition.getXY().distance(widgetInfo[i].lightCenter.getXY()))
                            .minus(widgetInfo[i].lightCenter);

                        try
                            (
                                VertexBuffer<ContextType> line =
                                    context.createVertexBuffer()
                                        .setData(NativeVectorBufferFactory.getInstance()
                                            .createFromFloatArray(3, 2,
                                                lineEndpoint.x, lineEndpoint.y, lineEndpoint.z,
                                                widgetInfo[i].lightCenter.x, widgetInfo[i].lightCenter.y,widgetInfo[i]. lightCenter.z));
                                Drawable<ContextType> lineRenderable = context.createDrawable(solidProgram)
                            )
                        {
                            lineRenderable.addVertexBuffer("position", line);
                            solidProgram.setUniform("model_view", Matrix4.IDENTITY);
                            solidProgram.setUniform("color",
                                new Vector3(sceneModel.getLightingModel().getLightWidgetModel(i).isDistanceWidgetSelected()
                                    || sceneModel.getLightingModel().getLightWidgetModel(i).isCenterWidgetSelected() ? 1.0f : 0.5f)
                                    .asVector4(1));
                            solidProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i + ".Distance"));
                            lineRenderable.draw(PrimitiveMode.LINES, cameraViewport.ofFramebuffer(framebuffer));
                        }
                    }

                    if (sceneModel.getLightingModel().getLightWidgetModel(i).isInclinationWidgetVisible()
                        && sceneModel.getLightingModel().getLightWidgetModel(i).isInclinationWidgetSelected())
                    {
                        Vector3 lineEndpoint1 = widgetInfo[i].lightCenter
                            .plus(cameraViewport.getView().times(new Vector4(0, widgetInfo[i].widgetDistance, 0, 0)).getXYZ());
                        Vector3 lineEndpoint2 = widgetInfo[i].lightCenter
                            .plus(cameraViewport.getView().times(new Vector4(0, -widgetInfo[i].widgetDistance, 0, 0)).getXYZ());

                        try
                            (
                                VertexBuffer<ContextType> line =
                                    context.createVertexBuffer()
                                        .setData(NativeVectorBufferFactory.getInstance()
                                            .createFromFloatArray(3, 2, lineEndpoint1.x, lineEndpoint1.y, lineEndpoint1.z, lineEndpoint2.x, lineEndpoint2.y, lineEndpoint2.z));
                                Drawable<ContextType> lineRenderable = context.createDrawable(solidProgram)
                            )
                        {
                            lineRenderable.addVertexBuffer("position", line);
                            solidProgram.setUniform("model_view", Matrix4.IDENTITY);
                            solidProgram.setUniform("color",
                                new Vector3(sceneModel.getLightingModel().getLightWidgetModel(i).isDistanceWidgetSelected()
                                    || sceneModel.getLightingModel().getLightWidgetModel(i).isCenterWidgetSelected() ? 1.0f : 0.5f)
                                    .asVector4(1));
                            solidProgram.setUniform("objectID", 0);
                            lineRenderable.draw(PrimitiveMode.LINES, cameraViewport.ofFramebuffer(framebuffer));
                        }
                    }
                }
            }

            context.getState().disableBlending();
            context.getState().enableDepthWrite();
            context.getState().enableDepthTest();
        }
    }

    @Override
    public void close()
    {
    }

    public void setSolidProgram(Program<ContextType> solidProgram)
    {
        this.solidProgram = solidProgram;
    }
}
