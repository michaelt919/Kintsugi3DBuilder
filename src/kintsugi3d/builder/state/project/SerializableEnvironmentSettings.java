package kintsugi3d.builder.state.project;

import javafx.scene.paint.Color;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.function.Supplier;

public abstract class SerializableEnvironmentSettings implements DOMConvertable, EnvironmentSettings
{
    static <EnvironmentSettingType extends SerializableEnvironmentSettings> EnvironmentSettingType
    createNoEnvironment(Supplier<EnvironmentSettingType> environmentSettingConstructor)
    {
        EnvironmentSettingType noEnvironment = environmentSettingConstructor.get();
        noEnvironment.setName("No Environment");
        noEnvironment.setLocked(true);
        noEnvironment.setIsNoEnvironment(true);
        return noEnvironment;
    }

    private boolean isNoEnvironment;

    @Override
    public boolean isNoEnvironment()
    {
        return isNoEnvironment;
    }

    @Override
    public void setIsNoEnvironment(boolean isNoEnvironment)
    {
        this.isNoEnvironment = isNoEnvironment;
    }

    @Override
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("Environment");
        element.setAttribute("envUseImage", Boolean.toString(isEnvironmentImageEnabled()));
        element.setAttribute("envUseColor", Boolean.toString(isEnvironmentColorEnabled()));
        element.setAttribute("bpUseImage", Boolean.toString(isBackplateImageEnabled()));
        element.setAttribute("bpUseColor", Boolean.toString(isBackplateColorEnabled()));
        element.setAttribute("imagePathsRelative", Boolean.toString(areImagePathsRelative()));

        if (getEnvironmentImageFile() != null)
        {
            element.setAttribute("envImageFile", getEnvironmentImageFile().getPath());
        }

        if (getBackplateImageFile() != null)
        {
            element.setAttribute("bpImageFile", getBackplateImageFile().getPath());
        }

        element.setAttribute("backgroundIntensity", Double.toString(getBackgroundIntensity()));
        element.setAttribute("envColorIntensity", Double.toString(getEnvironmentColorIntensity()));
        element.setAttribute("envRotation", Double.toString(getEnvironmentRotation()));
        element.setAttribute("envFilteringBias", Double.toString(getEnvironmentFilteringBias()));

        element.setAttribute("envColor", getEnvironmentColor().toString());
        element.setAttribute("bpColor", getBackplateColor().toString());

        element.setAttribute("name", getName());
        element.setAttribute("locked", Boolean.toString(isLocked()));
        element.setAttribute("envLoaded", Boolean.toString(isEnvironmentLoaded()));
        element.setAttribute("bpLoaded", Boolean.toString(isBackplateLoaded()));

        element.setAttribute("gpEnabled", Boolean.toString(isGroundPlaneEnabled()));
        element.setAttribute("gpColor", getGroundPlaneCrolor().toString());
        element.setAttribute("gpHeight", Double.toString(getGroundPlaneHeight()));
        element.setAttribute("gpSize", Double.toString(getGroundPlaneSize()));
        return element;
    }

    public static <EnvironmentSettingType extends SerializableEnvironmentSettings>
    EnvironmentSettingType fromDOMElement(Element element, Supplier<EnvironmentSettingType> environmentSettingConstructor)
    {
        EnvironmentSettingType newEnvironment = environmentSettingConstructor.get();
        newEnvironment.setEnvironmentImageEnabled(Boolean.parseBoolean(element.getAttribute("envUseImage")));
        newEnvironment.setEnvironmentColorEnabled(Boolean.parseBoolean(element.getAttribute("envUseColor")));
        newEnvironment.setBackplateImageEnabled(Boolean.parseBoolean(element.getAttribute("bpUseImage")));
        newEnvironment.setBackplateColorEnabled(Boolean.parseBoolean(element.getAttribute("bpUseColor")));
        newEnvironment.setImagePathsRelative(Boolean.parseBoolean(element.getAttribute("imagePathsRelative")));
        newEnvironment.setEnvironmentImageFile(element.hasAttribute("envImageFile") ?
            new File(element.getAttribute("envImageFile")) : null);
        newEnvironment.setBackplateImageFile(element.hasAttribute("bpImageFile") ?
            new File(element.getAttribute("bpImageFile")) : null);
        newEnvironment.setBackgroundIntensity(Double.parseDouble(element.getAttribute("backgroundIntensity")));
        newEnvironment.setEnvironmentIntensity(Double.parseDouble(element.getAttribute("envColorIntensity")));
        newEnvironment.setEnvironmentRotation(Double.parseDouble(element.getAttribute("envRotation")));
        newEnvironment.setEnvironmentColor(Color.valueOf(element.getAttribute("envColor")));
        newEnvironment.setEnvironmentFilteringBias(element.hasAttribute("envFilteringBias") ?
            Double.parseDouble(element.getAttribute("envFilteringBias")) : 0);
        newEnvironment.setBackplateColor(Color.valueOf(element.getAttribute("bpColor")));
        newEnvironment.setName(element.getAttribute("name"));
        newEnvironment.setLocked(Boolean.parseBoolean(element.getAttribute("locked")));
        newEnvironment.setEnvironmentLoaded(Boolean.parseBoolean(element.getAttribute("envLoaded")));
        newEnvironment.setBackplateLoaded(Boolean.parseBoolean(element.getAttribute("bpLoaded")));
        newEnvironment.setGroundPlaneEnabled(element.hasAttribute("gpEnabled") && Boolean.parseBoolean(element.getAttribute("gpEnabled")));
        newEnvironment.setGroundPlaneColor(element.hasAttribute("gpColor") ?
            Color.valueOf(element.getAttribute("gpColor")) : Color.WHITE);
        newEnvironment.setGroundPlaneHeight(element.hasAttribute("gpHeight") ? Double.parseDouble(element.getAttribute("gpHeight")) : 0.0);
        newEnvironment.setGroundPlaneSize(element.hasAttribute("gpSize") ? Double.parseDouble(element.getAttribute("gpSize")) : 0.0);
        return newEnvironment;
    }

}
