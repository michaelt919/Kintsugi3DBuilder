/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx.controllers.scene.environment;

import java.io.File;

import javafx.beans.property.*;
import javafx.scene.paint.Color;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import tetzlaff.ibrelight.javafx.util.DOMConvertable;
import tetzlaff.ibrelight.javafx.util.StaticUtilities;

public class EnvironmentSetting implements DOMConvertable
{
    private final BooleanProperty envUseImage = new SimpleBooleanProperty();
    private final BooleanProperty envUseColor = new SimpleBooleanProperty();
    private final BooleanProperty bpUseImage = new SimpleBooleanProperty();
    private final BooleanProperty bpUseColor = new SimpleBooleanProperty();
    private final BooleanProperty imagePathsRelative = new SimpleBooleanProperty();

    private final Property<File> envImageFile = new SimpleObjectProperty<>();
    private final Property<File> bpImageFile = new SimpleObjectProperty<>();

    private final DoubleProperty envColorIntensity = StaticUtilities.clamp(0, Double.MAX_VALUE, new SimpleDoubleProperty(1.0));
    private final DoubleProperty backgroundIntensity = StaticUtilities.clamp(0, Double.MAX_VALUE, new SimpleDoubleProperty(1.0));
    private final DoubleProperty envRotation = StaticUtilities.wrapAround(-180, 180, new SimpleDoubleProperty());
    private final IntegerProperty envFilteringBias = new SimpleIntegerProperty(0);
    private final Property<Color> envColor = new SimpleObjectProperty<>(Color.WHITE);
    private final Property<Color> bpColor = new SimpleObjectProperty<>(Color.WHITE);
    private final StringProperty name = new SimpleStringProperty("New Environment Map");
    private final BooleanProperty locked = new SimpleBooleanProperty();
    private final BooleanProperty envLoaded = new SimpleBooleanProperty();
    private final BooleanProperty bpLoaded = new SimpleBooleanProperty();

    public static final EnvironmentSetting NO_ENVIRONMENT;

    static
    {
        NO_ENVIRONMENT = new EnvironmentSetting();
        NO_ENVIRONMENT.setName("No Environment");
        NO_ENVIRONMENT.setLocked(true);
    }

    @Override
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("Environment");
        element.setAttribute("envUseImage", envUseImage.getValue().toString());
        element.setAttribute("envUseColor", envUseColor.getValue().toString());
        element.setAttribute("bpUseImage", bpUseImage.getValue().toString());
        element.setAttribute("bpUseColor", bpUseColor.getValue().toString());
        element.setAttribute("imagePathsRelative", imagePathsRelative.getValue().toString());
        if (envImageFile.getValue() != null)
        {
            element.setAttribute("envImageFile", envImageFile.getValue().getPath());
        }
        if (bpImageFile.getValue() != null)
        {
            element.setAttribute("bpImageFile", bpImageFile.getValue().getPath());
        }
        element.setAttribute("backgroundIntensity", backgroundIntensity.getValue().toString());
        element.setAttribute("envColorIntensity", envColorIntensity.getValue().toString());
        element.setAttribute("envRotation", envRotation.getValue().toString());
        element.setAttribute("envFilteringBias", envFilteringBias.getValue().toString());
        element.setAttribute("envColor", envColor.getValue().toString());
        element.setAttribute("bpColor", bpColor.getValue().toString());
        element.setAttribute("name", name.getValue());
        element.setAttribute("locked", locked.getValue().toString());
        element.setAttribute("envLoaded", envLoaded.getValue().toString());
        element.setAttribute("bpLoaded", bpLoaded.getValue().toString());
        return element;
    }

    public static EnvironmentSetting fromDOMElement(Element element)
    {
        EnvironmentSetting newEnvironment = new EnvironmentSetting();
        newEnvironment.envUseImage.setValue(Boolean.valueOf(element.getAttribute("envUseImage")));
        newEnvironment.envUseColor.setValue(Boolean.valueOf(element.getAttribute("envUseColor")));
        newEnvironment.bpUseImage.setValue(Boolean.valueOf(element.getAttribute("bpUseImage")));
        newEnvironment.bpUseColor.setValue(Boolean.valueOf(element.getAttribute("bpUseColor")));
        newEnvironment.imagePathsRelative.setValue(Boolean.valueOf(element.getAttribute("imagePathsRelative")));
        newEnvironment.envImageFile.setValue(element.hasAttribute("envImageFile") ?
            new File(element.getAttribute("envImageFile")) : null);
        newEnvironment.bpImageFile.setValue(element.hasAttribute("bpImageFile") ?
            new File(element.getAttribute("bpImageFile")) : null);
        newEnvironment.backgroundIntensity.setValue(Double.valueOf(element.getAttribute("backgroundIntensity")));
        newEnvironment.envColorIntensity.setValue(Double.valueOf(element.getAttribute("envColorIntensity")));
        newEnvironment.envRotation.setValue(Double.valueOf(element.getAttribute("envRotation")));
        newEnvironment.envColor.setValue(Color.valueOf(element.getAttribute("envColor")));
        newEnvironment.envFilteringBias.setValue(element.hasAttribute("envFilteringBias") ?
            Integer.valueOf(element.getAttribute("envFilteringBias")) : 0);
        newEnvironment.bpColor.setValue(Color.valueOf(element.getAttribute("bpColor")));
        newEnvironment.name.setValue(String.valueOf(element.getAttribute("name")));
        newEnvironment.locked.setValue(Boolean.valueOf(element.getAttribute("locked")));
        newEnvironment.envLoaded.setValue(Boolean.valueOf(element.getAttribute("envLoaded")));
        newEnvironment.bpLoaded.setValue(Boolean.valueOf(element.getAttribute("bpLoaded")));
        return newEnvironment;
    }

    @Override
    public String toString()
    {
        if (!this.equals(NO_ENVIRONMENT) && locked.getValue())
        {
            return "(L) " + this.name.getValue();
        }
        else
        {
            return this.name.getValue();
        }
    }

    public EnvironmentSetting duplicate()
    {
        EnvironmentSetting newEnvironment = new EnvironmentSetting();
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
        return newEnvironment;
    }

    public boolean isEnvUseImageEnabled()
    {
        return envUseImage.get();
    }

    public BooleanProperty envUseImageProperty()
    {
        return envUseImage;
    }

    public void setEnvUseImageEnabled(boolean envUseImage)
    {
        this.envUseImage.set(envUseImage);
    }

    public boolean isEnvUseColorEnabled()
    {
        return envUseColor.get();
    }

    public BooleanProperty envUseColorProperty()
    {
        return envUseColor;
    }

    public void setEnvUseColorEnabled(boolean envUseColor)
    {
        this.envUseColor.set(envUseColor);
    }

    public boolean isBPUseImageEnabled()
    {
        return bpUseImage.get();
    }

    public BooleanProperty bpUseImageProperty()
    {
        return bpUseImage;
    }

    public void setBpUseImageEnabled(boolean bpUseImage)
    {
        this.bpUseImage.set(bpUseImage);
    }

    public boolean isBPUseColorEnabled()
    {
        return bpUseColor.get();
    }

    public BooleanProperty bpUseColorProperty()
    {
        return bpUseColor;
    }

    public void setBpUseColorEnabled(boolean bpUseColor)
    {
        this.bpUseColor.set(bpUseColor);
    }

    public boolean areImagePathsRelative()
    {
        return imagePathsRelative.get();
    }

    public BooleanProperty imagePathsRelativeProperty()
    {
        return imagePathsRelative;
    }

    public void setImagePathsRelative(boolean imagePathsRelative)
    {
        this.imagePathsRelative.set(imagePathsRelative);
    }

    public File getEnvImageFile()
    {
        return envImageFile.getValue();
    }

    public Property<File> envImageFileProperty()
    {
        return envImageFile;
    }

    public void setEnvImageFile(File envImageFile)
    {
        this.envImageFile.setValue(envImageFile);
    }

    public File getBpImageFile()
    {
        return bpImageFile.getValue();
    }

    public Property<File> bpImageFileProperty()
    {
        return bpImageFile;
    }

    public void setBpImageFile(File bpImageFile)
    {
        this.bpImageFile.setValue(bpImageFile);
    }

    public double getBackgroundIntensity()
    {
        return backgroundIntensity.get();
    }

    public DoubleProperty backgroundIntensityProperty()
    {
        return backgroundIntensity;
    }

    public void setBackgroundIntensity(double backgroundIntensity)
    {
        this.backgroundIntensity.set(backgroundIntensity);
    }

    public double getEnvIntensity()
    {
        return envColorIntensity.get();
    }

    public DoubleProperty envColorIntensityProperty()
    {
        return envColorIntensity;
    }

    public void setEnvIntensity(double envColorIntensity)
    {
        this.envColorIntensity.set(envColorIntensity);
    }

    public double getEnvRotation()
    {
        return envRotation.get();
    }

    public DoubleProperty envRotationProperty()
    {
        return envRotation;
    }

    public void setEnvRotation(double envRotation)
    {
        this.envRotation.set(envRotation);
    }

    public IntegerProperty envFilteringBiasProperty()
    {
        return envFilteringBias;
    }

    public int getEnvFilteringBias()
    {
        return envFilteringBias.get();
    }

    public void setEnvFilteringBias(int envFilteringBias)
    {
        this.envFilteringBias.set(envFilteringBias);
    }

    public Color getEnvColor()
    {
        return envColor.getValue();
    }

    public Property<Color> envColorProperty()
    {
        return envColor;
    }

    public void setEnvColor(Color envColor)
    {
        this.envColor.setValue(envColor);
    }

    public Color getBpColor()
    {
        return bpColor.getValue();
    }

    public Property<Color> bpColorProperty()
    {
        return bpColor;
    }

    public void setBpColor(Color bpColor)
    {
        this.bpColor.setValue(bpColor);
    }

    public String getName()
    {
        return name.get();
    }

    public StringProperty nameProperty()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name.set(name);
    }

    public boolean isLocked()
    {
        return locked.get();
    }

    public BooleanProperty lockedProperty()
    {
        return locked;
    }

    public void setLocked(boolean locked)
    {
        this.locked.set(locked);
    }

    public boolean isEnvLoaded()
    {
        return envLoaded.get();
    }

    public BooleanProperty envLoadedProperty()
    {
        return envLoaded;
    }

    public void setEnvLoaded(boolean envLoaded)
    {
        this.envLoaded.set(envLoaded);
    }

    public boolean isBPLoaded()
    {
        return bpLoaded.get();
    }

    public BooleanProperty bpLoadedProperty()
    {
        return bpLoaded;
    }

    public void setBPLoaded(boolean bpLoaded)
    {
        this.bpLoaded.set(bpLoaded);
    }
}
