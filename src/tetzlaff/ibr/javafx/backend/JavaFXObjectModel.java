package tetzlaff.ibr.javafx.backend;//Created by alexk on 7/21/2017.

import com.sun.istack.internal.NotNull;
import javafx.beans.value.ObservableValue;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.javafx.controllers.scene.object.ObjectPoseSetting;
import tetzlaff.models.impl.ExtendedObjectModelBase;

public class JavaFXObjectModel extends ExtendedObjectModelBase
{
    private ObservableValue<ObjectPoseSetting> selectedObjectPoseProperty;
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

    public void setSelectedObjectPoseProperty(ObservableValue<ObjectPoseSetting> selectedObjectPoseProperty)
    {
        this.selectedObjectPoseProperty = selectedObjectPoseProperty;
    }

    @NotNull
    private ObjectPoseSetting getSelectedObjectPose()
    {
        if (selectedObjectPoseProperty == null || selectedObjectPoseProperty.getValue() == null)
        {
            return backup;
        }
        else
        {
            return selectedObjectPoseProperty.getValue();
        }
    }

    @Override
    public Matrix4 getTransformationMatrix()
    {
        return getOrbit().times(Matrix4.translate(getCenter().negated()));
    }

    @Override
    public Vector3 getCenter()
    {
        return new Vector3((float) getSelectedObjectPose().getCenterX(),
            (float) getSelectedObjectPose().getCenterY(),
            (float) getSelectedObjectPose().getCenterZ());
    }

    @Override
    public void setCenter(Vector3 center)
    {
        getSelectedObjectPose().setCenterX(center.x);
        getSelectedObjectPose().setCenterY(center.y);
        getSelectedObjectPose().setCenterZ(center.z);
    }

    @Override
    public float getRotationZ()
    {
        return (float) getSelectedObjectPose().getRotateZ();
    }

    @Override
    public void setRotationZ(float rotationZ)
    {
        getSelectedObjectPose().setRotateZ(rotationZ);
    }

    @Override
    public float getRotationY()
    {
        return (float) getSelectedObjectPose().getRotateY();
    }

    @Override
    public void setRotationY(float rotationY)
    {
        getSelectedObjectPose().setRotateY(rotationY);
    }

    @Override
    public float getRotationX()
    {
        return (float) getSelectedObjectPose().getRotateX();
    }

    @Override
    public void setRotationX(float rotationX)
    {
        getSelectedObjectPose().setRotateX(rotationX);
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
        return getSelectedObjectPose().isLocked();
    }
}
