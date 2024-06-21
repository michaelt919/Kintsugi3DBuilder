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

import java.util.stream.IntStream;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.gl.core.FramebufferSize;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;

public final class LightWidgetInfo
{
    public final Matrix4 widgetTransformation;
    public final float lightWidgetScale;
    public final Vector3 lightCenter;
    public final Vector3 widgetPosition;
    public final Vector3 widgetDisplacement;
    public final float widgetDistance;
    public final Vector3 distanceWidgetPosition;
    public final float perspectiveWidgetScale;
    public final Vector3 azimuthRotationAxis;
    public final float lightDistanceAtInclination;
    public final Vector3 lightDisplacementWorld;
    public final double azimuth;
    public final double inclination;
    public final double azimuthArrowRotation;
    public final double inclinationArrowRotation;

    public static LightWidgetInfo[] calculate(SceneViewportModel sceneViewportModel, SceneModel sceneModel, CameraViewport cameraViewport, FramebufferSize size)
    {
        return IntStream.range(0, sceneModel.getLightingModel().getLightCount())
            .mapToObj(i -> new LightWidgetInfo(sceneViewportModel, sceneModel, cameraViewport, size, i))
            .toArray(LightWidgetInfo[]::new);
    }

    private LightWidgetInfo(SceneViewportModel sceneViewportModel, SceneModel sceneModel, CameraViewport cameraViewport, FramebufferSize size, int lightIndex)
    {
        widgetTransformation = cameraViewport.getView().times(sceneModel.getInverseLightViewMatrix(lightIndex));
        lightWidgetScale = sceneViewportModel.computeRawLightWidgetScale(cameraViewport.getView(), size);
        lightCenter = cameraViewport.getView().times(sceneModel.getLightingModel().getLightCenter(lightIndex).times(sceneModel.getScale()).asPosition()).getXYZ();
        widgetPosition = widgetTransformation.getColumn(3).getXYZ()
            .minus(lightCenter)
            .normalized()
            .times(lightWidgetScale)
            .plus(lightCenter);
        widgetDisplacement = widgetPosition.minus(lightCenter);
        widgetDistance = widgetDisplacement.length();

        distanceWidgetPosition = widgetTransformation.getColumn(3).getXYZ()
            .minus(lightCenter)
            .times(Math.min(1, sceneViewportModel.computeRawLightWidgetScale(cameraViewport.getView(), size) /
                widgetTransformation.getColumn(3).getXYZ().distance(lightCenter)))
            .plus(lightCenter);

        perspectiveWidgetScale = -widgetPosition.z * sceneModel.getVerticalFieldOfView(size) / 128;

        azimuthRotationAxis = cameraViewport.getView().times(new Vector4(0,1,0,0)).getXYZ();

        Vector3 lightDisplacementAtInclination = widgetDisplacement
            .minus(azimuthRotationAxis.times(widgetDisplacement.dot(azimuthRotationAxis)));
        lightDistanceAtInclination = lightDisplacementAtInclination.length();

        lightDisplacementWorld = cameraViewport.getView().quickInverse(0.01f)
            .times(widgetDisplacement.asDirection()).getXYZ();

        azimuth = Math.atan2(lightDisplacementWorld.x, lightDisplacementWorld.z);
        inclination = Math.asin(lightDisplacementWorld.normalized().y);

        float cosineLightToPole = widgetDisplacement.normalized().dot(azimuthRotationAxis);
        azimuthArrowRotation = Math.min(Math.PI / 4,
            16 * perspectiveWidgetScale / (widgetDistance * Math.sqrt(1 - cosineLightToPole * cosineLightToPole)));

        inclinationArrowRotation = Math.min(Math.PI / 4, 16 * perspectiveWidgetScale / widgetDistance);

    }
}
