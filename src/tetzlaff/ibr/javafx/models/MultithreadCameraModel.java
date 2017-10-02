package tetzlaff.ibr.javafx.models;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.application.Platform;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.impl.ExtendedCameraModelBase;

public class MultithreadCameraModel extends ExtendedCameraModelBase
{
    private final ExtendedCameraModel baseModel;

    private Vector3 targetOverride;
    private Float azimuthOverride;
    private Float inclinationOverride;
    private Float log10DistanceOverride;
    private Float twistOverride;
    private Float horizontalFOVOverride;
    private Boolean orthographicOverride;

    private final Object overrideLock = new Object();

    public MultithreadCameraModel(ExtendedCameraModel baseModel)
    {
        this.baseModel = baseModel;
    }

    @Override
    public boolean isLocked()
    {
        return baseModel.isLocked();
    }

    private <T> void synchronizedUpdate(Consumer<T> overrideUpdate, Supplier<T> overrideCheck, Consumer<T> baseUpdate, T newValue)
    {
        synchronized (overrideLock)
        {
            overrideUpdate.accept(newValue);
        }

        Platform.runLater(() ->
        {
            synchronized(overrideLock)
            {
                if (Objects.equals(newValue, overrideCheck.get()))
                {
                    baseUpdate.accept(newValue);
                    overrideUpdate.accept(null);
                }
            }
        });
    }

    @Override
    public Vector3 getTarget()
    {
        return targetOverride != null ? targetOverride : baseModel.getTarget();
    }

    @Override
    public void setTarget(Vector3 target)
    {
        synchronizedUpdate(value -> targetOverride = value, () -> targetOverride, baseModel::setTarget, target);
    }

    @Override
    public boolean isOrthographic()
    {
        return false;
    }

    @Override
    public void setOrthographic(boolean orthographic)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getHorizontalFOV()
    {
        return 0;
    }

    @Override
    public void setHorizontalFOV(float fov)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getLog10Distance()
    {
        return 0;
    }

    @Override
    public void setLog10Distance(float log10Distance)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getTwist()
    {
        return 0;
    }

    @Override
    public void setTwist(float twist)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getAzimuth()
    {
        return 0;
    }

    @Override
    public void setAzimuth(float azimuth)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getInclination()
    {
        return 0;
    }

    @Override
    public void setInclination(float inclination)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getFocalLength()
    {
        return 0;
    }

    @Override
    public void setFocalLength(float focalLength)
    {
        throw new UnsupportedOperationException();
    }
}
