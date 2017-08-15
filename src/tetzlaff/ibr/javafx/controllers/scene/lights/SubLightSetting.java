package tetzlaff.ibr.javafx.controllers.scene.lights;//Created by alexk on 7/16/2017.

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

import org.jdom2.Element;

import tetzlaff.ibr.javafx.util.StaticUtilities;
import tetzlaff.misc.XML_Writable;

public class SubLightSetting implements XML_Writable{

    private final DoubleProperty xCenter = new SimpleDoubleProperty();
    private final DoubleProperty yCenter = new SimpleDoubleProperty();
    private final DoubleProperty zCenter = new SimpleDoubleProperty();
    private final DoubleProperty azimuth = StaticUtilities.wrap(-180, 180, new SimpleDoubleProperty());
    private final DoubleProperty inclination = StaticUtilities.bound(-90, 90, new SimpleDoubleProperty());
    private final DoubleProperty log10distance = new SimpleDoubleProperty();
    private final DoubleProperty intensity = StaticUtilities.bound(0, Double.MAX_VALUE, new SimpleDoubleProperty());
    private final BooleanProperty locked = new SimpleBooleanProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final Property<LightType> lightType = new SimpleObjectProperty<>();
    private final Property<Color> color = new SimpleObjectProperty<>();

    private final BooleanProperty groupLocked;

    public boolean getGroupLocked() {
        return groupLocked.get();
    }

    public SubLightSetting(
            Double xCenter,
            Double yCenter,
            Double zCenter,
            Double azimuth,
            Double inclination,
            Double log10distance,
            Double intensity,
            Boolean locked,
            String name,
            LightType lightType,
            Color color,
            BooleanProperty groupLockedProperty
    ) {
        this.xCenter.setValue(xCenter);
        this.yCenter.setValue(yCenter);
        this.zCenter.setValue(zCenter);
        this.azimuth.setValue(azimuth);
        this.inclination.setValue(inclination);
        this.log10distance.setValue(log10distance);
        this.intensity.setValue(intensity);
        this.locked.setValue(locked);
        this.name.setValue(name);
        this.lightType.setValue(lightType);
        this.color.setValue(color);
        this.groupLocked = groupLockedProperty;
    }

    public SubLightSetting duplicate() {
        return new SubLightSetting(
                xCenter.getValue(),
                yCenter.getValue(),
                zCenter.getValue(),
                azimuth.getValue(),
                inclination.getValue(),
                log10distance.getValue(),
                intensity.getValue(),
                locked.getValue(),
                name.getValue(),
                lightType.getValue(),
                color.getValue(),
                this.groupLocked
        );
    }

    @Override
    public String toString() {
        return name.getValue();
    }

    @Override
    public Element toJDOM2Element() {
        return new Element("SubLightSetting")
                .setAttribute("xCenter", xCenter.getValue().toString())
                .setAttribute("yCenter", yCenter.getValue().toString())
                .setAttribute("zCenter", zCenter.getValue().toString())
                .setAttribute("azimuth", azimuth.getValue().toString())
                .setAttribute("inclination", inclination.getValue().toString())
                .setAttribute("log10distance", log10distance.getValue().toString())
                .setAttribute("intensity", intensity.getValue().toString())
                .setAttribute("locked", locked.getValue().toString())
                .setAttribute("name", name.getValue())
                .setAttribute("lightType", lightType.getValue().toString())
                .setAttribute("color", color.getValue().toString())
                ;
    }

    public static SubLightSetting fromJDOM2Element(Element e, BooleanProperty groupLockedProperty) {
        return new SubLightSetting(
                Double.valueOf(e.getAttributeValue("xCenter")),
                Double.valueOf(e.getAttributeValue("yCenter")),
                Double.valueOf(e.getAttributeValue("zCenter")),
                Double.valueOf(e.getAttributeValue("azimuth")),
                Double.valueOf(e.getAttributeValue("inclination")),
                Double.valueOf(e.getAttributeValue("log10distance")),
                Double.valueOf(e.getAttributeValue("intensity")),
                Boolean.valueOf(e.getAttributeValue("locked")),
                e.getAttributeValue("name"),
                LightType.valueOf(e.getAttributeValue("lightType")),
                Color.valueOf(e.getAttributeValue("color")),
                groupLockedProperty
        );
    }

    public double getxCenter() {
        return xCenter.get();
    }

    public DoubleProperty xCenterProperty() {
        return xCenter;
    }

    public void setxCenter(double xCenter) {
        this.xCenter.set(xCenter);
    }

    public double getyCenter() {
        return yCenter.get();
    }

    public DoubleProperty yCenterProperty() {
        return yCenter;
    }

    public void setyCenter(double yCenter) {
        this.yCenter.set(yCenter);
    }

    public double getzCenter() {
        return zCenter.get();
    }

    public DoubleProperty zCenterProperty() {
        return zCenter;
    }

    public void setzCenter(double zCenter) {
        this.zCenter.set(zCenter);
    }

    public double getAzimuth() {
        return azimuth.get();
    }

    public DoubleProperty azimuthProperty() {
        return azimuth;
    }

    public void setAzimuth(double azimuth) {
        this.azimuth.set(azimuth);
    }

    public double getInclination() {
        return inclination.get();
    }

    public DoubleProperty inclinationProperty() {
        return inclination;
    }

    public void setInclination(double inclination) {
        this.inclination.set(inclination);
    }

    public double getLog10Distance() {
        return log10distance.get();
    }

    public DoubleProperty log10distanceProperty() {
        return log10distance;
    }

    public void setLog10distance(double log10distance) {
        this.log10distance.set(log10distance);
    }

    public double getIntensity() {
        return intensity.get();
    }

    public DoubleProperty intensityProperty() {
        return intensity;
    }

    public void setIntensity(double intensity) {
        this.intensity.set(intensity);
    }

    public boolean isLocked() {
        return locked.get();
    }

    public BooleanProperty lockedProperty() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked.set(locked);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public LightType getLightType() {
        return lightType.getValue();
    }

    public Property<LightType> lightTypeProperty() {
        return lightType;
    }

    public void setLightType(LightType lightType) {
        this.lightType.setValue(lightType);
    }

    public Color getColor() {
        return color.getValue();
    }

    public Property<Color> colorProperty() {
        return color;
    }

    public void setColor(Color color) {
        this.color.setValue(color);
    }
}
