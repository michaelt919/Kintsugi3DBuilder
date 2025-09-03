package kintsugi3d.builder.state.project;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.function.Supplier;

public abstract class SerializableObjectPoseSettings implements DOMConvertable, ObjectPoseSettings
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

    public static <ObjectPoseSettingType extends SerializableObjectPoseSettings>
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

}
