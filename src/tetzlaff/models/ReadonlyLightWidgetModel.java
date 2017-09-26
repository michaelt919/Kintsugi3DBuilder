package tetzlaff.models;

public interface ReadonlyLightWidgetModel
{
    boolean areWidgetsEnabled();

    boolean isAzimuthWidgetVisible();
    boolean isAzimuthWidgetSelected();

    boolean isInclinationWidgetVisible();
    boolean isInclinationWidgetSelected();

    boolean isDistanceWidgetVisible();
    boolean isDistanceWidgetSelected();

    boolean isCenterWidgetVisible();
    boolean isCenterWidgetSelected();
}
