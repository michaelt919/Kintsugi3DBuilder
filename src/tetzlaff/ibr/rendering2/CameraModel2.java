package tetzlaff.ibr.rendering2;

import javafx.beans.property.Property;
import kautzTesting.MoreMatrixMath;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibr.gui2.controllers.scene.camera.CameraSetting;
import tetzlaff.mvc.models.ReadonlyCameraModel;

public class CameraModel2 implements ReadonlyCameraModel {

    private Float zoom;
    private Vector3 offSet;
    private Matrix4 orbit;

    public final static Vector3 ORIGIN = new Vector3(0,0,0);

    public CameraModel2() {
        zoom = 1f;
        offSet = new Vector3(0,0,0);
        orbit = Matrix4.IDENTITY;
    }

    public Vector3 getDirectionIn(){
        Vector4 direction = getLookMatrix().times(new Vector4(0,0,1,1));
        return direction.getXYZ();
    }


    @Override
    public Matrix4 getLookMatrix() {
        return Matrix4.lookAt( new Vector3(0, 0, 1/zoom), ORIGIN, new Vector3(0,1,0))
                .times(orbit);
    }

    public Vector3 getOffSet() {
        return offSet;
    }

    public void setOffSet(Vector3 offSet) {
        this.offSet = offSet;
    }

    public Matrix4 getOrbit() {
        return orbit;
    }

    public void setOrbit(Matrix4 orbit) {
        this.orbit = orbit;
    }

    public Float getZoom() {
        return zoom;
    }

    public void setZoom(Float zoom) {
        this.zoom = zoom;
    }
}
