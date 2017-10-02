package tetzlaff.ibr.javafx.models;

import tetzlaff.ibr.javafx.controllers.scene.lights.LightGroupSetting;
import tetzlaff.models.ExtendedLightingModel;
import tetzlaff.models.impl.ExtendedLightingModelBase;

public class MultithreadLightingModel extends ExtendedLightingModelBase<MultithreadLightInstanceModel>
{
    private final ExtendedLightingModel baseModel;

    public MultithreadLightingModel(ExtendedLightingModel baseModel)
    {
        super(LightGroupSetting.LIGHT_LIMIT, index -> new MultithreadLightInstanceModel(baseModel.getLight(index)), baseModel.getEnvironmentModel());
        this.baseModel = baseModel;
    }

    @Override
    public int getLightCount()
    {
        return baseModel.getLightCount();
    }
}
