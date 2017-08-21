package tetzlaff.models;

public interface ExtendedLightingModel extends LightingModel, ReadonlyExtendedLightingModel
{
    @Override
    LightInstanceModel getLight(int index);
}
