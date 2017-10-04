package tetzlaff.models;

public interface ExtendedLightingModel extends LightingModel, ReadonlyExtendedLightingModel
{
    void setSelectedLightIndex(int index);

    @Override
    LightInstanceModel getLight(int index);

    @Override
    EnvironmentModel getEnvironmentModel();
}
