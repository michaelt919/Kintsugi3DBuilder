package tetzlaff.ibr.javafx.models;//Created by alexk on 7/21/2017.

import com.sun.istack.internal.NotNull;
import javafx.beans.value.ObservableValue;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.javafx.controllers.scene.object.ObjectPoseSetting;
import tetzlaff.models.ExtendedObjectModel;
import tetzlaff.util.OrbitPolarConverter;

public class JavaFXObjectModel implements ExtendedObjectModel
{
    private ObservableValue<ObjectPoseSetting> selectedObjectPoseSettingProperty;
    private final ObjectPoseSetting backup = new ObjectPoseSetting(
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        false,
        "backup"
    );

    public void setSelectedObjectPoseSettingProperty(ObservableValue<ObjectPoseSetting> selectedObjectPoseSettingProperty)
    {
        this.selectedObjectPoseSettingProperty = selectedObjectPoseSettingProperty;
    }

    @NotNull
    private ObjectPoseSetting getActiveObjectPoseSetting()
    {
        if (selectedObjectPoseSettingProperty == null || selectedObjectPoseSettingProperty.getValue() == null)
        {
            return backup;
        }
        else
        {
            return selectedObjectPoseSettingProperty.getValue();
        }
    }

    private Matrix4 orbitCache;
    private boolean fromRender = false;

    @Override
    public Matrix4 getTransformationMatrix()
    {
        return getOrbit().times(Matrix4.translate(getCenter().negated()));
    }

    @Override
    public Matrix4 getOrbit()
    {
        if (fromRender)
        {
            fromRender = false;
            return orbitCache;
        }
        Vector3 polar = new Vector3((float) getActiveObjectPoseSetting().getRotateY(), (float) getActiveObjectPoseSetting().getRotateX(), (float) getActiveObjectPoseSetting().getRotateZ());
        return OrbitPolarConverter.getInstance().convertToOrbitMatrix(polar);
    }

    @Override
    public void setOrbit(Matrix4 orbit)
    {
        Vector3 polar = OrbitPolarConverter.getInstance().convertToPolarCoordinates(orbit);
        getActiveObjectPoseSetting().setRotateY(polar.x);
        getActiveObjectPoseSetting().setRotateX(polar.y);
        getActiveObjectPoseSetting().setRotateZ(polar.z);
        orbitCache = orbit;
        fromRender = true;
    }

    @Override
    public Vector3 getCenter()
    {
        return new Vector3((float) getActiveObjectPoseSetting().getCenterX(),
            (float) getActiveObjectPoseSetting().getCenterY(),
            (float) getActiveObjectPoseSetting().getCenterZ());
    }

    @Override
    public void setCenter(Vector3 center)
    {
        getActiveObjectPoseSetting().setCenterX(center.x);
        getActiveObjectPoseSetting().setCenterY(center.y);
        getActiveObjectPoseSetting().setCenterZ(center.z);
    }

    @Override
    public float getRotationZ()
    {
        return (float) getActiveObjectPoseSetting().getRotateZ();
    }

    @Override
    public void setRotationZ(float rotationZ)
    {
        getActiveObjectPoseSetting().setRotateZ(rotationZ);
    }

    @Override
    public float getRotationY()
    {
        return (float) getActiveObjectPoseSetting().getRotateY();
    }

    @Override
    public void setRotationY(float rotationY)
    {
        getActiveObjectPoseSetting().setRotateY(rotationY);
    }

    @Override
    public float getRotationX()
    {
        return (float) getActiveObjectPoseSetting().getRotateX();
    }

    @Override
    public void setRotationX(float rotationX)
    {
        getActiveObjectPoseSetting().setRotateX(rotationX);
    }

    /**
     * this method is intended to return whether or not the selected pose is locked.
     * It is called by the render side of the program, and when it returns true
     * the pose should not be able to be changed using the tools in the render window.
     *
     * @return true for locked
     */
    @Override
    public boolean isLocked()
    {
        return getActiveObjectPoseSetting().isLocked();
    }

    @Override
    public void setTransformationMatrix(Matrix4 transformationMatrix)
    {
        throw new UnsupportedOperationException();
    }
}
