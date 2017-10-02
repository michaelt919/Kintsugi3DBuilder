package tetzlaff.ibr.javafx.models;

import javafx.application.Platform;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.LightInstanceModel;
import tetzlaff.models.impl.LightInstanceModelBase;

public class MultithreadLightInstanceModel extends LightInstanceModelBase
{
    private final LightInstanceModel baseModel;

    public MultithreadLightInstanceModel(LightInstanceModel baseModel)
    {
        this.baseModel = baseModel;
    }

    @Override
    public boolean isLocked()
    {
        return baseModel.isLocked();
    }

    @Override
    public boolean isEnabled()
    {
        return baseModel.isEnabled();
    }

    @Override
    public void setTarget(Vector3 target)
    {
        super.setTarget(target);
        Platform.runLater(() -> baseModel.setTarget(target));
    }

    @Override
    public void setAzimuth(float azimuth)
    {
        super.setAzimuth(azimuth);
        Platform.runLater(() -> baseModel.setAzimuth(azimuth));
    }

    @Override
    public void setInclination(float inclination)
    {
        super.setInclination(inclination);
        Platform.runLater(() -> baseModel.setInclination(inclination));
    }

    @Override
    public void setLog10Distance(float log10Distance)
    {
        super.setLog10Distance(log10Distance);
        Platform.runLater(() -> baseModel.setLog10Distance(log10Distance));
    }

    @Override
    public void setTwist(float twist)
    {
        super.setTwist(twist);
        Platform.runLater(() -> baseModel.setTwist(twist));
    }

    @Override
    public void setHorizontalFOV(float fov)
    {
        super.setHorizontalFOV(fov);
        Platform.runLater(() -> baseModel.setHorizontalFOV(fov));
    }

    @Override
    public void setFocalLength(float focalLength)
    {
        super.setFocalLength(focalLength);
        Platform.runLater(() -> baseModel.setFocalLength(focalLength));
    }

    @Override
    public void setOrthographic(boolean orthographic)
    {
        super.setOrthographic(orthographic);
        Platform.runLater(() -> baseModel.setOrthographic(orthographic));
    }

    @Override
    public void setColor(Vector3 color)
    {
        super.setColor(color);
        Platform.runLater(() -> baseModel.setColor(color));
    }
}
