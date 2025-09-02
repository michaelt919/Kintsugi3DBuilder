package kintsugi3d.builder.state;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.function.Supplier;

public abstract class ObjectPoseSetting implements DOMConvertable
{
    @Override
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("Camera");
        element.setAttribute("centerX", Double.toString(getCenterX()));
        element.setAttribute("centerY", Double.toString(getCenterY()));
        element.setAttribute("centerZ", Double.toString(getCenterZ()));
        element.setAttribute("rotateY", Double.toString(getRotateY()));
        element.setAttribute("rotateX", Double.toString(getRotateX()));
        element.setAttribute("rotateZ", Double.toString(getRotateZ()));
        element.setAttribute("locked", Boolean.toString(isLocked()));
        element.setAttribute("scale", Double.toString(getScale()));
        element.setAttribute("name", getName());
        return element;
    }

    public static <ObjectPoseSettingType extends ObjectPoseSetting>
    ObjectPoseSettingType fromDOMElement(Element element, Supplier<ObjectPoseSettingType> objectPoseSettingConstructor)
    {
        ObjectPoseSettingType setting = objectPoseSettingConstructor.get();
        setting.setName(element.getAttribute("name"));
        setting.setCenterX(Double.parseDouble(element.getAttribute("centerX")));
        setting.setCenterY(Double.parseDouble(element.getAttribute("centerY")));
        setting.setCenterZ(Double.parseDouble(element.getAttribute("centerZ")));
        setting.setRotateY(Double.parseDouble(element.getAttribute("rotateY")));
        setting.setRotateX(Double.parseDouble(element.getAttribute("rotateX")));
        setting.setRotateZ(Double.parseDouble(element.getAttribute("rotateZ")));
        setting.setLocked(Boolean.parseBoolean(element.getAttribute("locked")));

        setting.setScale(element.hasAttribute("scale")
            ? Double.parseDouble(element.getAttribute("scale"))
            : 1.0);

        return setting;
    }

    public abstract double getCenterX();

    public abstract void setCenterX(double centerX);

    public abstract double getCenterY();

    public abstract void setCenterY(double centerY);

    public abstract double getCenterZ();

    public abstract void setCenterZ(double centerZ);

    public abstract double getRotateY();

    public abstract void setRotateY(double rotateY);

    public abstract double getRotateX();

    public abstract void setRotateX(double rotateX);

    public abstract double getRotateZ();

    public abstract void setRotateZ(double rotateZ);

    public abstract boolean isLocked();

    public abstract void setLocked(boolean locked);

    public abstract double getScale();

    public abstract void setScale(double scale);

    public abstract String getName();

    public abstract void setName(String name);
}
