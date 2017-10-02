package tetzlaff.models;

public interface EnvironmentModel extends ReadonlyEnvironmentModel
{
    void setEnvironmentRotation(double environmentRotation);
    void setEnvironmentIntensity(double environmentIntensity);
}
