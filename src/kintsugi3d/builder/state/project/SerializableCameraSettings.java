package kintsugi3d.builder.state.project;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.function.Supplier;

public abstract class SerializableCameraSettings implements DOMConvertable, CameraSettings
{
    @Override
    public Element toDOMElement(Document document)
    {
        Element element = document.createElement("Camera");
        element.setAttribute("lookAtX", Double.toString(getXCenter()));
        element.setAttribute("lookAtY", Double.toString(getYCenter()));
        element.setAttribute("lookAtZ", Double.toString(getZCenter()));
        element.setAttribute("azimuth", Double.toString(getAzimuth()));
        element.setAttribute("inclination", Double.toString(getInclination()));
        element.setAttribute("log10Distance", Double.toString(getLog10Distance()));
        element.setAttribute("twist", Double.toString(getTwist()));
        element.setAttribute("fov", Double.toString(getFOV()));
        element.setAttribute("locked", Boolean.toString(isLocked()));
        element.setAttribute("orthographic", Boolean.toString(isOrthographic()));
        element.setAttribute("name", getName());
        return element;
    }

    public static <CameraSettingType extends SerializableCameraSettings> CameraSettingType
    fromDOMElement(Element element, Supplier<CameraSettingType> cameraSettingConstructor)
    {
        CameraSettingType newCamera = cameraSettingConstructor.get();
        newCamera.setXCenter(Double.parseDouble(element.getAttribute("lookAtX")));
        newCamera.setYCenter(Double.parseDouble(element.getAttribute("lookAtY")));
        newCamera.setZCenter(Double.parseDouble(element.getAttribute("lookAtZ")));
        newCamera.setAzimuth(Double.parseDouble(element.getAttribute("azimuth")));
        newCamera.setInclination(Double.parseDouble(element.getAttribute("inclination")));
        newCamera.setLog10Distance(Double.parseDouble(element.getAttribute("log10Distance")));
        newCamera.setTwist(Double.parseDouble(element.getAttribute("twist")));
        newCamera.setFOV(Double.parseDouble(element.getAttribute("fov")));
        newCamera.setLocked(Boolean.parseBoolean(element.getAttribute("locked")));
        newCamera.setOrthographic(Boolean.parseBoolean(element.getAttribute("orthographic")));
        newCamera.setName(element.getAttribute("name"));
        return newCamera;
    }

}
