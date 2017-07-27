package tetzlaff.ibr.rendering2;//Created by alexk on 7/21/2017.

import javafx.beans.property.ReadOnlyObjectProperty;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.gui2.controllers.scene.camera.CameraSetting;
import tetzlaff.ibr.gui2.other.OrbitPolarConverter;
import tetzlaff.mvc.models.ControllableCameraModel;

import com.sun.istack.internal.NotNull;
import tetzlaff.util.Math2;

public class CameraModel3 implements ControllableCameraModel {

    private ReadOnlyObjectProperty<CameraSetting> selected;
    private final CameraSetting backup = new CameraSetting(
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            1.0,
            0.0,
            1.0,
            1.0,
            true,
            false,
            "backup"
    );



    public void setSelectedCameraSettingProperty(ReadOnlyObjectProperty<CameraSetting> selectedCameraSettingProperty){
        this.selected = selectedCameraSettingProperty;
    }

    @NotNull
    private CameraSetting cam(){
        if(selected == null || selected.getValue() == null)return backup;
        else return selected.getValue();
    }


    private Matrix4 orbitCash;
    private boolean fromRender = false;
    @Override
    public Matrix4 getLookMatrix() {
        return Matrix4.lookAt(
                new Vector3(0,0,getDistance()),
                Vector3.ZERO,
                new Vector3(0,1,0)
        ).times(getOrbit().times(
                Matrix4.translate(getCenter())
        ));
    }

    @Override
    public Matrix4 getOrbit() {
        if(fromRender){
            fromRender = false;
            return orbitCash;
    }
        Vector3 poler = new Vector3((float) cam().getAzimuth(), (float) cam().getInclination(), (float) cam().getTwist());
        return OrbitPolarConverter.self.convertLeft(poler);
    }

    @Override
    public void setOrbit(Matrix4 orbit) {
        Vector3 poler = OrbitPolarConverter.self.convertRight(orbit);
        cam().setAzimuth(poler.x);
        cam().setInclination(poler.y);
        cam().setTwist(poler.z);
        orbitCash = orbit;
        fromRender = true;
    }

    @Override
    public Float getLog10distance() {
        return (float) cam().getLog10distance();
    }

    @Override
    public void setLog10distance(Float log10distance) {
        cam().setLog10distance(log10distance);
    }

    @Override
    public Float getDistance() {
        return (float) Math2.pow10(cam().getLog10distance());
    }

    @Override
    public void setDistance(Float distance) {
        cam().setLog10distance(Math.log10(distance));
    }

    @Override
    public Vector3 getCenter() {
        return new Vector3((float) cam().getxCenter(),
                (float) cam().getyCenter(),
                (float) cam().getzCenter());
    }

    @Override
    public void setCenter(Vector3 center) {
        cam().setxCenter(center.x);
        cam().setyCenter(center.y);
        cam().setzCenter(center.z);
    }

    @Override
    public Double getTwist() {
        return cam().getTwist();
    }

    @Override
    public void setTwist(Double twist) {
        cam().setTwist(twist);
    }

    @Override
    public Double getAzimuth() {
        return cam().getAzimuth();
    }

    @Override
    public void setAzimuth(Double azimuth) {
        cam().setAzimuth(azimuth);
    }

    @Override
    public Double getInclination() {
        return cam().getInclination();
    }

    @Override
    public void setInclination(Double inclination) {
        cam().setInclination(inclination);
    }

    /**
     * this method is intended to return whether or not the selected camera is locked.
     * It is called by the render side of the program, and when it returns true
     * the camera should not be able to be changed using the tools in the render window.
     *
     * @return true for locked
     */
    @Override
    public boolean getLocked() {
        return cam().isLocked();
    }
}
