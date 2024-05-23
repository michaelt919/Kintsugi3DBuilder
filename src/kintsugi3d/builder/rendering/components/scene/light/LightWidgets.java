/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
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
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.Matrix3;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;

public class LightWidgets<ContextType extends Context<ContextType>> extends ShaderComponent<ContextType>
{
    private final SceneViewportModel sceneViewportModel;
    private final SceneModel sceneModel;

    private LightWidgetInfo[] widgetInfo;

    public LightWidgets(ContextType context, SceneViewportModel sceneViewportModel, SceneModel sceneModel)
    {
        super(context);
        this.sceneViewportModel = sceneViewportModel;
        this.sceneModel = sceneModel;

        for (int i = 0; i < sceneModel.getLightingModel().getMaxLightCount(); i++)
        {
            sceneViewportModel.addSceneObjectType("Light." + i + ".Azimuth");
            sceneViewportModel.addSceneObjectType("Light." + i + ".Inclination");
            sceneViewportModel.addSceneObjectType("Light." + i + ".Distance");
        }
    }

    @Override
    protected ProgramObject<ContextType> createProgram(ContextType context) throws IOException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
            .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "solid.frag"))
            .createProgram();
    }

    @Override
    protected Map<String, VertexBuffer<ContextType>> createVertexBuffers(ContextType context)
    {
        return Map.of("position",
            context.createVertexBuffer()
                .setData(NativeVectorBufferFactory.getInstance()
                    .createFromFloatArray(3, 3, -1, -1, 0, 1, -1, 0, 0, 1, 0)));
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

            getContext().getState().disableDepthWrite();
            getContext().getState().disableDepthTest();
            getContext().getState().setBlendFunction(new BlendFunction(Weight.ONE, Weight.ONE));

            Program<ContextType> program = getDrawable().program();
            program.setUniform("projection", cameraViewport.getViewportProjection());

            // Draw light
            for (int i = 0; i < sceneModel.getLightingModel().getLightCount(); i++)
            {
                Matrix4 widgetTransformation = cameraViewport.getView().times(sceneModel.getInverseLightViewMatrix(i));

                if (sceneModel.getLightingModel().isLightWidgetEnabled(i))
                {
                    getContext().getState().enableBackFaceCulling();

                    Vector3 arrow1PositionR = Matrix3.rotateAxis(widgetInfo[i].azimuthRotationAxis, widgetInfo[i].azimuthArrowRotation)
                        .times(widgetInfo[i].widgetPosition.minus(widgetInfo[i].lightCenter))
                        .plus(widgetInfo[i].lightCenter);

                    Vector3 arrow1PositionL = Matrix3.rotateAxis(widgetInfo[i].azimuthRotationAxis, -widgetInfo[i].azimuthArrowRotation)
                        .times(widgetInfo[i].widgetPosition.minus(widgetInfo[i].lightCenter))
                        .plus(widgetInfo[i].lightCenter);

                    Vector3 arrow2PositionR = widgetInfo[i].widgetTransformation.getUpperLeft3x3()
                        .times(Matrix3.rotateX(-widgetInfo[i].inclinationArrowRotation))
                        .times(widgetInfo[i].widgetTransformation.quickInverse(0.01f).getUpperLeft3x3())
                        .times(widgetInfo[i].widgetPosition.minus(widgetInfo[i].lightCenter))
                        .plus(widgetInfo[i].lightCenter);

                    Vector3 arrow2PositionL = widgetInfo[i].widgetTransformation.getUpperLeft3x3()
                        .times(Matrix3.rotateX(widgetInfo[i].inclinationArrowRotation))
                        .times(widgetInfo[i].widgetTransformation.quickInverse(0.01f).getUpperLeft3x3())
                        .times(widgetInfo[i].widgetPosition.minus(widgetInfo[i].lightCenter))
                        .plus(widgetInfo[i].lightCenter);

                    Vector4 arrow1RDirectionY =  Matrix3.rotateAxis(widgetInfo[i].azimuthRotationAxis, widgetInfo[i].azimuthArrowRotation)
                        .times(widgetInfo[i].widgetTransformation.getUpperLeft3x3())
                        .times(new Vector3(1,0,0))
                        .getXY().normalized().asDirection();

                    Vector4 arrow1LDirectionY =  Matrix3.rotateAxis(widgetInfo[i].azimuthRotationAxis, -widgetInfo[i].azimuthArrowRotation)
                        .times(widgetInfo[i].widgetTransformation.getUpperLeft3x3())
                        .times(new Vector3(1,0,0))
                        .getXY().normalized().asDirection();

                    Vector4 arrow2RDirectionY = widgetInfo[i].widgetTransformation.getUpperLeft3x3()
                        .times(Matrix3.rotateX(-widgetInfo[i].inclinationArrowRotation))
                        .times(new Vector3(0,1,0))
                        .getXY().normalized().asDirection();

                    Vector4 arrow2LDirectionY = widgetInfo[i].widgetTransformation.getUpperLeft3x3()
                        .times(Matrix3.rotateX(widgetInfo[i].inclinationArrowRotation))
                        .times(new Vector3(0,1,0))
                        .getXY().normalized().asDirection();

                    // TODO account for perspective distortion in arrow orientation
                    @SuppressWarnings("SuspiciousNameCombination")
                    Vector4 arrow1RDirectionX = new Vector4(arrow1RDirectionY.y, -arrow1RDirectionY.x, 0, 0).normalized();
                    @SuppressWarnings("SuspiciousNameCombination")
                    Vector4 arrow1LDirectionX = new Vector4(arrow1LDirectionY.y, -arrow1LDirectionY.x, 0, 0).normalized();
                    @SuppressWarnings("SuspiciousNameCombination")
                    Vector4 arrow2RDirectionX = new Vector4(arrow2RDirectionY.y, -arrow2RDirectionY.x, 0, 0).normalized();
                    @SuppressWarnings("SuspiciousNameCombination")
                    Vector4 arrow2LDirectionX = new Vector4(arrow2LDirectionY.y, -arrow2LDirectionY.x, 0, 0).normalized();


                    Vector4 arrow3DirectionY = widgetInfo[i].lightCenter.minus(widgetInfo[i].widgetPosition).getXY().normalized().asDirection();
                    @SuppressWarnings("SuspiciousNameCombination")
                    Vector4 arrow3DirectionX = new Vector4(arrow3DirectionY.y, -arrow3DirectionY.x, 0, 0).normalized();

                    Vector3 arrow3PositionR = widgetInfo[i].distanceWidgetPosition.minus(widgetInfo[i].widgetDisplacement.times(0.5f));
                    Vector3 arrow3PositionL = widgetInfo[i].distanceWidgetPosition.plus(widgetInfo[i].widgetDisplacement.times(0.5f));

//                            Vector3 arrow1PositionR = widgetTransformation.times(new Vector4(1,0,0,1)).getXYZ();
//                            Vector3 arrow1PositionL = widgetTransformation.times(new Vector4(-1,0,0,1)).getXYZ();
//                            Vector3 arrow2PositionR = widgetTransformation.times(new Vector4(0,1,0,1)).getXYZ();
//                            Vector3 arrow2PositionL = widgetTransformation.times(new Vector4(0,-1,0,1)).getXYZ();
//                            Vector3 arrow3PositionR = widgetTransformation.times(new Vector4(0,0,1,1)).getXYZ();
//                            Vector3 arrow3PositionL = widgetTransformation.times(new Vector4(0,0,-1,1)).getXYZ();

                    this.getContext().getState().disableDepthTest();
                    this.getContext().getState().disableBlending();
                    getDrawable().program().setUniform("color", new Vector4(1));

                    if (sceneModel.getLightingModel().getLightWidgetModel(i).isAzimuthWidgetVisible() &&
                        (Math.abs(widgetInfo[i].lightDisplacementWorld.x) > 0.001f || Math.abs(widgetInfo[i].lightDisplacementWorld.z) > 0.001f))
                    {
                        getDrawable().program().setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i + ".Azimuth"));

                        getDrawable().program().setUniform("model_view",
                            Matrix4.translate(arrow1PositionR)
                                .times(Matrix4.scale(widgetInfo[i].perspectiveWidgetScale, widgetInfo[i].perspectiveWidgetScale, 1.0f))
                                .times(Matrix4.fromColumns(
                                    arrow1RDirectionX,
                                    arrow1RDirectionY,
                                    new Vector4(0, 0, 1, 0),
                                    new Vector4(0, 0, 0, 1))));

                        getDrawable().draw(PrimitiveMode.TRIANGLE_FAN, cameraViewport.ofFramebuffer(framebuffer));

                        getDrawable().program().setUniform("model_view",
                            Matrix4.translate(arrow1PositionL)
                                .times(Matrix4.scale(widgetInfo[i].perspectiveWidgetScale, widgetInfo[i].perspectiveWidgetScale, 1.0f))
                                .times(Matrix4.fromColumns(
                                    arrow1LDirectionX.negated(),
                                    arrow1LDirectionY.negated(),
                                    new Vector4(0, 0, 1, 0),
                                    new Vector4(0, 0, 0, 1))));

                        getDrawable().draw(PrimitiveMode.TRIANGLE_FAN, cameraViewport.ofFramebuffer(framebuffer));
                    }

                    if (sceneModel.getLightingModel().getLightWidgetModel(i).isInclinationWidgetVisible())
                    {
                        getDrawable().program().setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i + ".Inclination"));

                        if (Math.PI / 2 - widgetInfo[i].inclination > 0.01f)
                        {
                            getDrawable().program().setUniform("model_view",
                                Matrix4.translate(arrow2PositionR)
                                    .times(Matrix4.scale(widgetInfo[i].perspectiveWidgetScale, widgetInfo[i].perspectiveWidgetScale, 1.0f))
                                    .times(Matrix4.fromColumns(
                                        arrow2RDirectionX,
                                        arrow2RDirectionY,
                                        new Vector4(0, 0, 1, 0),
                                        new Vector4(0, 0, 0, 1))));
                            getDrawable().draw(PrimitiveMode.TRIANGLE_FAN, cameraViewport.ofFramebuffer(framebuffer));
                        }

                        if (Math.PI / 2 + widgetInfo[i].inclination > 0.01f)
                        {
                            getDrawable().program().setUniform("model_view",
                                Matrix4.translate(arrow2PositionL)
                                    .times(Matrix4.scale(widgetInfo[i].perspectiveWidgetScale, widgetInfo[i].perspectiveWidgetScale, 1.0f))
                                    .times(Matrix4.fromColumns(
                                        arrow2LDirectionX.negated(),
                                        arrow2LDirectionY.negated(),
                                        new Vector4(0, 0, 1, 0),
                                        new Vector4(0, 0, 0, 1))));
                            getDrawable().draw(PrimitiveMode.TRIANGLE_FAN, cameraViewport.ofFramebuffer(framebuffer));
                        }
                    }

                    if (sceneModel.getLightingModel().getLightWidgetModel(i).isDistanceWidgetVisible())
                    {
                        getDrawable().program().setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i + ".Distance"));

                        getDrawable().program().setUniform("model_view",
                            Matrix4.translate(arrow3PositionL)
                                .times(Matrix4.scale(widgetInfo[i].perspectiveWidgetScale, widgetInfo[i].perspectiveWidgetScale, 1.0f))
                                .times(Matrix4.fromColumns(
                                    arrow3DirectionX.negated(),
                                    arrow3DirectionY.negated(),
                                    new Vector4(0, 0, 1, 0),
                                    new Vector4(0, 0, 0, 1))));

                        getDrawable().draw(PrimitiveMode.TRIANGLE_FAN, cameraViewport.ofFramebuffer(framebuffer));

                        if (widgetTransformation.getColumn(3).getXYZ().distance(widgetInfo[i].lightCenter) > 0.01f)
                        {
                            getDrawable().program().setUniform("model_view",
                                Matrix4.translate(arrow3PositionR)
                                    .times(Matrix4.scale(widgetInfo[i].perspectiveWidgetScale, widgetInfo[i].perspectiveWidgetScale, 1.0f))
                                    .times(Matrix4.fromColumns(
                                        arrow3DirectionX,
                                        arrow3DirectionY,
                                        new Vector4(0, 0, 1, 0),
                                        new Vector4(0, 0, 0, 1))));
                            getDrawable().draw(PrimitiveMode.TRIANGLE_FAN, cameraViewport.ofFramebuffer(framebuffer));
                        }
                    }
                }
            }

            getContext().getState().disableBlending();
            getContext().getState().enableDepthWrite();
            getContext().getState().enableDepthTest();
        }
    }
}
