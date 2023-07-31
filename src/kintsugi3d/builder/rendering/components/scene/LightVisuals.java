/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.rendering.components.scene;

import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.Matrix3;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;
import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Class for drawing the 3D light representations and manipulation widgets.
 * @param <ContextType>
 */
public class LightVisuals<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final ContextType context;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;

    private ProgramObject<ContextType> lightProgram;
    private VertexBuffer<ContextType> rectangleVertices;
    private Texture2D<ContextType> lightTexture;
    private Texture2D<ContextType> lightCenterTexture;
    private Drawable<ContextType> lightDrawable;

    private ProgramObject<ContextType> solidProgram;
    private VertexBuffer<ContextType> widgetVertices;
    private Drawable<ContextType> widgetDrawable;

    private ProgramObject<ContextType> circleProgram;
    private Drawable<ContextType> circleDrawable;

    public LightVisuals(ContextType context, SceneModel sceneModel, SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.context = context;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;

        for (int i = 0; i < 4; i++)
        {
            sceneViewportModel.addSceneObjectType("Light." + i);
            sceneViewportModel.addSceneObjectType("Light." + i + ".Center");
            sceneViewportModel.addSceneObjectType("Light." + i + ".Azimuth");
            sceneViewportModel.addSceneObjectType("Light." + i + ".Inclination");
            sceneViewportModel.addSceneObjectType("Light." + i + ".Distance");
        }
    }

    @Override
    public void initialize() throws FileNotFoundException
    {
        this.rectangleVertices = context.createRectangle();

        this.solidProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "solid.frag"))
                .createProgram();
        this.widgetVertices = context.createVertexBuffer()
                .setData(NativeVectorBufferFactory.getInstance()
                        .createFromFloatArray(3, 3, -1, -1, 0, 1, -1, 0, 0, 1, 0));

        this.widgetDrawable = context.createDrawable(this.solidProgram);
        this.widgetDrawable.addVertexBuffer("position", widgetVertices);

        this.lightProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "light.frag"))
                .createProgram();
        this.lightDrawable = context.createDrawable(this.lightProgram);
        this.lightDrawable.addVertexBuffer("position", rectangleVertices);

        this.circleProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "circle.frag"))
                .createProgram();
        this.circleDrawable = context.createDrawable(this.circleProgram);
        this.circleDrawable.addVertexBuffer("position", rectangleVertices);

        NativeVectorBuffer lightTextureData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 1, 4096);

        NativeVectorBuffer lightCenterTextureData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 1, 4096);

        int k = 0;
        for (int i = 0; i < 64; i++)
        {
            double x = i * 2.0 / 63.0 - 1.0;

            for (int j = 0; j < 64; j++)
            {
                double y = j * 2.0 / 63.0 - 1.0;

                double rSq = x*x + y*y;
                lightTextureData.set(k, 0, (float)(Math.cos(Math.min(Math.sqrt(rSq), 1.0) * Math.PI) + 1.0) * 0.5f);

                if (rSq <= 1.0)
                {
                    lightCenterTextureData.set(k, 0, 1.0f);
                }

                k++;
            }
        }

        this.lightTexture = context.getTextureFactory().build2DColorTextureFromBuffer(64, 64, lightTextureData)
                .setInternalFormat(ColorFormat.R8)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(true)
                .createTexture();

        this.lightCenterTexture = context.getTextureFactory().build2DColorTextureFromBuffer(64, 64, lightCenterTextureData)
                .setInternalFormat(ColorFormat.R8)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(true)
                .createTexture();
    }

    @Override
    public void reloadShaders() throws FileNotFoundException
    {
        ProgramObject<ContextType> newLightProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "light.frag"))
                .createProgram();

        if (this.lightProgram != null)
        {
            this.lightProgram.close();
        }

        this.lightProgram = newLightProgram;
        this.lightDrawable = context.createDrawable(this.lightProgram);
        this.lightDrawable.addVertexBuffer("position", rectangleVertices);

        ProgramObject<ContextType> newWidgetProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "solid.frag"))
                .createProgram();

        if (this.solidProgram != null)
        {
            this.solidProgram.close();
        }

        this.solidProgram = newWidgetProgram;

        this.widgetDrawable = context.createDrawable(this.solidProgram);
        this.widgetDrawable.addVertexBuffer("position", widgetVertices);

        ProgramObject<ContextType> newCircleProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "relight"), "circle.frag"))
                .createProgram();

        if (this.circleProgram != null)
        {
            this.circleProgram.close();
        }

        this.circleProgram = newCircleProgram;

        this.circleDrawable = context.createDrawable(this.circleProgram);
        this.circleDrawable.addVertexBuffer("position", rectangleVertices);
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        FramebufferSize size = framebuffer.getSize();

        if (this.sceneModel.getSettingsModel().getBoolean("relightingEnabled")
            && this.sceneModel.getSettingsModel().getBoolean("visibleLightsEnabled")
            && !sceneModel.getSettingsModel().getBoolean("lightCalibrationMode"))
        {
            this.context.getState().disableDepthWrite();

            // Draw lights
            for (int i = 0; i < sceneModel.getLightingModel().getLightCount(); i++)
            {
                this.context.getState().setBlendFunction(new BlendFunction(BlendFunction.Weight.ONE, BlendFunction.Weight.ONE));
                this.context.getState().enableDepthTest();

                if (sceneModel.getSettingsModel().getBoolean("lightWidgetsEnabled")
                    && sceneModel.getLightingModel().isLightWidgetEnabled(i)
                    && sceneModel.getLightingModel().getLightWidgetModel(i).isCenterWidgetVisible())
                {
                    this.lightProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i + ".Center"));

                    Vector3 lightCenter = cameraViewport.getView().times(this.sceneModel.getLightingModel().getLightCenter(i).times(sceneModel.getScale()).asPosition()).getXYZ();

                    this.lightProgram.setUniform("model_view",
                            Matrix4.translate(lightCenter)
                                    .times(Matrix4.scale(
                                            -lightCenter.z * sceneModel.getVerticalFieldOfView(size) / 64.0f,
                                            -lightCenter.z * sceneModel.getVerticalFieldOfView(size) / 64.0f,
                                            1.0f)));
                    this.lightProgram.setUniform("projection", cameraViewport.getViewportProjection());

                    this.lightProgram.setTexture("lightTexture", this.lightCenterTexture);

                    this.context.getState().disableDepthTest();
                    this.lightProgram.setUniform("color",
                            new Vector3(this.sceneModel.getLightingModel().getLightWidgetModel(i).isCenterWidgetSelected() ? 1.0f : 0.5f));
                    this.lightDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());
                }

                Matrix4 widgetTransformation = cameraViewport.getView()
                        .times(sceneModel.getUnscaledMatrix(sceneModel.getLightingModel().getLightMatrix(i).quickInverse(0.01f)));

                if (sceneModel.getLightingModel().isLightVisualizationEnabled(i))
                {
                    this.context.getState().setBlendFunction(new BlendFunction(BlendFunction.Weight.ONE, BlendFunction.Weight.ONE));
                    this.lightProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i));
                    this.lightProgram.setUniform("color", sceneModel.getLightingModel().getLightPrototype(i).getColor().times((float)Math.PI));

                    Vector3 lightPosition = widgetTransformation.getColumn(3).getXYZ();

                    this.lightProgram.setUniform("model_view",
                            Matrix4.translate(lightPosition)
                                    .times(Matrix4.scale(-lightPosition.z / 32.0f, -lightPosition.z / 32.0f, 1.0f)));
                    this.lightProgram.setUniform("projection", cameraViewport.getViewportProjection());
                    this.lightProgram.setTexture("lightTexture", this.lightTexture);
                    this.lightDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());
                }

                if (sceneModel.getSettingsModel().getBoolean("lightWidgetsEnabled") && sceneModel.getLightingModel().isLightWidgetEnabled(i))
                {
                    this.solidProgram.setUniform("projection", cameraViewport.getViewportProjection());

                    float lightWidgetScale = sceneViewportModel.computeRawLightWidgetScale(cameraViewport.getView(), size);
                    Vector3 lightCenter = cameraViewport.getView().times(this.sceneModel.getLightingModel().getLightCenter(i).times(sceneModel.getScale()).asPosition()).getXYZ();
                    Vector3 widgetPosition = widgetTransformation.getColumn(3).getXYZ()
                            .minus(lightCenter)
                            .normalized()
                            .times(lightWidgetScale)
                            .plus(lightCenter);
                    Vector3 widgetDisplacement = widgetPosition.minus(lightCenter);
                    float widgetDistance = widgetDisplacement.length();

                    Vector3 distanceWidgetPosition = widgetTransformation.getColumn(3).getXYZ()
                            .minus(lightCenter)
                            .times(Math.min(1, sceneViewportModel.computeRawLightWidgetScale(cameraViewport.getView(), size) /
                                    widgetTransformation.getColumn(3).getXYZ().distance(lightCenter)))
                            .plus(lightCenter);

                    float perspectiveWidgetScale = -widgetPosition.z * sceneModel.getVerticalFieldOfView(size) / 128;

                    this.context.getState().disableDepthTest();
                    this.context.getState().setBlendFunction(new BlendFunction(BlendFunction.Weight.ONE, BlendFunction.Weight.ONE));

                    if (sceneModel.getLightingModel().getLightWidgetModel(i).isDistanceWidgetVisible() || sceneModel.getLightingModel().getLightWidgetModel(i).isCenterWidgetVisible())
                    {
                        Vector3 lineEndpoint = widgetPosition.minus(lightCenter)
                                .times(0.5f / widgetPosition.getXY().distance(lightCenter.getXY()))
                                .minus(lightCenter);

                        try
                                (
                                        VertexBuffer<ContextType> line =
                                                context.createVertexBuffer()
                                                        .setData(NativeVectorBufferFactory.getInstance()
                                                                .createFromFloatArray(3, 2, lineEndpoint.x, lineEndpoint.y, lineEndpoint.z, lightCenter.x, lightCenter.y, lightCenter.z))
                                )
                        {
                            Drawable<ContextType> lineRenderable = context.createDrawable(this.solidProgram);
                            lineRenderable.addVertexBuffer("position", line);
                            this.solidProgram.setUniform("model_view", Matrix4.IDENTITY);
                            this.solidProgram.setUniform("color",
                                    new Vector3(sceneModel.getLightingModel().getLightWidgetModel(i).isDistanceWidgetSelected()
                                            || sceneModel.getLightingModel().getLightWidgetModel(i).isCenterWidgetSelected() ? 1.0f : 0.5f)
                                            .asVector4(1));
                            this.solidProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i + ".Distance"));
                            lineRenderable.draw(PrimitiveMode.LINES, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());
                        }
                    }

                    if (sceneModel.getLightingModel().getLightWidgetModel(i).isInclinationWidgetVisible()
                            && sceneModel.getLightingModel().getLightWidgetModel(i).isInclinationWidgetSelected())
                    {
                        Vector3 lineEndpoint1 = lightCenter
                                .plus(cameraViewport.getView().times(new Vector4(0,widgetDistance,0,0)).getXYZ());
                        Vector3 lineEndpoint2 = lightCenter
                                .plus(cameraViewport.getView().times(new Vector4(0,-widgetDistance,0,0)).getXYZ());

                        try
                                (
                                        VertexBuffer<ContextType> line =
                                                context.createVertexBuffer()
                                                        .setData(NativeVectorBufferFactory.getInstance()
                                                                .createFromFloatArray(3, 2, lineEndpoint1.x, lineEndpoint1.y, lineEndpoint1.z, lineEndpoint2.x, lineEndpoint2.y, lineEndpoint2.z))
                                )
                        {
                            Drawable<ContextType> lineRenderable = context.createDrawable(this.solidProgram);
                            lineRenderable.addVertexBuffer("position", line);
                            this.solidProgram.setUniform("model_view", Matrix4.IDENTITY);
                            this.solidProgram.setUniform("color",
                                    new Vector3(sceneModel.getLightingModel().getLightWidgetModel(i).isDistanceWidgetSelected()
                                            || sceneModel.getLightingModel().getLightWidgetModel(i).isCenterWidgetSelected() ? 1.0f : 0.5f)
                                            .asVector4(1));
                            this.solidProgram.setUniform("objectID", 0);
                            lineRenderable.draw(PrimitiveMode.LINES, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());
                        }
                    }

                    Vector3 azimuthRotationAxis = cameraViewport.getView().times(new Vector4(0,1,0,0)).getXYZ();

                    this.circleProgram.setUniform("color", new Vector3(1));
                    this.circleProgram.setUniform("projection", cameraViewport.getViewportProjection());
                    this.circleProgram.setUniform("width", 1 / 128.0f);
                    this.circleProgram.setUniform("maxAngle", (float)Math.PI / 4);
                    this.circleProgram.setUniform("threshold", 0.005f);

                    Vector3 lightDisplacementAtInclination = widgetDisplacement
                            .minus(azimuthRotationAxis.times(widgetDisplacement.dot(azimuthRotationAxis)));
                    float lightDistanceAtInclination = lightDisplacementAtInclination.length();

                    context.getState().disableBackFaceCulling();

                    Vector3 lightDisplacementWorld = cameraViewport.getView().quickInverse(0.01f)
                            .times(widgetDisplacement.asDirection()).getXYZ();

                    double azimuth = Math.atan2(lightDisplacementWorld.x, lightDisplacementWorld.z);
                    double inclination = Math.asin(lightDisplacementWorld.normalized().y);

                    float cosineLightToPole = widgetDisplacement.normalized().dot(azimuthRotationAxis);
                    double azimuthArrowRotation = Math.min(Math.PI / 4,
                            16 * perspectiveWidgetScale / (widgetDistance * Math.sqrt(1 - cosineLightToPole * cosineLightToPole)));

                    double inclinationArrowRotation = Math.min(Math.PI / 4, 16 * perspectiveWidgetScale / widgetDistance);

                    if (sceneModel.getLightingModel().getLightWidgetModel(i).isAzimuthWidgetVisible() &&
                            (Math.abs(lightDisplacementWorld.x) > 0.001f || Math.abs(lightDisplacementWorld.z) > 0.001f))
                    {
                        // Azimuth circle
                        this.circleProgram.setUniform("maxAngle",
                                (float) (sceneModel.getLightingModel().getLightWidgetModel(i).isAzimuthWidgetSelected() ?
                                        Math.PI : azimuthArrowRotation));
                        this.circleProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i + ".Azimuth"));
                        this.circleProgram.setUniform("color",
                                new Vector3(sceneModel.getLightingModel().getLightWidgetModel(i).isAzimuthWidgetSelected() ? 1.0f :0.5f));
                        this.circleProgram.setUniform("model_view",
                                Matrix4.translate(lightCenter.plus(azimuthRotationAxis.times(widgetDisplacement.dot(azimuthRotationAxis))))
                                        .times(cameraViewport.getView().getUpperLeft3x3().asMatrix4())
                                        .times(Matrix4.scale(2 * lightDistanceAtInclination))
                                        .times(Matrix4.rotateX(-Math.PI / 2))
                                        .times(Matrix4.rotateZ(azimuth - Math.PI / 2)));
                        this.circleDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());
                    }

                    if (sceneModel.getLightingModel().getLightWidgetModel(i).isInclinationWidgetVisible())
                    {
                        // Inclination circle
                        this.circleProgram.setUniform("maxAngle",
                                (float) (sceneModel.getLightingModel().getLightWidgetModel(i).isInclinationWidgetSelected() ?
                                        Math.PI / 2 : inclinationArrowRotation));
                        this.circleProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i + ".Inclination"));
                        this.circleProgram.setUniform("color",
                                new Vector3(sceneModel.getLightingModel().getLightWidgetModel(i).isInclinationWidgetSelected() ? 1.0f : 0.5f));
                        this.circleProgram.setUniform("model_view",
                                Matrix4.translate(lightCenter)
                                        .times(Matrix4.scale(2 * widgetDistance))
                                        .times(widgetTransformation.getUpperLeft3x3()
                                                .times(Matrix3.rotateY(-Math.PI / 2))
                                                .times(sceneModel.getLightingModel().getLightWidgetModel(i).isInclinationWidgetSelected() ?
                                                        Matrix3.rotateZ(-inclination) : Matrix3.IDENTITY)
                                                .asMatrix4()));
                        this.circleDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());
                    }

                    context.getState().enableBackFaceCulling();

                    Vector3 arrow1PositionR = Matrix3.rotateAxis(azimuthRotationAxis, azimuthArrowRotation)
                            .times(widgetPosition.minus(lightCenter))
                            .plus(lightCenter);

                    Vector3 arrow1PositionL = Matrix3.rotateAxis(azimuthRotationAxis, -azimuthArrowRotation)
                            .times(widgetPosition.minus(lightCenter))
                            .plus(lightCenter);

                    Vector3 arrow2PositionR = widgetTransformation.getUpperLeft3x3()
                            .times(Matrix3.rotateX(-inclinationArrowRotation))
                            .times(widgetTransformation.quickInverse(0.01f).getUpperLeft3x3())
                            .times(widgetPosition.minus(lightCenter))
                            .plus(lightCenter);

                    Vector3 arrow2PositionL = widgetTransformation.getUpperLeft3x3()
                            .times(Matrix3.rotateX(inclinationArrowRotation))
                            .times(widgetTransformation.quickInverse(0.01f).getUpperLeft3x3())
                            .times(widgetPosition.minus(lightCenter))
                            .plus(lightCenter);

                    Vector4 arrow1RDirectionY =  Matrix3.rotateAxis(azimuthRotationAxis, azimuthArrowRotation)
                            .times(widgetTransformation.getUpperLeft3x3())
                            .times(new Vector3(1,0,0))
                            .getXY().normalized().asDirection();

                    Vector4 arrow1LDirectionY =  Matrix3.rotateAxis(azimuthRotationAxis, -azimuthArrowRotation)
                            .times(widgetTransformation.getUpperLeft3x3())
                            .times(new Vector3(1,0,0))
                            .getXY().normalized().asDirection();

                    Vector4 arrow2RDirectionY = widgetTransformation.getUpperLeft3x3()
                            .times(Matrix3.rotateX(-inclinationArrowRotation))
                            .times(new Vector3(0,1,0))
                            .getXY().normalized().asDirection();

                    Vector4 arrow2LDirectionY = widgetTransformation.getUpperLeft3x3()
                            .times(Matrix3.rotateX(inclinationArrowRotation))
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


                    Vector4 arrow3DirectionY = lightCenter.minus(widgetPosition).getXY().normalized().asDirection();
                    @SuppressWarnings("SuspiciousNameCombination")
                    Vector4 arrow3DirectionX = new Vector4(arrow3DirectionY.y, -arrow3DirectionY.x, 0, 0).normalized();

                    Vector3 arrow3PositionR = distanceWidgetPosition.minus(widgetDisplacement.times(0.5f));
                    Vector3 arrow3PositionL = distanceWidgetPosition.plus(widgetDisplacement.times(0.5f));

//                            Vector3 arrow1PositionR = widgetTransformation.times(new Vector4(1,0,0,1)).getXYZ();
//                            Vector3 arrow1PositionL = widgetTransformation.times(new Vector4(-1,0,0,1)).getXYZ();
//                            Vector3 arrow2PositionR = widgetTransformation.times(new Vector4(0,1,0,1)).getXYZ();
//                            Vector3 arrow2PositionL = widgetTransformation.times(new Vector4(0,-1,0,1)).getXYZ();
//                            Vector3 arrow3PositionR = widgetTransformation.times(new Vector4(0,0,1,1)).getXYZ();
//                            Vector3 arrow3PositionL = widgetTransformation.times(new Vector4(0,0,-1,1)).getXYZ();

                    this.context.getState().disableDepthTest();
                    this.context.getState().disableBlending();
                    this.solidProgram.setUniform("color", new Vector4(1));

                    if (sceneModel.getLightingModel().getLightWidgetModel(i).isAzimuthWidgetVisible() &&
                            (Math.abs(lightDisplacementWorld.x) > 0.001f || Math.abs(lightDisplacementWorld.z) > 0.001f))
                    {
                        this.solidProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i + ".Azimuth"));

                        this.solidProgram.setUniform("model_view",
                                Matrix4.translate(arrow1PositionR)
                                        .times(Matrix4.scale(perspectiveWidgetScale, perspectiveWidgetScale, 1.0f))
                                        .times(Matrix4.fromColumns(
                                                arrow1RDirectionX,
                                                arrow1RDirectionY,
                                                new Vector4(0, 0, 1, 0),
                                                new Vector4(0, 0, 0, 1))));

                        this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());

                        this.solidProgram.setUniform("model_view",
                                Matrix4.translate(arrow1PositionL)
                                        .times(Matrix4.scale(perspectiveWidgetScale, perspectiveWidgetScale, 1.0f))
                                        .times(Matrix4.fromColumns(
                                                arrow1LDirectionX.negated(),
                                                arrow1LDirectionY.negated(),
                                                new Vector4(0, 0, 1, 0),
                                                new Vector4(0, 0, 0, 1))));

                        this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());
                    }

                    if (sceneModel.getLightingModel().getLightWidgetModel(i).isInclinationWidgetVisible())
                    {
                        this.solidProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i + ".Inclination"));

                        if (Math.PI / 2 - inclination > 0.01f)
                        {
                            this.solidProgram.setUniform("model_view",
                                    Matrix4.translate(arrow2PositionR)
                                            .times(Matrix4.scale(perspectiveWidgetScale, perspectiveWidgetScale, 1.0f))
                                            .times(Matrix4.fromColumns(
                                                    arrow2RDirectionX,
                                                    arrow2RDirectionY,
                                                    new Vector4(0, 0, 1, 0),
                                                    new Vector4(0, 0, 0, 1))));
                            this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());
                        }

                        if (Math.PI / 2 + inclination > 0.01f)
                        {
                            this.solidProgram.setUniform("model_view",
                                    Matrix4.translate(arrow2PositionL)
                                            .times(Matrix4.scale(perspectiveWidgetScale, perspectiveWidgetScale, 1.0f))
                                            .times(Matrix4.fromColumns(
                                                    arrow2LDirectionX.negated(),
                                                    arrow2LDirectionY.negated(),
                                                    new Vector4(0, 0, 1, 0),
                                                    new Vector4(0, 0, 0, 1))));
                            this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());
                        }
                    }

                    if (sceneModel.getLightingModel().getLightWidgetModel(i).isDistanceWidgetVisible())
                    {
                        this.solidProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("Light." + i + ".Distance"));

                        this.solidProgram.setUniform("model_view",
                                Matrix4.translate(arrow3PositionL)
                                        .times(Matrix4.scale(perspectiveWidgetScale, perspectiveWidgetScale, 1.0f))
                                        .times(Matrix4.fromColumns(
                                                arrow3DirectionX.negated(),
                                                arrow3DirectionY.negated(),
                                                new Vector4(0, 0, 1, 0),
                                                new Vector4(0, 0, 0, 1))));

                        this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());

                        if (widgetTransformation.getColumn(3).getXYZ().distance(lightCenter) > 0.01f)
                        {
                            this.solidProgram.setUniform("model_view",
                                    Matrix4.translate(arrow3PositionR)
                                            .times(Matrix4.scale(perspectiveWidgetScale, perspectiveWidgetScale, 1.0f))
                                            .times(Matrix4.fromColumns(
                                                    arrow3DirectionX,
                                                    arrow3DirectionY,
                                                    new Vector4(0, 0, 1, 0),
                                                    new Vector4(0, 0, 0, 1))));
                            this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());
                        }
                    }
                }
            }

            context.getState().disableBlending();
            context.getState().enableDepthWrite();
            this.context.getState().enableDepthTest();
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

        if (lightProgram != null)
        {
            lightProgram.close();
            lightProgram = null;
        }

        if (lightTexture != null)
        {
            lightTexture.close();
            lightTexture = null;
        }

        if (lightCenterTexture != null)
        {
            lightCenterTexture.close();
            lightCenterTexture = null;
        }

        if (widgetVertices != null)
        {
            widgetVertices.close();
            widgetVertices = null;
        }

        if (circleProgram != null)
        {
            circleProgram.close();
            circleProgram = null;
        }
    }
}
