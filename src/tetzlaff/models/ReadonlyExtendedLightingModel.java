package tetzlaff.models;

public interface ReadonlyExtendedLightingModel extends ReadonlyLightingModel
{
    ReadonlyLightInstanceModel getLight(int index);
}
