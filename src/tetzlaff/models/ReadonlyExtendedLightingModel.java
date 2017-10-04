package tetzlaff.models;

public interface ReadonlyExtendedLightingModel extends ReadonlyLightingModel
{
    int getSelectedLightIndex();
    ReadonlyLightInstanceModel getLight(int index);
    ReadonlyEnvironmentModel getEnvironmentModel();

    default ReadonlyLightInstanceModel getSelectedLight()
    {
        return getLight(getSelectedLightIndex());
    }
}
