package tetzlaff.ibr.javafx.controllers.scene.environment_map;

import java.io.File;

import javafx.beans.property.*;
import javafx.scene.paint.Color;
import org.jdom2.Element;
import tetzlaff.ibr.javafx.util.StaticUtilities;
import tetzlaff.misc.XML_Writable;

public class EnvironmentSettings implements XML_Writable
{

    private final BooleanProperty envUseImage = new SimpleBooleanProperty();
    private final BooleanProperty envUseColor = new SimpleBooleanProperty();
    private final BooleanProperty bpUseImage = new SimpleBooleanProperty();
    private final BooleanProperty bpUseColor = new SimpleBooleanProperty();
    private final BooleanProperty imagePathsRelative = new SimpleBooleanProperty();

    private final Property<File> envImageFile = new SimpleObjectProperty<>();
    private final Property<File> bpImageFile = new SimpleObjectProperty<>();

    private final DoubleProperty envColorIntensity = StaticUtilities.bound(0, Double.MAX_VALUE, new SimpleDoubleProperty());
    private final DoubleProperty envRotation = StaticUtilities.wrap(-180, 180, new SimpleDoubleProperty());
    private final Property<Color> envColor = new SimpleObjectProperty<>();
    private final Property<Color> bpColor = new SimpleObjectProperty<>();
    private final StringProperty name = new SimpleStringProperty();
    private final BooleanProperty locked = new SimpleBooleanProperty();
    private final BooleanProperty firstEnvLoaded = new SimpleBooleanProperty();

    public EnvironmentSettings(Boolean envUseImage, Boolean envUseColor, Boolean bpUseImage, Boolean bpUseColor, Boolean imagePathsRelative, File envImageFile, File bpImageFile, Double envColorIntensity, Double envRotation, Color envColor, Color bpColor, String name, Boolean locked, Boolean firstEnvLoaded)
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
    public Element toJDOM2Element()
    {
        return new Element("EVSetting")
            .setAttribute("evUseImage", envUseImage.getValue().toString())
            .setAttribute("evUseColor", envUseColor.getValue().toString())
            .setAttribute("bpUseImage", bpUseImage.getValue().toString())
            .setAttribute("bpUseColor", bpUseColor.getValue().toString())
            .setAttribute("imagePathsRelative", imagePathsRelative.getValue().toString())
            .setAttribute("evImageFile", envImageFile.getValue().getPath())
            .setAttribute("bpImageFile", bpImageFile.getValue().getPath())

            .setAttribute("evColorIntensity", envColorIntensity.getValue().toString())
            .setAttribute("evRotation", envRotation.getValue().toString())
            .setAttribute("evColor", envColor.getValue().toString())
            .setAttribute("bpColor", bpColor.getValue().toString())
            .setAttribute("name", name.getValue())
            .setAttribute("locked", locked.getValue().toString())
            .setAttribute("firstEVLoaded", firstEnvLoaded.getValue().toString());
    }

    public static EnvironmentSettings fromJDOM2Element(Element element)
    {
        return new EnvironmentSettings(
            Boolean.valueOf(element.getAttributeValue("evUseImage")),
            Boolean.valueOf(element.getAttributeValue("evUseColor")),
            Boolean.valueOf(element.getAttributeValue("bpUseImage")),
            Boolean.valueOf(element.getAttributeValue("bpUseColor")),
            Boolean.valueOf(element.getAttributeValue("imagePathsRelative")),

            new File(element.getAttributeValue("evImageFile")),
            new File(element.getAttributeValue("bpImageFile")),

            Double.valueOf(element.getAttributeValue("evColorIntensity")),
            Double.valueOf(element.getAttributeValue("evRotation")),
            Color.valueOf(element.getAttributeValue("evColor")),
            Color.valueOf(element.getAttributeValue("bpColor")),
            String.valueOf(element.getAttributeValue("name")),
            Boolean.valueOf(element.getAttributeValue("locked")),
            Boolean.valueOf(element.getAttributeValue("firstEVLoaded"))
        );
    }

    @Override
    public String toString()
    {
        String out = name.getValue();
        if (locked.getValue())
        {
            out = "(L) " + out;
        }
        return out;
    }

    public EnvironmentSettings duplicate()
    {
        return new EnvironmentSettings(
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
            (name.getValue() + " copy"),
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

    public double getEnvColorIntensity()
    {
        return envColorIntensity.get();
    }

    public DoubleProperty envColorIntensityProperty()
    {
        return envColorIntensity;
    }

    public void setEnvColorIntensity(double envColorIntensity)
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
