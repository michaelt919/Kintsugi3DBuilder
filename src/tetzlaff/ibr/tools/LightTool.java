package tetzlaff.ibr.tools;

import java.time.LocalTime;
import java.util.function.Consumer;

import tetzlaff.gl.vecmath.DoubleVector2;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.*;
import tetzlaff.models.*;

final class LightTool implements Tool
{
    private float azimuthOffset;
    private float inclinationOffset;
    private float distanceOffset;

    private Consumer<DoubleVector2> updateFunction;
    private int lightIndex;

    private final ExtendedLightingModel lightingModel;
    private final SceneViewportModel sceneViewportModel;

    private final OrbitTool orbitFallbackTool;

    private static class Builder extends ToolBuilderBase<LightTool>
    {
        @Override
        public LightTool build()
        {
            return new LightTool(getCameraModel(), getEnvironmentMapModel(), getLightingModel(), getSceneViewportModel(), getToolSelectionModel());
        }
    }

    static ToolBuilder<LightTool> getBuilder()
    {
        return new Builder();
    }

    private LightTool(ExtendedCameraModel cameraModel, ReadonlyEnvironmentMapModel environmentMapModel, ExtendedLightingModel lightingModel,
        SceneViewportModel sceneViewportModel, ToolSelectionModel toolSelectionModel)
    {
        this.lightingModel = lightingModel;
        this.sceneViewportModel = sceneViewportModel;
        this.orbitFallbackTool = OrbitTool.getBuilder()
            .setCameraModel(cameraModel)
            .setEnvironmentMapModel(environmentMapModel)
            .setLightingModel(lightingModel)
            .setSceneViewportModel(sceneViewportModel)
            .setToolSelectionModel(toolSelectionModel)
            .build();
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        if (buttonIndex == 0)
        {
            updateFunction = null;

            CursorPosition cursorPosition = window.getCursorPosition();
            WindowSize windowSize = window.getWindowSize();

            double normalizedX = cursorPosition.x / windowSize.width;
            double normalizedY = cursorPosition.y / windowSize.height;

            Object clickedObject = sceneViewportModel.getObjectAtCoordinates(normalizedX, normalizedY);
            if (clickedObject instanceof String)
            {
                String clickedObjectName = (String)clickedObject;
                String[] nameParts = clickedObjectName.split("\\.");
                if ("Light".equals(nameParts[0]))
                {
                    this.lightIndex = Integer.parseInt(nameParts[1]);
                    LightWidgetModel lightWidgetModel = lightingModel.getLightWidgetModel(lightIndex);

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

        if (updateFunction == null)
        {
            orbitFallbackTool.mouseButtonPressed(window, buttonIndex, mods);
        }

        System.out.println("Click: " + LocalTime.now());
    }

    @Override
    public void mouseButtonReleased(Window<?> window, int buttonIndex, ModifierKeys mods)
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

        Object hoverObject = sceneViewportModel.getObjectAtCoordinates(normalizedX, normalizedY);
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
        Vector3 viewportCenter = sceneViewportModel.getViewportCenter();
        Vector3 cursorDirection = sceneViewportModel.getViewingDirection(normalizedPosition.x, normalizedPosition.y);

        Vector3 azimuthCenter = lightingModel.getLightCenter(lightIndex)
            .plus(new Vector3(
                0,
                lightingModel.getLight(lightIndex).getDistance() *
                    (float)Math.sin(lightingModel.getLight(lightIndex).getInclination() * Math.PI / 180),
                0));

        float t = (azimuthCenter.y - viewportCenter.y) / cursorDirection.y;
        Vector3 newAzimuthDirection = viewportCenter.plus(cursorDirection.times(t)).minus(azimuthCenter);
        Vector2 newAzimuthDirectionNormalized = new Vector2(newAzimuthDirection.x, newAzimuthDirection.z).normalized();

        return (float)Math.atan2(-newAzimuthDirectionNormalized.x, newAzimuthDirectionNormalized.y);
    }

    private float getInclinationAtWindowPosition(DoubleVector2 normalizedPosition)
    {
        Vector3 viewportCenter = sceneViewportModel.getViewportCenter();
        Vector3 cursorDirection = sceneViewportModel.getViewingDirection(normalizedPosition.x, normalizedPosition.y);

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
        Vector3 viewportCenter = sceneViewportModel.getViewportCenter();
        Vector3 cursorDirection = sceneViewportModel.getViewingDirection(normalizedPosition.x, normalizedPosition.y);

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
        if ("IBRObject".equals(sceneViewportModel.getObjectAtCoordinates(normalizedPosition.x, normalizedPosition.y)))
        {
            System.out.println("Valid object");

            lightingModel.setLightCenter(lightIndex, sceneViewportModel.get3DPositionAtCoordinates(normalizedPosition.x, normalizedPosition.y));
        }
        else
        {
            System.out.println("INVALID OBJECT");
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xPos, double yPos)
    {
        if (updateFunction == null)
        {
            orbitFallbackTool.cursorMoved(window, xPos, yPos);
            updateForHoverState(window, xPos, yPos);
        }
        else if (window.getMouseButtonState(0) == MouseButtonState.Pressed)
        {
            System.out.println("Move: " + LocalTime.now());

            WindowSize windowSize = window.getWindowSize();
            updateFunction.accept(new DoubleVector2(xPos / windowSize.width, yPos / windowSize.height));

            System.out.println("Update finished: " + LocalTime.now());
        }
    }
}