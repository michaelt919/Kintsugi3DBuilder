package tetzlaff.ibr.tools;

import java.util.function.Consumer;

import tetzlaff.gl.vecmath.DoubleVector2;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.*;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ExtendedLightingModel;
import tetzlaff.models.ReadonlyEnvironmentMapModel;
import tetzlaff.models.SceneViewportModel;

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

                    if (nameParts.length > 2)
                    {
                        switch (nameParts[2])
                        {
                            case "Azimuth":
                                updateFunction = this::updateAzimuth;
                                azimuthOffset = lightingModel.getLight(lightIndex).getAzimuth() * (float)Math.PI / 180
                                    - getAzimuthAtWindowPosition(new DoubleVector2(normalizedX, normalizedY));
                                break;
                            case "Inclination":
                                updateFunction = this::updateInclination;
                                inclinationOffset = lightingModel.getLight(lightIndex).getInclination() * (float)Math.PI / 180
                                    - getInclinationAtWindowPosition(new DoubleVector2(normalizedX, normalizedY));
                                break;
                            case "Distance":
                                updateFunction = this::updateDistance;
                                distanceOffset = lightingModel.getLight(lightIndex).getDistance()
                                    - getDistanceAtWindowPosition(new DoubleVector2(normalizedX, normalizedY));
                                break;
                            case "Center":
                                updateFunction = this::updateCenter;
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }

        if (updateFunction == null)
        {
            orbitFallbackTool.mouseButtonPressed(window, buttonIndex, mods);
        }
    }

    private float getAzimuthAtWindowPosition(DoubleVector2 normalizedPosition)
    {
        Vector3 viewportCenter = sceneViewportModel.getViewportCenter();
        Vector3 cursorDirection = sceneViewportModel.getViewingDirection(normalizedPosition.x, normalizedPosition.y);

        Vector3 azimuthCenter = lightingModel.getLightCenter(lightIndex)
            .plus(new Vector3(0, (float)Math.sin(lightingModel.getLight(lightIndex).getInclination()), 0));

        float t = (azimuthCenter.y - viewportCenter.y) / cursorDirection.y;
        Vector3 newAzimuthDirection = viewportCenter.plus(cursorDirection.times(t)).minus(azimuthCenter);
        Vector2 newAzimuthDirectionNormalized = new Vector2(newAzimuthDirection.x, newAzimuthDirection.z).normalized();

        return (float)Math.atan2(newAzimuthDirectionNormalized.y, newAzimuthDirectionNormalized.x);
    }

    private float getInclinationAtWindowPosition(DoubleVector2 normalizedPosition)
    {
        Vector3 viewportCenter = sceneViewportModel.getViewportCenter();
        Vector3 cursorDirection = sceneViewportModel.getViewingDirection(normalizedPosition.x, normalizedPosition.y);

        Vector3 lightCenter = lightingModel.getLightCenter(lightIndex);
        Vector3 lightDisplacement = lightingModel.getLightMatrix(lightIndex).quickInverse(0.01f).getColumn(3).getXYZ()
                                    .minus(lightingModel.getLightCenter(lightIndex));

        Vector3 lightDirectionProjected = new Vector3(lightDisplacement.x, 0, lightDisplacement.z).normalized();
        Vector3 inclinationAxis = new Vector3(-lightDisplacement.z, 0, lightDisplacement.x).normalized();

        float t = lightCenter.minus(viewportCenter).dot(inclinationAxis) / cursorDirection.dot(inclinationAxis);
        Vector3 newLightDirection = viewportCenter.plus(cursorDirection.times(t)).minus(lightCenter).normalized();

        return newLightDirection.dot(lightDirectionProjected) < 0.0f ? Math.signum(newLightDirection.y) * (float)Math.PI / 2
            : (float)(Math.acos(newLightDirection.y) - Math.PI / 2);
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
        lightingModel.getLight(lightIndex).setInclination((inclinationOffset + getInclinationAtWindowPosition(normalizedPosition)) * 180 / (float)Math.PI);
    }

    private void updateDistance(DoubleVector2 normalizedPosition)
    {
        lightingModel.getLight(lightIndex).setDistance(Math.max(0.001f, distanceOffset + getDistanceAtWindowPosition(normalizedPosition)));
    }

    private void updateCenter(DoubleVector2 normalizedPosition)
    {
        if ("IBRObject".equals(sceneViewportModel.getObjectAtCoordinates(normalizedPosition.x, normalizedPosition.y)))
        {
            lightingModel.setLightCenter(lightIndex, sceneViewportModel.get3DPositionAtCoordinates(normalizedPosition.x, normalizedPosition.y));
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xPos, double yPos)
    {
        if (updateFunction == null)
        {
            orbitFallbackTool.cursorMoved(window, xPos, yPos);
        }
        else if (window.getMouseButtonState(0) == MouseButtonState.Pressed)
        {
            WindowSize windowSize = window.getWindowSize();
            updateFunction.accept(new DoubleVector2(xPos / windowSize.width, yPos / windowSize.height));
        }
    }
}
