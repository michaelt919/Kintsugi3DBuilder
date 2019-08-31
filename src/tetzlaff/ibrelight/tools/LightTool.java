/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.tools;

import java.util.function.Consumer;

import tetzlaff.gl.vecmath.DoubleVector2;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.*;
import tetzlaff.models.ExtendedLightingModel;
import tetzlaff.models.LightWidgetModel;
import tetzlaff.models.SceneViewport;
import tetzlaff.models.SceneViewportModel;

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
        public LightTool build()
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
    public boolean mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        if (buttonIndex == 0)
        {
            updateFunction = null;

            CursorPosition cursorPosition = window.getCursorPosition();
            WindowSize windowSize = window.getWindowSize();

            double normalizedX = cursorPosition.x / windowSize.width;
            double normalizedY = cursorPosition.y / windowSize.height;

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
    public boolean mouseButtonReleased(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        if (buttonIndex == 0)
        {
            LightWidgetModel lightWidgetModel = lightingModel.getLightWidgetModel(lightIndex);
            lightWidgetModel.setAzimuthWidgetVisible(true);
            lightWidgetModel.setInclinationWidgetVisible(true);
            lightWidgetModel.setDistanceWidgetVisible(true);
            lightWidgetModel.setCenterWidgetVisible(true);

            lightingModel.setLightWidgetsEthereal(false);

            WindowPosition position = window.getWindowPosition();
            updateForHoverState(window, position.x, position.y);

            updateFunction = null;
        }

        return updateFunction != null;
    }

    private void updateForHoverState(Window<?> window, double xPos, double yPos)
    {
        for (int i = 0; i < lightingModel.getLightCount(); i++)
        {
            LightWidgetModel lightWidgetModel = lightingModel.getLightWidgetModel(i);
            lightWidgetModel.setAzimuthWidgetSelected(false);
            lightWidgetModel.setInclinationWidgetSelected(false);
            lightWidgetModel.setDistanceWidgetSelected(false);
            lightWidgetModel.setCenterWidgetSelected(false);
        }

        WindowSize windowSize = window.getWindowSize();

        double normalizedX = xPos / windowSize.width;
        double normalizedY = yPos / windowSize.height;

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
    public boolean cursorMoved(Window<?> window, double xPos, double yPos)
    {
        if (updateFunction == null)
        {
            updateForHoverState(window, xPos, yPos);
            return false;
        }
        else
        {
            WindowSize windowSize = window.getWindowSize();
            updateFunction.accept(new DoubleVector2(xPos / windowSize.width, yPos / windowSize.height));
            return true;
        }
    }
}
