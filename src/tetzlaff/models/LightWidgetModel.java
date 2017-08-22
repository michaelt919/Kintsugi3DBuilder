package tetzlaff.models;

public interface LightWidgetModel extends ReadonlyLightWidgetModel
{
    void setWidgetsEnabled(boolean widgetsEnabled);

    void setAzimuthWidgetVisible(boolean azimuthWidgetVisible);
    void setAzimuthWidgetSelected(boolean azimuthWidgetSelected);

    void setInclinationWidgetVisible(boolean inclinationWidgetVisible);
    void setInclinationWidgetSelected(boolean inclinationWidgetSelected);

    void setDistanceWidgetVisible(boolean distanceWidgetVisible);
    void setDistanceWidgetSelected(boolean distanceWidgetSelected);

    void setCenterWidgetVisible(boolean centerWidgetVisible);
    void setCenterWidgetSelected(boolean centerWidgetSelected);
}
