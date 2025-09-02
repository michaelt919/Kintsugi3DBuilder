package kintsugi3d.builder.state;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.function.Supplier;

public abstract class LightGroupSetting<LightInstanceSettingType extends LightInstanceSetting> implements DOMConvertable
{
    public abstract void addLight(int index, double targetX, double targetY, double targetZ);

    public abstract void removeLight(int index);

    @Override
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("LightGroup");
        element.setAttribute("name", getName());
        element.setAttribute("locked", Boolean.toString(isLocked()));

        for (LightInstanceSetting lightInstance : getLightList())
        {
            element.appendChild(lightInstance.toDOMElement(document));
        }

        return element;
    }

    public static <LightGroupSettingType extends LightGroupSetting<LightInstanceSettingType>, LightInstanceSettingType extends LightInstanceSetting>
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
                lightGroup.getLightList().add(LightInstanceSetting.fromDOMElement((Element)child, lightGroup::constructLightInstanceSetting));
            }
        }

        return lightGroup;
    }

    protected abstract LightInstanceSettingType constructLightInstanceSetting();

    public abstract int getLightCount();

    public abstract List<LightInstanceSettingType> getLightList();

    public abstract boolean isLocked();

    public abstract void setLocked(boolean locked);

    public abstract String getName();

    public abstract void setName(String name);
}
