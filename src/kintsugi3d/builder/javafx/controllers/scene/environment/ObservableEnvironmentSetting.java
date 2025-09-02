/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.scene.environment;

import javafx.beans.property.*;
import javafx.scene.paint.Color;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.builder.state.project.EnvironmentSetting;

import java.io.File;

public class ObservableEnvironmentSetting extends EnvironmentSetting
{
    private final BooleanProperty envUseImage = new SimpleBooleanProperty();
    private final BooleanProperty envUseColor = new SimpleBooleanProperty(true);
    private final BooleanProperty bpUseImage = new SimpleBooleanProperty();
    private final BooleanProperty bpUseColor = new SimpleBooleanProperty();
    private final BooleanProperty imagePathsRelative = new SimpleBooleanProperty();

    private final Property<File> envImageFile = new SimpleObjectProperty<>();
    private final Property<File> bpImageFile = new SimpleObjectProperty<>();

    private final DoubleProperty envColorIntensity = StaticUtilities.clamp(0, Double.MAX_VALUE, new SimpleDoubleProperty(1.0));
    private final DoubleProperty backgroundIntensity = StaticUtilities.clamp(0, Double.MAX_VALUE, new SimpleDoubleProperty(1.0));
    private final DoubleProperty envRotation = StaticUtilities.wrapAround(-180, 180, new SimpleDoubleProperty());
    private final DoubleProperty envFilteringBias = new SimpleDoubleProperty(0);
    private final Property<Color> envColor = new SimpleObjectProperty<>(Color.valueOf("#222222"));
    private final Property<Color> bpColor = new SimpleObjectProperty<>(Color.valueOf("#222222"));
    private final StringProperty name = new SimpleStringProperty("New Environment Map");
    private final BooleanProperty locked = new SimpleBooleanProperty();
    private final BooleanProperty envLoaded = new SimpleBooleanProperty();
    private final BooleanProperty bpLoaded = new SimpleBooleanProperty();

    // ground plane
    private final BooleanProperty gpEnabled = new SimpleBooleanProperty();
    private final Property<Color> gpColor = new SimpleObjectProperty<>(Color.WHITE);
    private final DoubleProperty gpHeight = new SimpleDoubleProperty();
    private final DoubleProperty gpSize = new SimpleDoubleProperty(1.0);

    @Override
    public String toString()
    {
        if (!this.isNoEnvironment() && locked.getValue())
        {
            return "(L) " + this.name.getValue();
        }
        else
        {
            return this.name.getValue();
        }
    }

    public ObservableEnvironmentSetting duplicate()
    {
        ObservableEnvironmentSetting newEnvironment = new ObservableEnvironmentSetting();
        newEnvironment.envUseImage.setValue(envUseImage.getValue());
        newEnvironment.envUseColor.setValue(envUseColor.getValue());
        newEnvironment.bpUseImage.setValue(bpUseImage.getValue());
        newEnvironment.bpUseColor.setValue(bpUseColor.getValue());
        newEnvironment.imagePathsRelative.setValue(imagePathsRelative.getValue());
        newEnvironment.envImageFile.setValue(envImageFile.getValue());
        newEnvironment.bpImageFile.setValue(bpImageFile.getValue());
        newEnvironment.backgroundIntensity.setValue(backgroundIntensity.getValue());
        newEnvironment.envColorIntensity.setValue(envColorIntensity.getValue());
        newEnvironment.envRotation.setValue(envRotation.getValue());
        newEnvironment.envFilteringBias.setValue(envFilteringBias.getValue());
        newEnvironment.envColor.setValue(envColor.getValue());
        newEnvironment.bpColor.setValue(bpColor.getValue());
        newEnvironment.name.setValue(name.getValue() + " copy");
        newEnvironment.locked.setValue(locked.getValue());
        newEnvironment.envLoaded.setValue(envLoaded.getValue());
        newEnvironment.bpLoaded.setValue(bpLoaded.getValue());
        newEnvironment.gpEnabled.setValue(gpEnabled.getValue());
        newEnvironment.gpColor.setValue(gpColor.getValue());
        newEnvironment.gpHeight.setValue(gpHeight.getValue());
        newEnvironment.gpSize.setValue(gpSize.getValue());
        return newEnvironment;
    }

    @Override
    public boolean isEnvironmentImageEnabled()
    {
        return envUseImage.get();
    }

    public BooleanProperty envUseImageProperty()
    {
        return envUseImage;
    }

    @Override
    public void setEnvironmentImageEnabled(boolean envUseImage)
    {
        this.envUseImage.set(envUseImage);
    }

    @Override
    public boolean isEnvironmentColorEnabled()
    {
        return envUseColor.get();
    }

    public BooleanProperty envUseColorProperty()
    {
        return envUseColor;
    }

    @Override
    public void setEnvironmentColorEnabled(boolean envUseColor)
    {
        this.envUseColor.set(envUseColor);
    }

    @Override
    public boolean isBackplateImageEnabled()
    {
        return bpUseImage.get();
    }

    public BooleanProperty bpUseImageProperty()
    {
        return bpUseImage;
    }

    @Override
    public void setBackplateImageEnabled(boolean bpUseImage)
    {
        this.bpUseImage.set(bpUseImage);
    }

    @Override
    public boolean isBackplateColorEnabled()
    {
        return bpUseColor.get();
    }

    public BooleanProperty bpUseColorProperty()
    {
        return bpUseColor;
    }

    @Override
    public void setBackplateColorEnabled(boolean bpUseColor)
    {
        this.bpUseColor.set(bpUseColor);
    }

    @Override
    public boolean areImagePathsRelative()
    {
        return imagePathsRelative.get();
    }

    public BooleanProperty imagePathsRelativeProperty()
    {
        return imagePathsRelative;
    }

    @Override
    public void setImagePathsRelative(boolean imagePathsRelative)
    {
        this.imagePathsRelative.set(imagePathsRelative);
    }

    @Override
    public File getEnvironmentImageFile()
    {
        return envImageFile.getValue();
    }

    public Property<File> envImageFileProperty()
    {
        return envImageFile;
    }

    @Override
    public void setEnvironmentImageFile(File envImageFile)
    {
        this.envImageFile.setValue(envImageFile);
    }

    @Override
    public File getBackplateImageFile()
    {
        return bpImageFile.getValue();
    }

    public Property<File> bpImageFileProperty()
    {
        return bpImageFile;
    }

    @Override
    public void setBackplateImageFile(File bpImageFile)
    {
        this.bpImageFile.setValue(bpImageFile);
    }

    @Override
    public double getBackgroundIntensity()
    {
        return backgroundIntensity.get();
    }

    public DoubleProperty backgroundIntensityProperty()
    {
        return backgroundIntensity;
    }

    @Override
    public void setBackgroundIntensity(double backgroundIntensity)
    {
        this.backgroundIntensity.set(backgroundIntensity);
    }

    @Override
    public double getEnvironmentColorIntensity()
    {
        return envColorIntensity.get();
    }

    public DoubleProperty envColorIntensityProperty()
    {
        return envColorIntensity;
    }

    @Override
    public void setEnvironmentIntensity(double envColorIntensity)
    {
        this.envColorIntensity.set(envColorIntensity);
    }

    @Override
    public double getEnvironmentRotation()
    {
        return envRotation.get();
    }

    public DoubleProperty envRotationProperty()
    {
        return envRotation;
    }

    @Override
    public void setEnvironmentRotation(double envRotation)
    {
        this.envRotation.set(envRotation);
    }

    public DoubleProperty envFilteringBiasProperty()
    {
        return envFilteringBias;
    }

    @Override
    public double getEnvironmentFilteringBias()
    {
        return envFilteringBias.get();
    }

    @Override
    public void setEnvironmentFilteringBias(double envFilteringBias)
    {
        this.envFilteringBias.set(envFilteringBias);
    }

    @Override
    public Color getEnvironmentColor()
    {
        return envColor.getValue();
    }

    public Property<Color> envColorProperty()
    {
        return envColor;
    }

    @Override
    public void setEnvironmentColor(Color envColor)
    {
        this.envColor.setValue(envColor);
    }

    @Override
    public Color getBackplateColor()
    {
        return bpColor.getValue();
    }

    public Property<Color> bpColorProperty()
    {
        return bpColor;
    }

    @Override
    public void setBackplateColor(Color bpColor)
    {
        this.bpColor.setValue(bpColor);
    }

    @Override
    public String getName()
    {
        return name.get();
    }

    public StringProperty nameProperty()
    {
        return name;
    }

    @Override
    public void setName(String name)
    {
        this.name.set(name);
    }

    @Override
    public boolean isLocked()
    {
        return locked.get();
    }

    public BooleanProperty lockedProperty()
    {
        return locked;
    }

    @Override
    public void setLocked(boolean locked)
    {
        this.locked.set(locked);
    }

    @Override
    public boolean isEnvironmentLoaded()
    {
        return envLoaded.get();
    }

    public BooleanProperty envLoadedProperty()
    {
        return envLoaded;
    }

    @Override
    public void setEnvironmentLoaded(boolean envLoaded)
    {
        this.envLoaded.set(envLoaded);
    }

    @Override
    public boolean isBackplateLoaded()
    {
        return bpLoaded.get();
    }

    public BooleanProperty bpLoadedProperty()
    {
        return bpLoaded;
    }

    @Override
    public void setBackplateLoaded(boolean bpLoaded)
    {
        this.bpLoaded.set(bpLoaded);
    }

    @Override
    public boolean isGroundPlaneEnabled()
    {
        return gpEnabled.get();
    }

    public BooleanProperty gpEnabledProperty()
    {
        return gpEnabled;
    }

    @Override
    public void setGroundPlaneEnabled(boolean gpEnabled)
    {
        this.gpEnabled.set(gpEnabled);
    }

    @Override
    public Color getGroundPlaneCrolor()
    {
        return gpColor.getValue();
    }

    public Property<Color> gpColorProperty()
    {
        return gpColor;
    }

    @Override
    public void setGroundPlaneColor(Color gpColor)
    {
        this.gpColor.setValue(gpColor);
    }

    @Override
    public double getGroundPlaneHeight()
    {
        return gpHeight.getValue();
    }

    public DoubleProperty gpHeightProperty()
    {
        return gpHeight;
    }

    @Override
    public void setGroundPlaneHeight(double gpHeight)
    {
        this.gpHeight.setValue(gpHeight);
    }

    @Override
    public double getGroundPlaneSize()
    {
        return gpSize.getValue();
    }

    public DoubleProperty gpSizeProperty()
    {
        return gpSize;
    }

    @Override
    public void setGroundPlaneSize(double gpSize)
    {
        this.gpSize.setValue(gpSize);
    }
}
