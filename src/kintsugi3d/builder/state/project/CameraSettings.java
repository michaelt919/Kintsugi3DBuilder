package kintsugi3d.builder.state.project;

public interface CameraSettings
{
    double getXCenter();

    void setXCenter(double xCenter);

    double getYCenter();

    void setYCenter(double yCenter);

    double getZCenter();

    void setZCenter(double zCenter);

    double getAzimuth();

    void setAzimuth(double azimuth);

    double getInclination();

    void setInclination(double inclination);

    double getLog10Distance();

    void setLog10Distance(double log10distance);

    double getTwist();

    void setTwist(double twist);

    double getFOV();

    void setFOV(double fOV);

    double getFocalLength();

    void setFocalLength(double focalLength);

    boolean isLocked();

    void setLocked(boolean locked);

    boolean isOrthographic();

    void setOrthographic(boolean orthographic);

    String getName();

    void setName(String name);
}
