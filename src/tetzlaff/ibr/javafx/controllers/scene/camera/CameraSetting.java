package tetzlaff.ibr.javafx.controllers.scene.camera;

import javafx.beans.property.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import tetzlaff.ibr.javafx.util.DOMConvertable;
import tetzlaff.ibr.javafx.util.StaticUtilities;

public class CameraSetting implements DOMConvertable
{
    private final DoubleProperty xCenter = new SimpleDoubleProperty();
    private final DoubleProperty yCenter = new SimpleDoubleProperty();
    private final DoubleProperty zCenter = new SimpleDoubleProperty();
    private final DoubleProperty azimuth = StaticUtilities.wrap(-180, 180, new SimpleDoubleProperty());
    private final DoubleProperty inclination = StaticUtilities.bound(-90, 90, new SimpleDoubleProperty());
    private final DoubleProperty log10distance = new SimpleDoubleProperty();
    private final DoubleProperty twist = StaticUtilities.wrap(-180.0, 180.0, new SimpleDoubleProperty());
    private final DoubleProperty fOV = new SimpleDoubleProperty(90.0);
    private final DoubleProperty focalLength = new SimpleDoubleProperty(18.0);
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
    public CameraSetting(Double xCenter, Double yCenter, Double zCenter, Double azimuth, Double inclination, Double log10distance, Double twist, Double fOV, Double focalLength, Boolean locked, Boolean orthographic, String name)
    {
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

    @Override
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("Camera");
        element.setAttribute("lookAtX", xCenter.getValue().toString());
        element.setAttribute("lookAtY", yCenter.getValue().toString());
        element.setAttribute("lookAtZ", zCenter.getValue().toString());
        element.setAttribute("azimuth", azimuth.getValue().toString());
        element.setAttribute("inclination", inclination.getValue().toString());
        element.setAttribute("log10Distance", log10distance.getValue().toString());
        element.setAttribute("twist", twist.getValue().toString());
        element.setAttribute("fov", fOV.getValue().toString());
        element.setAttribute("focalLength", focalLength.getValue().toString());
        element.setAttribute("locked", locked.getValue().toString());
        element.setAttribute("orthographic", orthographic.getValue().toString());
        element.setAttribute("name", name.getValue());
        return element;
    }

    public static CameraSetting fromDOMElement(Element element)
    {
        return new CameraSetting(
            Double.valueOf(element.getAttribute("lookAtX")),
            Double.valueOf(element.getAttribute("lookAtY")),
            Double.valueOf(element.getAttribute("lookAtZ")),
            Double.valueOf(element.getAttribute("azimuth")),
            Double.valueOf(element.getAttribute("inclination")),
            Double.valueOf(element.getAttribute("log10Distance")),
            Double.valueOf(element.getAttribute("twist")),
            Double.valueOf(element.getAttribute("fov")),
            Double.valueOf(element.getAttribute("focalLength")),
            Boolean.valueOf(element.getAttribute("locked")),
            Boolean.valueOf(element.getAttribute("orthographic")),
            element.getAttribute("name")
        );
    }

    @Override
    public String toString()
    {
        if (locked.getValue())
        {
            return "(L) " + name.getValue();
        }
        else
        {
            return name.getValue();
        }
    }

    public CameraSetting duplicate()
    {
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
            this.name.getValue() + " copy"
        );
    }

    public double getxCenter()
    {
        return xCenter.get();
    }

    public DoubleProperty xCenterProperty()
    {
        return xCenter;
    }

    public void setxCenter(double xCenter)
    {
        this.xCenter.set(xCenter);
    }

    public double getyCenter()
    {
        return yCenter.get();
    }

    public DoubleProperty yCenterProperty()
    {
        return yCenter;
    }

    public void setyCenter(double yCenter)
    {
        this.yCenter.set(yCenter);
    }

    public double getzCenter()
    {
        return zCenter.get();
    }

    public DoubleProperty zCenterProperty()
    {
        return zCenter;
    }

    public void setzCenter(double zCenter)
    {
        this.zCenter.set(zCenter);
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
        return log10distance.get();
    }

    public DoubleProperty log10distanceProperty()
    {
        return log10distance;
    }

    public void setLog10Distance(double log10distance)
    {
        this.log10distance.set(log10distance);
    }

    public double getTwist()
    {
        return twist.get();
    }

    public DoubleProperty twistProperty()
    {
        return twist;
    }

    public void setTwist(double twist)
    {
        this.twist.set(twist);
    }

    public double getFOV()
    {
        return fOV.get();
    }

    public DoubleProperty fovProperty()
    {
        return fOV;
    }

    public void setFOV(double fOV)
    {
        this.fOV.set(fOV);
    }

    public double getFocalLength()
    {
        return focalLength.get();
    }

    public DoubleProperty focalLengthProperty()
    {
        return focalLength;
    }

    public void setFocalLength(double focalLength)
    {
        this.focalLength.set(focalLength);
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

    public boolean isOrthographic()
    {
        return orthographic.get();
    }

    public BooleanProperty orthographicProperty()
    {
        return orthographic;
    }

    public void setOrthographic(boolean orthographic)
    {
        this.orthographic.set(orthographic);
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

    // V keep!
    public static void main(String... args)
    {
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

        for (String thing5 : things)
        {
            System.out.println("final DoubleProperty " + thing5 + " = new SimpleDoubleProperty();");
        }

        System.out.print("public CameraSetting(");
        for (String thing4 : things)
        {
            System.out.print("Double " + thing4 + ", ");
        }
        System.out.println(") {");

        for (String thing3 : things)
        {
            System.out.println("    this." + thing3 + ".setValue(" + thing3 + ");");
        }

        System.out.println("}");

        System.out.println("public Element toDOMElement() {");
        System.out.println("    return new Element(\"CameraSetting\")");
        for (String thing2 : things)
        {
            System.out.println("        .setAttribute(\"" + thing2 + "\", " + thing2 + ".getValue().toString())");
        }
        System.out.println("    ;\n}");

        System.out.println("public static CameraSetting fromDOMElement(Element element){\n");
        System.out.println("    return new CameraSetting(");
        for (String thing1 : things)
        {
            System.out.println("        Double.valueOf(element.getAttributeValue(\"" + thing1 + "\")),");
        }
        System.out.println("    );\n}");

        System.out.println("public CameraSetting duplicate(){");
        System.out.println("    return new CameraSetting(");
        for (String thing : things)
        {
            System.out.println("        this." + thing + ".getValue(),");
        }
        System.out.println("    );\n}");
    }
}
