package tetzlaff.ibr.gui2.controllers.scene.lights;//Created by alexk on 7/16/2017.

import javafx.beans.property.*;
import javafx.fxml.Initializable;
import org.jdom2.Element;
import tetzlaff.misc.XML_Writable;

import java.net.URL;
import java.util.ResourceBundle;

public class LightSetting implements XML_Writable{

    private final DoubleProperty xCenter = new SimpleDoubleProperty();
    private final DoubleProperty yCenter = new SimpleDoubleProperty();
    private final DoubleProperty zCenter = new SimpleDoubleProperty();
    private final DoubleProperty azimuth = new SimpleDoubleProperty();
    private final DoubleProperty inclination = new SimpleDoubleProperty();
    private final DoubleProperty distance = new SimpleDoubleProperty();
    private final DoubleProperty intensity = new SimpleDoubleProperty();
    private final BooleanProperty locked = new SimpleBooleanProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final Property<LightType> lightType = new SimpleObjectProperty<>();

    private final BooleanProperty groupLocked;

    public boolean getGroupLocked() {
        return groupLocked.get();
    }

    public LightSetting(
            Double xCenter,
            Double yCenter,
            Double zCenter,
            Double azimuth,
            Double inclination,
            Double distance,
            Double intensity,
            Boolean locked,
            String name,
            LightType lightType,
            BooleanProperty groopLockedProperty
    ) {
        this.xCenter.setValue(xCenter);
        this.yCenter.setValue(yCenter);
        this.zCenter.setValue(zCenter);
        this.azimuth.setValue(azimuth);
        this.inclination.setValue(inclination);
        this.distance.setValue(distance);
        this.intensity.setValue(intensity);
        this.locked.setValue(locked);
        this.name.setValue("X");
        this.lightType.setValue(lightType);
        this.groupLocked = groopLockedProperty;
    }

    public LightSetting duplicate() {
        return new LightSetting(
                xCenter.getValue(),
                yCenter.getValue(),
                zCenter.getValue(),
                azimuth.getValue(),
                inclination.getValue(),
                distance.getValue(),
                intensity.getValue(),
                locked.getValue(),
                (name.getValue() + " copy"),
                lightType.getValue(),
                this.groupLocked
        );
    }

    @Override
    public String toString() {
        return name.getValue();
    }

    @Override
    public Element toJDOM2Element() {
        return new Element("LightSetting")
                .setAttribute("xCenter", xCenter.getValue().toString())
                .setAttribute("yCenter", yCenter.getValue().toString())
                .setAttribute("zCenter", zCenter.getValue().toString())
                .setAttribute("azimuth", azimuth.getValue().toString())
                .setAttribute("inclination", inclination.getValue().toString())
                .setAttribute("distance", distance.getValue().toString())
                .setAttribute("intensity", intensity.getValue().toString())
                .setAttribute("locked", locked.getValue().toString())
                .setAttribute("name", name.getValue())
                .setAttribute("lightType", lightType.getValue().toString())
                ;
    }

    public static LightSetting fromJDOM2Element(Element e, BooleanProperty groupLockedProperty) {
        return new LightSetting(
                Double.valueOf(e.getAttributeValue("xCenter")),
                Double.valueOf(e.getAttributeValue("yCenter")),
                Double.valueOf(e.getAttributeValue("zCenter")),
                Double.valueOf(e.getAttributeValue("azimuth")),
                Double.valueOf(e.getAttributeValue("inclination")),
                Double.valueOf(e.getAttributeValue("distance")),
                Double.valueOf(e.getAttributeValue("intensity")),
                Boolean.valueOf(e.getAttributeValue("locked")),
                e.getAttributeValue("name"),
                LightType.valueOf(e.getAttributeValue("lightType")),
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

    public double getDistance() {
        return distance.get();
    }

    public DoubleProperty distanceProperty() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance.set(distance);
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
}
