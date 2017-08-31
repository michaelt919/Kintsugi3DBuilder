package tetzlaff.ibr.javafx.controllers.scene.lights;//Created by alexk on 7/16/2017.

import javafx.beans.property.*;
import javafx.scene.paint.Color;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import tetzlaff.ibr.javafx.util.DOMConvertable;
import tetzlaff.ibr.javafx.util.StaticUtilities;

public class LightInstanceSetting implements DOMConvertable
{
    private final DoubleProperty targetX = new SimpleDoubleProperty();
    private final DoubleProperty targetY = new SimpleDoubleProperty();
    private final DoubleProperty targetZ = new SimpleDoubleProperty();
    private final DoubleProperty azimuth = StaticUtilities.wrap(-180, 180, new SimpleDoubleProperty());
    private final DoubleProperty inclination = StaticUtilities.bound(-90, 90, new SimpleDoubleProperty());
    private final DoubleProperty log10Distance = new SimpleDoubleProperty();
    private final DoubleProperty intensity = StaticUtilities.bound(0, Double.MAX_VALUE, new SimpleDoubleProperty());
    private final BooleanProperty locked = new SimpleBooleanProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final Property<LightType> lightType = new SimpleObjectProperty<>();
    private final Property<Color> color = new SimpleObjectProperty<>();

    private final BooleanProperty groupLocked;

    public boolean isGroupLocked()
    {
        return groupLocked.get();
    }

    public LightInstanceSetting(
        Double targetX,
        Double targetY,
        Double targetZ,
        Double azimuth,
        Double inclination,
        Double log10Distance,
        Double intensity,
        Boolean locked,
        String name,
        LightType lightType,
        Color color,
        BooleanProperty groupLockedProperty
    )
    {
        this.targetX.setValue(targetX);
        this.targetY.setValue(targetY);
        this.targetZ.setValue(targetZ);
        this.azimuth.setValue(azimuth);
        this.inclination.setValue(inclination);
        this.log10Distance.setValue(log10Distance);
        this.intensity.setValue(intensity);
        this.locked.setValue(locked);
        this.name.setValue(name);
        this.lightType.setValue(lightType);
        this.color.setValue(color);
        this.groupLocked = groupLockedProperty;
    }

    public LightInstanceSetting duplicate()
    {
        return new LightInstanceSetting(
            targetX.getValue(),
            targetY.getValue(),
            targetZ.getValue(),
            azimuth.getValue(),
            inclination.getValue(),
            log10Distance.getValue(),
            intensity.getValue(),
            locked.getValue(),
            name.getValue(),
            lightType.getValue(),
            color.getValue(),
            this.groupLocked
        );
    }

    @Override
    public String toString()
    {
        return name.getValue();
    }

    @Override
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("LightInstance");
        element.setAttribute("targetX", targetX.getValue().toString());
        element.setAttribute("targetY", targetY.getValue().toString());
        element.setAttribute("targetZ", targetZ.getValue().toString());
        element.setAttribute("azimuth", azimuth.getValue().toString());
        element.setAttribute("inclination", inclination.getValue().toString());
        element.setAttribute("log10Distance", log10Distance.getValue().toString());
        element.setAttribute("intensity", intensity.getValue().toString());
        element.setAttribute("locked", locked.getValue().toString());
        element.setAttribute("name", name.getValue());
        element.setAttribute("lightType", lightType.getValue().toString());
        element.setAttribute("color", color.getValue().toString());
        return element;
    }

    public static LightInstanceSetting fromDOMElement(Element element, BooleanProperty groupLockedProperty)
    {
        return new LightInstanceSetting(
            Double.valueOf(element.getAttribute("targetX")),
            Double.valueOf(element.getAttribute("targetY")),
            Double.valueOf(element.getAttribute("targetZ")),
            Double.valueOf(element.getAttribute("azimuth")),
            Double.valueOf(element.getAttribute("inclination")),
            Double.valueOf(element.getAttribute("log10Distance")),
            Double.valueOf(element.getAttribute("intensity")),
            Boolean.valueOf(element.getAttribute("locked")),
            element.getAttribute("name"),
            LightType.valueOf(element.getAttribute("lightType")),
            Color.valueOf(element.getAttribute("color")),
            groupLockedProperty
        );
    }

    public double getTargetX()
    {
        return targetX.get();
    }

    public DoubleProperty targetXProperty()
    {
        return targetX;
    }

    public void setTargetX(double targetX)
    {
        this.targetX.set(targetX);
    }

    public double getTargetY()
    {
        return targetY.get();
    }

    public DoubleProperty targetYProperty()
    {
        return targetY;
    }

    public void setTargetY(double targetY)
    {
        this.targetY.set(targetY);
    }

    public double getTargetZ()
    {
        return targetZ.get();
    }

    public DoubleProperty targetZProperty()
    {
        return targetZ;
    }

    public void setTargetZ(double targetZ)
    {
        this.targetZ.set(targetZ);
    }

    public double getAzimuth()
    {
        return azimuth.get();
    }

    public DoubleProperty azimuthProperty()
    {
        return azimuth;
    }

    public void setAzimuth(double azimuth)
    {
        this.azimuth.set(azimuth);
    }

    public double getInclination()
    {
        return inclination.get();
    }

    public DoubleProperty inclinationProperty()
    {
        return inclination;
    }

    public void setInclination(double inclination)
    {
        this.inclination.set(inclination);
    }

    public double getLog10Distance()
    {
        return log10Distance.get();
    }

    public DoubleProperty log10DistanceProperty()
    {
        return log10Distance;
    }

    public void setLog10Distance(double log10Distance)
    {
        this.log10Distance.set(log10Distance);
    }

    public double getIntensity()
    {
        return intensity.get();
    }

    public DoubleProperty intensityProperty()
    {
        return intensity;
    }

    public void setIntensity(double intensity)
    {
        this.intensity.set(intensity);
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

    public LightType getLightType()
    {
        return lightType.getValue();
    }

    public Property<LightType> lightTypeProperty()
    {
        return lightType;
    }

    public void setLightType(LightType lightType)
    {
        this.lightType.setValue(lightType);
    }

    public Color getColor()
    {
        return color.getValue();
    }

    public Property<Color> colorProperty()
    {
        return color;
    }

    public void setColor(Color color)
    {
        this.color.setValue(color);
    }
}
