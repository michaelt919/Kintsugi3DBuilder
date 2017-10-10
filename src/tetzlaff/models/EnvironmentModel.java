package tetzlaff.models;

public interface EnvironmentModel extends ReadonlyEnvironmentModel
{
    void setEnvironmentRotation(float environmentRotation);
    void setEnvironmentIntensity(float environmentIntensity);
    void setBackgroundIntensity(float backgroundIntensity);
}
