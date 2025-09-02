package kintsugi3d.builder.state.project;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.function.Supplier;

public abstract class CameraSetting implements DOMConvertable
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

    public static <CameraSettingType extends CameraSetting> CameraSettingType
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

    public abstract double getXCenter();

    public abstract void setXCenter(double xCenter);

    public abstract double getYCenter();

    public abstract void setYCenter(double yCenter);

    public abstract double getZCenter();

    public abstract void setZCenter(double zCenter);

    public abstract double getAzimuth();

    public abstract void setAzimuth(double azimuth);

    public abstract double getInclination();

    public abstract void setInclination(double inclination);

    public abstract double getLog10Distance();

    public abstract void setLog10Distance(double log10distance);

    public abstract double getTwist();

    public abstract void setTwist(double twist);

    public abstract double getFOV();

    public abstract void setFOV(double fOV);

    public abstract double getFocalLength();

    public abstract void setFocalLength(double focalLength);

    public abstract boolean isLocked();

    public abstract void setLocked(boolean locked);

    public abstract boolean isOrthographic();

    public abstract void setOrthographic(boolean orthographic);

    public abstract String getName();

    public abstract void setName(String name);
}
