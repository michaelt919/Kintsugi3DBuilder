package tetzlaff.ibrelight.javafx.multithread;

import tetzlaff.ibrelight.javafx.controllers.scene.lights.LightGroupSetting;
import tetzlaff.ibrelight.javafx.util.MultithreadValue;
import tetzlaff.models.ExtendedLightingModel;
import tetzlaff.models.impl.ExtendedLightingModelBase;

public class LightingModelWrapper extends ExtendedLightingModelBase<LightInstanceModelWrapper>
{
   private final ExtendedLightingModel baseModel;
    private final MultithreadValue<Integer> selectedLightIndex;

    public LightingModelWrapper(ExtendedLightingModel baseModel)
    {
        super(LightGroupSetting.LIGHT_LIMIT,
            index -> new LightInstanceModelWrapper(baseModel.getLight(index)),
            new EnvironmentModelWrapper(baseModel.getEnvironmentModel()));
        this.baseModel = baseModel;
        this.selectedLightIndex = MultithreadValue.createFromFunctions(baseModel::getSelectedLightIndex, baseModel::setSelectedLightIndex);
    }

    @Override
    public int getLightCount()
    {
        return baseModel.getLightCount();
    }

    @Override
    public boolean isLightWidgetEnabled(int index)
    {
        return baseModel.isLightWidgetEnabled(index);
    }

    @Override
    public int getSelectedLightIndex()
    {
        return selectedLightIndex.getValue();
    }

    @Override
    public void setSelectedLightIndex(int index)
    {
        this.selectedLightIndex.setValue(index);
    }
}
