package kintsugi3d.builder.state.project;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.function.Supplier;

public abstract class SerializableLightGroupSettings<LightInstanceSettingType extends SerializableLightSettings> implements DOMConvertable, LightGroupSettings<LightInstanceSettingType>
{

    @Override
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("LightGroup");
        element.setAttribute("name", getName());
        element.setAttribute("locked", Boolean.toString(isLocked()));

        for (SerializableLightSettings lightInstance : getLightList())
        {
            element.appendChild(lightInstance.toDOMElement(document));
        }

        return element;
    }

    public static <LightGroupSettingType extends SerializableLightGroupSettings<LightInstanceSettingType>, LightInstanceSettingType extends SerializableLightSettings>
    LightGroupSettingType fromDOMElement(Element element, Supplier<LightGroupSettingType> lightGroupSettingConstructor)
    {
        LightGroupSettingType lightGroup = lightGroupSettingConstructor.get();
        lightGroup.setName(element.getAttribute("name"));
        lightGroup.setLocked(Boolean.parseBoolean(element.getAttribute("locked")));

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            Node child = children.item(i);
            if (child instanceof Element)
            {
                lightGroup.getLightList().add(SerializableLightSettings.fromDOMElement((Element)child, lightGroup::constructLightInstanceSetting));
            }
        }

        return lightGroup;
    }

}
