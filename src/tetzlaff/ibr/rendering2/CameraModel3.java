package tetzlaff.ibr.rendering2;//Created by alexk on 7/21/2017.

import com.sun.istack.internal.NotNull;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.gui2.controllers.scene.camera.CameraSetting;
import tetzlaff.ibr.gui2.other.OrbitPolarConverter;
import tetzlaff.mvc.models.ControllableCameraModel;

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

    @Override
    public Matrix4 getLookMatrix() {
        return Matrix4.lookAt(
                new Vector3(0,0,1/getZoom()),
                Vector3.ZERO,
                new Vector3(0,1,0)
        ).times(getOrbit());
    }

    @Override
    public Matrix4 getOrbit() {
        Vector3 poler = new Vector3((float) cam().getAzimuth(), (float) cam().getInclination(), (float) cam().getTwist());
        return OrbitPolarConverter.self.convertLeft(poler);
    }

    @Override
    public void setOrbit(Matrix4 orbit) {
        Vector3 poler = OrbitPolarConverter.self.convertRight(orbit);
        cam().setAzimuth(poler.x);
        cam().setInclination(poler.y);
        cam().setTwist(poler.z);
    }

    @Override
    public Float getZoom() {
        return (float) cam().getDistance();
    }

    @Override
    public void setZoom(Float zoom) {
        cam().setDistance(zoom);
    }

    @Override
    public Vector3 getOffSet() {
        return Vector3.ZERO;
    }

    @Override
    public void setOffSet(Vector3 offSet) {

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
