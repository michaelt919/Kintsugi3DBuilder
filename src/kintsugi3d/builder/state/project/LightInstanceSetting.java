package kintsugi3d.builder.state.project;

import javafx.scene.paint.Color;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.function.Supplier;

public abstract class LightInstanceSetting implements DOMConvertable
{
    @Override
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("LightInstance");
        element.setAttribute("targetX", Double.toString(getTargetX()));
        element.setAttribute("targetY", Double.toString(getTargetY()));
        element.setAttribute("targetZ", Double.toString(getTargetZ()));
        element.setAttribute("azimuth", Double.toString(getAzimuth()));
        element.setAttribute("inclination", Double.toString(getInclination()));
        element.setAttribute("log10Distance", Double.toString(getLog10Distance()));
        element.setAttribute("intensity", Double.toString(getIntensity()));
        element.setAttribute("spotSize", Double.toString(getSpotSize()));
        element.setAttribute("spotTaper", Double.toString(getSpotTaper()));
        element.setAttribute("locked", Boolean.toString(isLocked()));
        element.setAttribute("name", getName());
        element.setAttribute("color", getColor().toString());
        return element;
    }

    public static <LightInstanceSettingType extends LightInstanceSetting>
    LightInstanceSettingType fromDOMElement(Element element, Supplier<LightInstanceSettingType> lightInstanceSettingConstructor)
    {
        LightInstanceSettingType setting = lightInstanceSettingConstructor.get();
        setting.setName(element.getAttribute("name"));
        setting.setTargetX(Double.parseDouble(element.getAttribute("targetX")));
        setting.setTargetY(Double.parseDouble(element.getAttribute("targetY")));
        setting.setTargetZ(Double.parseDouble(element.getAttribute("targetZ")));
        setting.setAzimuth(Double.parseDouble(element.getAttribute("azimuth")));
        setting.setInclination(Double.parseDouble(element.getAttribute("inclination")));
        setting.setLog10Distance(Double.parseDouble(element.getAttribute("log10Distance")));
        setting.setIntensity(Double.parseDouble(element.getAttribute("intensity")));

        if (element.hasAttribute("spotSize"))
        {
            setting.setSpotSize(Double.parseDouble(element.getAttribute("spotSize")));
        }

        if (element.hasAttribute("spotTaper"))
        {
            setting.setSpotTaper(Double.parseDouble(element.getAttribute("spotTaper")));
        }

        setting.setLocked(Boolean.parseBoolean(element.getAttribute("locked")));
        setting.setColor(Color.valueOf(element.getAttribute("color")));

        return setting;
    }

    public abstract boolean isGroupLocked();

    public abstract double getTargetX();

    public abstract double getTargetY();

    public abstract double getTargetZ();

    public abstract double getAzimuth();

    public abstract double getInclination();

    public abstract double getLog10Distance();

    public abstract double getIntensity();

    public abstract boolean isLocked();

    public abstract String getName();

    public abstract double getSpotSize();

    public abstract double getSpotTaper();

    public abstract Color getColor();

    public abstract void setTargetX(double value);

    public abstract void setTargetY(double value);

    public abstract void setTargetZ(double value);

    public abstract void setAzimuth(double value);

    public abstract void setInclination(double value);

    public abstract void setLog10Distance(double value);

    public abstract void setIntensity(double value);

    public abstract void setLocked(boolean value);

    public abstract void setName(String value);

    public abstract void setSpotSize(double value);

    public abstract void setSpotTaper(double value);

    public abstract void setColor(Color value);
}
