package tetzlaff.ibr.alexkautz_workspace.render.new_tool_setup_rename_this_later;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.mvc.models.ReadonlyCameraModel;

public class CameraModelX implements ReadonlyCameraModel {

    private Float zoom;
    private Vector3 offSet;
    private Matrix4 orbit;

    public final static Vector3 ORIGIN = new Vector3(0,0,0);

    public CameraModelX() {
        zoom = 1f;
        offSet = new Vector3(0,0,0);
        orbit = Matrix4.IDENTITY;
    }

    @Override
    public Matrix4 getLookMatrix() {
        return Matrix4.lookAt( new Vector3(0, 0, 1/zoom), ORIGIN, new Vector3(0,1,0))

                .times(Matrix4.translate(offSet)

                .times(orbit));
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
