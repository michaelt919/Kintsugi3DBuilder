package tetzlaff.ibr.javafx.multithread;

import tetzlaff.ibr.javafx.controllers.scene.lights.LightGroupSetting;
import tetzlaff.models.ExtendedLightingModel;
import tetzlaff.models.impl.ExtendedLightingModelBase;

public class LightingModelWrapper extends ExtendedLightingModelBase<LightInstanceModelWrapper>
{
    private final ExtendedLightingModel baseModel;

    public LightingModelWrapper(ExtendedLightingModel baseModel)
    {
        super(LightGroupSetting.LIGHT_LIMIT,
            index -> new LightInstanceModelWrapper(baseModel.getLight(index)),
            new EnvironmentModelWrapper(baseModel.getEnvironmentModel()));
        this.baseModel = baseModel;
    }

    @Override
    public int getLightCount()
    {
        return baseModel.getLightCount();
    }
}
