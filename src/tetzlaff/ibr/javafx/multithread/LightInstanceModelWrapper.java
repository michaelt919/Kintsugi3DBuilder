package tetzlaff.ibr.javafx.multithread;

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.javafx.util.MultithreadValue;
import tetzlaff.models.LightInstanceModel;
import tetzlaff.models.impl.LightInstanceModelBase;

public class LightInstanceModelWrapper extends LightInstanceModelBase
{
    private final LightInstanceModel baseModel;
    private final MultithreadValue<Vector3> color;

    public LightInstanceModelWrapper(LightInstanceModel baseModel)
    {
        super(new CameraModelWrapper(baseModel));
        this.baseModel = baseModel;

        this.color = MultithreadValue.createFromFunctions(baseModel::getColor, baseModel::setColor);
    }

    @Override
    public boolean isEnabled()
    {
        return this.baseModel.isEnabled();
    }

    @Override
    public Vector3 getColor()
    {
        return color.getValue();
    }

    @Override
    public void setColor(Vector3 color)
    {
        this.color.setValue(color);
    }
}
