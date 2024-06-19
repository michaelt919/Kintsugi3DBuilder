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

package kintsugi3d.builder.tools;

import java.util.function.Consumer;

import kintsugi3d.gl.vecmath.DoubleVector2;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.window.*;
import kintsugi3d.builder.state.ExtendedLightingModel;
import kintsugi3d.builder.state.LightWidgetModel;
import kintsugi3d.builder.state.SceneViewport;
import kintsugi3d.builder.state.SceneViewportModel;

final class LightTool implements PickerTool
{
    private float azimuthOffset;
    private float inclinationOffset;
    private float distanceOffset;

    private Consumer<DoubleVector2> updateFunction;
    private int lightIndex;

    private final ExtendedLightingModel lightingModel;
    private final SceneViewportModel sceneViewportModel;

    private static class Builder extends ToolBuilderBase<LightTool>
    {
        @Override
        public LightTool create()
        {
            return new LightTool(getLightingModel(), getSceneViewportModel());
        }
    }

    static ToolBuilder<LightTool> getBuilder()
    {
        return new Builder();
    }

    private LightTool(ExtendedLightingModel lightingModel, SceneViewportModel sceneViewportModel)
    {
        this.lightingModel = lightingModel;
        this.sceneViewportModel = sceneViewportModel;
    }

    @Override
    public boolean mouseButtonPressed(Canvas3D<? extends kintsugi3d.gl.core.Context<?>> canvas, int buttonIndex, ModifierKeys mods)
    {
        if (buttonIndex == 0)
        {
            updateFunction = null;

            CursorPosition cursorPosition = canvas.getCursorPosition();
            CanvasSize canvasSize = canvas.getSize();

            double normalizedX = cursorPosition.x / canvasSize.width;
            double normalizedY = cursorPosition.y / canvasSize.height;

            Object clickedObject = sceneViewportModel.getSceneViewport().getObjectAtCoordinates(normalizedX, normalizedY);
            if (clickedObject instanceof String)
            {
                String clickedObjectName = (String)clickedObject;
                String[] nameParts = clickedObjectName.split("\\.");
                if ("Light".equals(nameParts[0]))
                {
                    this.lightIndex = Integer.parseInt(nameParts[1]);
                    LightWidgetModel lightWidgetModel = lightingModel.getLightWidgetModel(lightIndex);

                    lightingModel.setSelectedLightIndex(this.lightIndex);

                    if (nameParts.length > 2)
                    {
                        switch (nameParts[2])
                        {
                            case "Azimuth":
                                updateFunction = this::updateAzimuth;
                                azimuthOffset = lightingModel.getLight(lightIndex).getAzimuth() * (float)Math.PI / 180
                                    - getAzimuthAtWindowPosition(new DoubleVector2(normalizedX, normalizedY));

                                lightWidgetModel.setAzimuthWidgetVisible(true);
                                lightWidgetModel.setAzimuthWidgetSelected(true);
                                lightWidgetModel.setInclinationWidgetVisible(false);
                                lightWidgetModel.setDistanceWidgetVisible(false);
                                lightWidgetModel.setCenterWidgetVisible(false);

                                break;

                            case "Inclination":
                                updateFunction = this::updateInclination;
                                inclinationOffset = lightingModel.getLight(lightIndex).getInclination() * (float)Math.PI / 180
                                    - getInclinationAtWindowPosition(new DoubleVector2(normalizedX, normalizedY));

                                lightWidgetModel.setInclinationWidgetVisible(true);
                                lightWidgetModel.setInclinationWidgetSelected(true);
                                lightWidgetModel.setAzimuthWidgetVisible(false);
                                lightWidgetModel.setDistanceWidgetVisible(false);
                                lightWidgetModel.setCenterWidgetVisible(false);

                                break;

                            case "Distance":
                                updateFunction = this::updateDistance;
                                distanceOffset = lightingModel.getLight(lightIndex).getDistance()
                                    - getDistanceAtWindowPosition(new DoubleVector2(normalizedX, normalizedY));

                                lightWidgetModel.setDistanceWidgetVisible(true);
                                lightWidgetModel.setDistanceWidgetSelected(true);
                                lightWidgetModel.setAzimuthWidgetVisible(false);
                                lightWidgetModel.setInclinationWidgetVisible(false);
                                lightWidgetModel.setCenterWidgetVisible(false);

                                break;

                            case "Center":
                                updateFunction = this::updateCenter;
                                lightWidgetModel.setCenterWidgetVisible(true);
                                lightWidgetModel.setCenterWidgetSelected(true);
                                lightWidgetModel.setAzimuthWidgetVisible(false);
                                lightWidgetModel.setInclinationWidgetVisible(false);
                                lightWidgetModel.setDistanceWidgetVisible(false);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }

            lightingModel.setLightWidgetsEthereal(true);
        }

        return updateFunction != null;
    }

    @Override
    public boolean mouseButtonReleased(Canvas3D<? extends kintsugi3d.gl.core.Context<?>> canvas, int buttonIndex, ModifierKeys mods)
    {
        if (buttonIndex == 0)
        {
            LightWidgetModel lightWidgetModel = lightingModel.getLightWidgetModel(lightIndex);
            lightWidgetModel.setAzimuthWidgetVisible(true);
            lightWidgetModel.setInclinationWidgetVisible(true);
            lightWidgetModel.setDistanceWidgetVisible(true);
            lightWidgetModel.setCenterWidgetVisible(true);

            lightingModel.setLightWidgetsEthereal(false);

            CanvasPosition position = canvas.getPosition();
            updateForHoverState(canvas, position.x, position.y);

            updateFunction = null;
        }

        return updateFunction != null;
    }

    private void updateForHoverState(Canvas3D<? extends kintsugi3d.gl.core.Context<?>> canvas, double xPos, double yPos)
    {
        for (int i = 0; i < lightingModel.getLightCount(); i++)
        {
            LightWidgetModel lightWidgetModel = lightingModel.getLightWidgetModel(i);
            lightWidgetModel.setAzimuthWidgetSelected(false);
            lightWidgetModel.setInclinationWidgetSelected(false);
            lightWidgetModel.setDistanceWidgetSelected(false);
            lightWidgetModel.setCenterWidgetSelected(false);
        }

        CanvasSize canvasSize = canvas.getSize();

        double normalizedX = xPos / canvasSize.width;
        double normalizedY = yPos / canvasSize.height;

        Object hoverObject = sceneViewportModel.getSceneViewport().getObjectAtCoordinates(normalizedX, normalizedY);
        if (hoverObject instanceof String)
        {
            String clickedObjectName = (String)hoverObject;
            String[] nameParts = clickedObjectName.split("\\.");
            if ("Light".equals(nameParts[0]))
            {
                LightWidgetModel lightWidgetModel = lightingModel.getLightWidgetModel(Integer.parseInt(nameParts[1]));

                if (nameParts.length > 2)
                {
                    switch (nameParts[2])
                    {
                        case "Azimuth":
                            lightWidgetModel.setAzimuthWidgetSelected(true);
                            break;

                        case "Inclination":
                            lightWidgetModel.setInclinationWidgetSelected(true);
                            break;

                        case "Distance":
                            lightWidgetModel.setDistanceWidgetSelected(true);
                            break;

                        case "Center":
                            lightWidgetModel.setCenterWidgetSelected(true);
                            break;

                        default:
                            break;
                    }
                }
            }
        }
    }

    private float getAzimuthAtWindowPosition(DoubleVector2 normalizedPosition)
    {
        SceneViewport sceneViewport = sceneViewportModel.getSceneViewport();
        Vector3 viewportCenter = sceneViewport.getViewportCenter();
        Vector3 cursorDirection = sceneViewport.getViewingDirection(normalizedPosition.x, normalizedPosition.y);

        Vector3 azimuthCenter = lightingModel.getLightCenter(lightIndex)
            .plus(new Vector3(
                0,
                sceneViewport.getLightWidgetScale() *
                    (float)Math.sin(lightingModel.getLight(lightIndex).getInclination() * Math.PI / 180),
                0));

        float t = (azimuthCenter.y - viewportCenter.y) / cursorDirection.y;
        Vector3 newAzimuthDirection = viewportCenter.plus(cursorDirection.times(t)).minus(azimuthCenter);
        Vector2 newAzimuthDirectionNormalized = new Vector2(newAzimuthDirection.x, newAzimuthDirection.z).normalized();

        return (float)Math.atan2(-newAzimuthDirectionNormalized.x, newAzimuthDirectionNormalized.y);
    }

    private float getInclinationAtWindowPosition(DoubleVector2 normalizedPosition)
    {
        SceneViewport sceneViewport = sceneViewportModel.getSceneViewport();
        Vector3 viewportCenter = sceneViewport.getViewportCenter();
        Vector3 cursorDirection = sceneViewport.getViewingDirection(normalizedPosition.x, normalizedPosition.y);

        Vector3 lightCenter = lightingModel.getLightCenter(lightIndex);
        Vector3 lightDisplacement = lightingModel.getLightMatrix(lightIndex).quickInverse(0.01f).getColumn(3).getXYZ()
                                    .minus(lightingModel.getLightCenter(lightIndex));

        double azimuth = lightingModel.getLight(lightIndex).getAzimuth() * Math.PI / 180;

        Vector3 lightDirectionProjected = new Vector3(-(float)Math.sin(azimuth), 0, (float)Math.cos(azimuth));
        Vector3 inclinationAxis = new Vector3(-lightDisplacement.z, 0, lightDisplacement.x).normalized();

        float t = lightCenter.minus(viewportCenter).dot(inclinationAxis) / cursorDirection.dot(inclinationAxis);
        Vector3 newLightDirection = viewportCenter.plus(cursorDirection.times(t)).minus(lightCenter).normalized();

        return (float)Math.atan2(newLightDirection.y, newLightDirection.dot(lightDirectionProjected));
    }

    private float getDistanceAtWindowPosition(DoubleVector2 normalizedPosition)
    {
        SceneViewport sceneViewport = sceneViewportModel.getSceneViewport();
        Vector3 viewportCenter = sceneViewport.getViewportCenter();
        Vector3 cursorDirection = sceneViewport.getViewingDirection(normalizedPosition.x, normalizedPosition.y);

        Vector3 lightCenter = lightingModel.getLightCenter(lightIndex);
        Vector3 lightDirection = lightingModel.getLightMatrix(lightIndex).quickInverse(0.01f).getColumn(3).getXYZ()
            .minus(lightingModel.getLightCenter(lightIndex))
            .normalized();

        Vector3 normalDirection = cursorDirection.cross(lightDirection.cross(cursorDirection));
        return viewportCenter.minus(lightCenter).dot(normalDirection) / lightDirection.dot(normalDirection);
    }

    private void updateAzimuth(DoubleVector2 normalizedPosition)
    {
        lightingModel.getLight(lightIndex).setAzimuth((azimuthOffset + getAzimuthAtWindowPosition(normalizedPosition)) * 180 / (float)Math.PI);
    }

    private void updateInclination(DoubleVector2 normalizedPosition)
    {
        lightingModel.getLight(lightIndex).setInclination(Math.max(-90, Math.min(90,
            (inclinationOffset + getInclinationAtWindowPosition(normalizedPosition)) * 180 / (float)Math.PI)));
    }

    private void updateDistance(DoubleVector2 normalizedPosition)
    {
        lightingModel.getLight(lightIndex).setDistance(Math.max(0.001f, distanceOffset + getDistanceAtWindowPosition(normalizedPosition)));
    }

    private void updateCenter(DoubleVector2 normalizedPosition)
    {
        SceneViewport sceneViewport = sceneViewportModel.getSceneViewport();
        if ("IBRObject".equals(sceneViewport.getObjectAtCoordinates(normalizedPosition.x, normalizedPosition.y)))
        {
            lightingModel.setLightCenter(lightIndex, sceneViewport.get3DPositionAtCoordinates(normalizedPosition.x, normalizedPosition.y));
        }
    }

    @Override
    public boolean cursorMoved(Canvas3D<? extends kintsugi3d.gl.core.Context<?>> canvas, double xPos, double yPos)
    {
        if (updateFunction == null)
        {
            updateForHoverState(canvas, xPos, yPos);
            return false;
        }
        else
        {
            CanvasSize canvasSize = canvas.getSize();
            updateFunction.accept(new DoubleVector2(xPos / canvasSize.width, yPos / canvasSize.height));
            return true;
        }
    }
}
