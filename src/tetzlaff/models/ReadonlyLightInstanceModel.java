package tetzlaff.models;

public interface ReadonlyLightInstanceModel extends ReadonlyCameraModel, ReadonlyLightPrototypeModel
{
    boolean isEnabled();
}
