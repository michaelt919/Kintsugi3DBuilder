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

    private final DoubleProperty envColorIntensity = StaticUtilities.clamp(0, Double.MAX_VALUE, new SimpleDoubleProperty());
    private final DoubleProperty envRotation = StaticUtilities.wrapAround(-180, 180, new SimpleDoubleProperty());
    private final Property<Color> envColor = new SimpleObjectProperty<>();
    private final Property<Color> bpColor = new SimpleObjectProperty<>();
    private final StringProperty name = new SimpleStringProperty();
    private final BooleanProperty locked = new SimpleBooleanProperty();
    private final BooleanProperty firstEnvLoaded = new SimpleBooleanProperty();

    public EnvironmentSetting(Boolean envUseImage, Boolean envUseColor, Boolean bpUseImage, Boolean bpUseColor, Boolean imagePathsRelative, File envImageFile, File bpImageFile, Double envColorIntensity, Double envRotation, Color envColor, Color bpColor, String name, Boolean locked, Boolean firstEnvLoaded)
    {
        this.envUseImage.setValue(envUseImage);
        this.envUseColor.setValue(envUseColor);
        this.bpUseImage.setValue(bpUseImage);
        this.bpUseColor.setValue(bpUseColor);
        this.imagePathsRelative.setValue(imagePathsRelative);
        this.envImageFile.setValue(envImageFile);
        this.bpImageFile.setValue(bpImageFile);
        this.envColorIntensity.setValue(envColorIntensity);
        this.envRotation.setValue(envRotation);
        this.envColor.setValue(envColor);
        this.bpColor.setValue(bpColor);
        this.name.setValue(name);
        this.locked.setValue(locked);
        this.firstEnvLoaded.setValue(firstEnvLoaded);
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
        element.setAttribute("envColorIntensity", envColorIntensity.getValue().toString());
        element.setAttribute("envRotation", envRotation.getValue().toString());
        element.setAttribute("envColor", envColor.getValue().toString());
        element.setAttribute("bpColor", bpColor.getValue().toString());
        element.setAttribute("name", name.getValue());
        element.setAttribute("locked", locked.getValue().toString());
        element.setAttribute("firstEnvLoaded", firstEnvLoaded.getValue().toString());
        return element;
    }

    public static EnvironmentSetting fromDOMElement(Element element)
    {
        return new EnvironmentSetting(
            Boolean.valueOf(element.getAttribute("envUseImage")),
            Boolean.valueOf(element.getAttribute("envUseColor")),
            Boolean.valueOf(element.getAttribute("bpUseImage")),
            Boolean.valueOf(element.getAttribute("bpUseColor")),
            Boolean.valueOf(element.getAttribute("imagePathsRelative")),

            new File(element.getAttribute("envImageFile")),
            new File(element.getAttribute("bpImageFile")),

            Double.valueOf(element.getAttribute("envColorIntensity")),
            Double.valueOf(element.getAttribute("envRotation")),
            Color.valueOf(element.getAttribute("envColor")),
            Color.valueOf(element.getAttribute("bpColor")),
            String.valueOf(element.getAttribute("name")),
            Boolean.valueOf(element.getAttribute("locked")),
            Boolean.valueOf(element.getAttribute("firstEnvLoaded"))
        );
    }

    @Override
    public String toString()
    {
        if (locked.getValue())
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
        return new EnvironmentSetting(
            envUseImage.getValue(),
            envUseColor.getValue(),
            bpUseImage.getValue(),
            bpUseColor.getValue(),
            imagePathsRelative.getValue(),
            envImageFile.getValue(),
            bpImageFile.getValue(),
            envColorIntensity.getValue(),
            envRotation.getValue(),
            envColor.getValue(),
            bpColor.getValue(),
            name.getValue() + " copy",
            locked.getValue(),
            firstEnvLoaded.getValue()
        );
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

    public boolean isBpUseImageEnabled()
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

    public boolean isBpUseColorEnabled()
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

    public boolean isFirstEnvLoaded()
    {
        return firstEnvLoaded.get();
    }

    public BooleanProperty firstEnvLoadedProperty()
    {
        return firstEnvLoaded;
    }

    public void setFirstEnvLoaded(boolean firstEnvLoaded)
    {
        this.firstEnvLoaded.set(firstEnvLoaded);
    }
}
