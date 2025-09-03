package kintsugi3d.builder.state.project;

import javafx.scene.paint.Color;

public interface LightSettings
{
    boolean isGroupLocked();

    double getTargetX();

    double getTargetY();

    double getTargetZ();

    double getAzimuth();

    double getInclination();

    double getLog10Distance();

    double getIntensity();

    boolean isLocked();

    String getName();

    double getSpotSize();

    double getSpotTaper();

    Color getColor();

    void setTargetX(double value);

    void setTargetY(double value);

    void setTargetZ(double value);

    void setAzimuth(double value);

    void setInclination(double value);

    void setLog10Distance(double value);

    void setIntensity(double value);

    void setLocked(boolean value);

    void setName(String value);

    void setSpotSize(double value);

    void setSpotTaper(double value);

    void setColor(Color value);
}
