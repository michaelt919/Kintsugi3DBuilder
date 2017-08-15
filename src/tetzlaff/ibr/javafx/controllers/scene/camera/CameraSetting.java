package tetzlaff.ibr.javafx.controllers.scene.camera;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jdom2.Element;

import tetzlaff.ibr.javafx.util.StaticUtilities;
import tetzlaff.misc.XML_Writable;

public class CameraSetting implements XML_Writable{
    private final DoubleProperty xCenter = new SimpleDoubleProperty();
    private final DoubleProperty yCenter = new SimpleDoubleProperty();
    private final DoubleProperty zCenter = new SimpleDoubleProperty();
    private final DoubleProperty azimuth = StaticUtilities.wrap(-180, 180, new SimpleDoubleProperty());
    private final DoubleProperty inclination = StaticUtilities.bound(-90,90, new SimpleDoubleProperty());
    private final DoubleProperty log10distance = new SimpleDoubleProperty();
    private final DoubleProperty twist = StaticUtilities.wrap(-180.0, 180.0, new SimpleDoubleProperty());
    private final DoubleProperty fOV = new SimpleDoubleProperty();
    private final DoubleProperty focalLength = new SimpleDoubleProperty();
    private final BooleanProperty locked = new SimpleBooleanProperty();
    private final BooleanProperty orthographic = new SimpleBooleanProperty();
    private final StringProperty name = new SimpleStringProperty();






    /*
    xCenter
    yCenter
    zCenter
    azimuth
    inclination
    log10distance
    twist
    fOV
    focalLength
    locked
    orthographic
    name
     */
    public CameraSetting(Double xCenter, Double yCenter, Double zCenter, Double azimuth, Double inclination, Double log10distance, Double twist, Double fOV, Double focalLength, Boolean locked, Boolean orthographic, String name) {
        this.xCenter.setValue(xCenter);
        this.yCenter.setValue(yCenter);
        this.zCenter.setValue(zCenter);
        this.azimuth.setValue(azimuth);
        this.inclination.setValue(inclination);
        this.log10distance.setValue(log10distance);
        this.twist.setValue(twist);
        this.fOV.setValue(fOV);
        this.focalLength.setValue(focalLength);
        this.locked.setValue(locked);
        this.orthographic.setValue(orthographic);
        this.name.setValue(name);
    }

    public Element toJDOM2Element() {
        return new Element("CameraSetting")
                .setAttribute("xCenter", xCenter.getValue().toString())
                .setAttribute("yCenter", yCenter.getValue().toString())
                .setAttribute("zCenter", zCenter.getValue().toString())
                .setAttribute("azimuth", azimuth.getValue().toString())
                .setAttribute("inclination", inclination.getValue().toString())
                .setAttribute("log10distance", log10distance.getValue().toString())
                .setAttribute("twist", twist.getValue().toString())
                .setAttribute("fOV", fOV.getValue().toString())
                .setAttribute("focalLength", focalLength.getValue().toString())
                .setAttribute("locked", locked.getValue().toString())
                .setAttribute("orthographic", orthographic.getValue().toString())
                .setAttribute("name", name.getValue())
                ;
    }

    public static CameraSetting fromJDOM2Element(Element element) {
        return new CameraSetting(
                Double.valueOf(element.getAttributeValue("xCenter")),
                Double.valueOf(element.getAttributeValue("yCenter")),
                Double.valueOf(element.getAttributeValue("zCenter")),
                Double.valueOf(element.getAttributeValue("azimuth")),
                Double.valueOf(element.getAttributeValue("inclination")),
                Double.valueOf(element.getAttributeValue("log10distance")),
                Double.valueOf(element.getAttributeValue("twist")),
                Double.valueOf(element.getAttributeValue("fOV")),
                Double.valueOf(element.getAttributeValue("focalLength")),
                Boolean.valueOf(element.getAttributeValue("locked")),
                Boolean.valueOf(element.getAttributeValue("orthographic")),
                element.getAttributeValue("name")
        );
    }

    @Override
    public String toString() {
        String out = name.getValue();
        if(locked.getValue()){
            out = "(L) " + out;
        }
        return out;
    }


    public CameraSetting duplicate(){
        return new CameraSetting(
                this.xCenter.getValue(),
                this.yCenter.getValue(),
                this.zCenter.getValue(),
                this.azimuth.getValue(),
                this.inclination.getValue(),
                this.log10distance.getValue(),
                this.twist.getValue(),
                this.fOV.getValue(),
                this.focalLength.getValue(),
                this.locked.getValue(),
                this.orthographic.getValue(),
                (this.name.getValue() + " copy")
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

    public double getLog10distance() {
        return log10distance.get();
    }

    public DoubleProperty log10distanceProperty() {
        return log10distance;
    }

    public void setLog10distance(double log10distance) {
        this.log10distance.set(log10distance);
    }

    public double getTwist() {
        return twist.get();
    }

    public DoubleProperty twistProperty() {
        return twist;
    }

    public void setTwist(double twist) {
        this.twist.set(twist);
    }

    public double getfOV() {
        return fOV.get();
    }

    public DoubleProperty fOVProperty() {
        return fOV;
    }

    public void setfOV(double fOV) {
        this.fOV.set(fOV);
    }

    public double getFocalLength() {
        return focalLength.get();
    }

    public DoubleProperty focalLengthProperty() {
        return focalLength;
    }

    public void setFocalLength(double focalLength) {
        this.focalLength.set(focalLength);
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

    public boolean isOrthographic() {
        return orthographic.get();
    }

    public BooleanProperty orthographicProperty() {
        return orthographic;
    }

    public void setOrthographic(boolean orthographic) {
        this.orthographic.set(orthographic);
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

    // V keep!
    public static void main(String[] args) {
        String[] things = {
                "xCenter",
                "yCenter",
                "zCenter",
                "azimuth",
                "inclination",
                "log10distance",
                "twist",
                "fOV",
                "focalLength"
        };

        for (int i = 0; i < things.length; i++) {
            System.out.println("final DoubleProperty " + things[i] + " = new SimpleDoubleProperty();");
        }

        System.out.print("public CameraSetting(");
        for (int i = 0; i < things.length; i++) {
            System.out.print("Double " + things[i] + ", ");
        }
        System.out.println(") {");

        for (int i = 0; i < things.length; i++) {
            System.out.println("    this." + things[i] + ".setValue(" + things[i] + ");");
        }

        System.out.println("}");

        System.out.println("public Element toJDOM2Element() {");
        System.out.println("    return new Element(\"CameraSetting\")");
        for (int i = 0; i < things.length; i++) {
            System.out.println("        .setAttribute(\"" + things[i] + "\", " + things[i] + ".getValue().toString())");
        }
        System.out.println("    ;\n}");


        System.out.println("public static CameraSetting fromJDOM2Element(Element element){\n");
        System.out.println("    return new CameraSetting(");
        for (int i = 0; i < things.length; i++) {
            System.out.println("        Double.valueOf(element.getAttributeValue(\"" + things[i] + "\")),");
        }
        System.out.println("    );\n}");

        System.out.println("public CameraSetting duplicate(){");
        System.out.println("    return new CameraSetting(");
        for (int i = 0; i < things.length; i++) {
            System.out.println("        this." + things[i] + ".getValue(),");
        }
        System.out.println("    );\n}");


    }
}
