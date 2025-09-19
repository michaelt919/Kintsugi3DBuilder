package kintsugi3d.builder.state.project;

import javafx.scene.paint.Color;

import java.io.File;

public interface EnvironmentSettings
{
    boolean isNoEnvironment();

    void setIsNoEnvironment(boolean isNoEnvironment);

    boolean isEnvironmentImageEnabled();

    void setEnvironmentImageEnabled(boolean envUseImage);

    boolean isEnvironmentColorEnabled();

    void setEnvironmentColorEnabled(boolean envUseColor);

    boolean isBackplateImageEnabled();

    void setBackplateImageEnabled(boolean bpUseImage);

    boolean isBackplateColorEnabled();

    void setBackplateColorEnabled(boolean bpUseColor);

    boolean areImagePathsRelative();

    void setImagePathsRelative(boolean imagePathsRelative);

    File getEnvironmentImageFile();

    void setEnvironmentImageFile(File envImageFile);

    File getBackplateImageFile();

    void setBackplateImageFile(File bpImageFile);

    double getBackgroundIntensity();

    void setBackgroundIntensity(double backgroundIntensity);

    double getEnvironmentColorIntensity();

    void setEnvironmentIntensity(double envColorIntensity);

    double getEnvironmentRotation();

    void setEnvironmentRotation(double envRotation);

    double getEnvironmentFilteringBias();

    void setEnvironmentFilteringBias(double envFilteringBias);

    Color getEnvironmentColor();

    void setEnvironmentColor(Color envColor);

    Color getBackplateColor();

    void setBackplateColor(Color bpColor);

    String getName();

    void setName(String name);

    boolean isLocked();

    void setLocked(boolean locked);

    boolean isEnvironmentLoaded();

    void setEnvironmentLoaded(boolean envLoaded);

    boolean isBackplateLoaded();

    void setBackplateLoaded(boolean bpLoaded);

    boolean isGroundPlaneEnabled();

    void setGroundPlaneEnabled(boolean gpEnabled);

    Color getGroundPlaneCrolor();

    void setGroundPlaneColor(Color gpColor);

    double getGroundPlaneHeight();

    void setGroundPlaneHeight(double gpHeight);

    double getGroundPlaneSize();

    void setGroundPlaneSize(double gpSize);
}
