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
import kintsugi3d.gl.vecmath.Matrix3;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

public class LightWidgetCircles<ContextType extends Context<ContextType>> extends ShaderComponent<ContextType>
{
    private final SceneViewportModel sceneViewportModel;
    private final SceneModel sceneModel;

    private LightWidgetInfo[] widgetInfo;

    public LightWidgetCircles(ContextType context, SceneViewportModel sceneViewportModel, SceneModel sceneModel)
    {
        super(context);
        this.sceneViewportModel = sceneViewportModel;
        this.sceneModel = sceneModel;
    }

    @Override
    protected ProgramObject<ContextType> createProgram(ContextType context) throws IOException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
            .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "scene"), "circle.frag"))
            .createProgram();
    }

    @Override
    protected Map<String, VertexBuffer<ContextType>> createVertexBuffers(ContextType context)
    {
        return Map.of("position", context.createRectangle());
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
            Program<ContextType> circleProgram = getDrawable().program();

            getContext().getState().disableBackFaceCulling();

            getContext().getState().disableDepthWrite();
            getContext().getState().disableDepthTest();
            getContext().getState().setBlendFunction(new BlendFunction(Weight.ONE, Weight.ONE));

            // Draw light widget circles
            for (int i = 0; i < sceneModel.getLightingModel().getLightCount(); i++)
            {
                if (sceneModel.getLightingModel().isLightWidgetEnabled(i))
                {
                    circleProgram.setUniform("color", new Vector3(1));
                    circleProgram.setUniform("projection", cameraViewport.getViewportProjection());
                    circleProgram.setUniform("width", 1 / 128.0f);
                    circleProgram.setUniform("maxAngle", (float) Math.PI / 4);
                    circleProgram.setUniform("threshold", 0.005f);

                    if (sceneModel.getLightingModel().getLightWidgetModel(i).isAzimuthWidgetVisible() &&
                        (Math.abs(widgetInfo[i].lightDisplacementWorld.x) > 0.001f || Math.abs(widgetInfo[i].lightDisplacementWorld.z) > 0.001f))
                    {
                        // Azimuth circle
                        circleProgram.setUniform("maxAngle",
                            (float) (sceneModel.getLightingModel().getLightWidgetModel(i).isAzimuthWidgetSelected() ?
                                Math.PI : widgetInfo[i].azimuthArrowRotation));
                        circleProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i + ".Azimuth"));
                        circleProgram.setUniform("color",
                            new Vector3(sceneModel.getLightingModel().getLightWidgetModel(i).isAzimuthWidgetSelected() ? 1.0f : 0.5f));
                        circleProgram.setUniform("model_view",
                            Matrix4.translate(widgetInfo[i].lightCenter.plus(widgetInfo[i].azimuthRotationAxis.times(widgetInfo[i].widgetDisplacement.dot(widgetInfo[i].azimuthRotationAxis))))
                                .times(cameraViewport.getView().getUpperLeft3x3().asMatrix4())
                                .times(Matrix4.scale(2 * widgetInfo[i].lightDistanceAtInclination))
                                .times(Matrix4.rotateX(-Math.PI / 2))
                                .times(Matrix4.rotateZ(widgetInfo[i].azimuth - Math.PI / 2)));
                        getDrawable().draw(PrimitiveMode.TRIANGLE_FAN, cameraViewport.ofFramebuffer(framebuffer));
                    }

                    if (sceneModel.getLightingModel().getLightWidgetModel(i).isInclinationWidgetVisible())
                    {
                        // Inclination circle
                        circleProgram.setUniform("maxAngle",
                            (float) (sceneModel.getLightingModel().getLightWidgetModel(i).isInclinationWidgetSelected() ?
                                Math.PI / 2 : widgetInfo[i].inclinationArrowRotation));
                        circleProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i + ".Inclination"));
                        circleProgram.setUniform("color",
                            new Vector3(sceneModel.getLightingModel().getLightWidgetModel(i).isInclinationWidgetSelected() ? 1.0f : 0.5f));
                        circleProgram.setUniform("model_view",
                            Matrix4.translate(widgetInfo[i].lightCenter)
                                .times(Matrix4.scale(2 * widgetInfo[i].widgetDistance))
                                .times(widgetInfo[i].widgetTransformation.getUpperLeft3x3()
                                    .times(Matrix3.rotateY(-Math.PI / 2))
                                    .times(sceneModel.getLightingModel().getLightWidgetModel(i).isInclinationWidgetSelected() ?
                                        Matrix3.rotateZ(-widgetInfo[i].inclination) : Matrix3.IDENTITY)
                                    .asMatrix4()));
                        getDrawable().draw(PrimitiveMode.TRIANGLE_FAN, cameraViewport.ofFramebuffer(framebuffer));
                    }
                }
            }

            getContext().getState().disableBlending();
            getContext().getState().enableDepthWrite();
            getContext().getState().enableDepthTest();
        }
    }
}
