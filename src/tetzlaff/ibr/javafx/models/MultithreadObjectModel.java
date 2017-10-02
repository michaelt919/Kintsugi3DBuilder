package tetzlaff.ibr.javafx.models;

import javafx.application.Platform;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.ExtendedObjectModel;
import tetzlaff.models.impl.SimpleExtendedObjectModel;

public class MultithreadObjectModel extends SimpleExtendedObjectModel
{
    private final ExtendedObjectModel baseModel;

    public MultithreadObjectModel(ExtendedObjectModel baseModel)
    {
        this.baseModel = baseModel;
    }

    @Override
    public boolean isLocked()
    {
        return baseModel.isLocked();
    }

    @Override
    public void setCenter(Vector3 center)
    {
        super.setCenter(center);
        Platform.runLater(() -> baseModel.setCenter(center));
    }

    @Override
    public void setRotationZ(float rotationZ)
    {
        super.setRotationZ(rotationZ);
        Platform.runLater(() -> baseModel.setRotationZ(rotationZ));
    }

    @Override
    public void setRotationY(float rotationY)
    {
        super.setRotationY(rotationY);
        Platform.runLater(() -> baseModel.setRotationY(rotationY));
    }

    @Override
    public void setRotationX(float rotationX)
    {
        super.setRotationX(rotationX);
        Platform.runLater(() -> baseModel.setRotationX(rotationX));
    }
}
