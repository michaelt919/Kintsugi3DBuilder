package kintsugi3d.builder.state.scene;

import kintsugi3d.builder.state.project.ObjectPoseSettings;
import kintsugi3d.gl.vecmath.Vector3;

public abstract class ObjectPoseModelFromSettings extends ObjectPoseModelBase
{
    protected abstract ObjectPoseSettings getObjectPoseSettings();

    @Override
    public Vector3 getCenter()
    {
        return new Vector3((float) getObjectPoseSettings().getCenterX(),
            (float) getObjectPoseSettings().getCenterY(),
            (float) getObjectPoseSettings().getCenterZ());
    }

    @Override
    public void setCenter(Vector3 center)
    {
        if (!getObjectPoseSettings().isLocked())
        {
            getObjectPoseSettings().setCenterX(center.x);
            getObjectPoseSettings().setCenterY(center.y);
            getObjectPoseSettings().setCenterZ(center.z);
        }
    }

    @Override
    public float getRotationZ()
    {
        return (float) getObjectPoseSettings().getRotateZ();
    }

    @Override
    public void setRotationZ(float rotationZ)
    {
        if (!getObjectPoseSettings().isLocked())
        {
            getObjectPoseSettings().setRotateZ(rotationZ);
        }
    }

    @Override
    public float getRotationY()
    {
        return (float) getObjectPoseSettings().getRotateY();
    }

    @Override
    public void setRotationY(float rotationY)
    {
        if (!getObjectPoseSettings().isLocked())
        {
            getObjectPoseSettings().setRotateY(rotationY);
        }
    }

    @Override
    public float getRotationX()
    {
        return (float) getObjectPoseSettings().getRotateX();
    }

    @Override
    public void setRotationX(float rotationX)
    {
        if (!getObjectPoseSettings().isLocked())
        {
            getObjectPoseSettings().setRotateX(rotationX);
        }
    }

    @Override
    public float getScale()
    {
        return (float) getObjectPoseSettings().getScale();
    }

    @Override
    public void setScale(float scale)
    {
        if (!getObjectPoseSettings().isLocked())
        {
            getObjectPoseSettings().setScale(scale);
        }
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
        return getObjectPoseSettings().isLocked();
    }
}
