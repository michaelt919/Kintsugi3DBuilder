package kintsugi3d.builder.state;

import kintsugi3d.builder.state.project.LightGroupSettings;

import java.util.function.IntFunction;

public abstract class LightingEnvironmentModelFromSettings<LightType extends DiscreteLightModel> extends LightingEnvironmentModelBase<LightType>
{
    public LightingEnvironmentModelFromSettings(int lightCount, IntFunction<LightType> lightInstanceCreator, EnvironmentModel environmentModel)
    {
        super(lightCount, lightInstanceCreator, environmentModel);
    }

    protected abstract LightGroupSettings<?> getLightGroupSetting();

    @Override
    public int getLightCount()
    {
        return getLightGroupSetting().getLightCount();
    }

    @Override
    public int getMaxLightCount()
    {
        return 4;
    }

    @Override
    public boolean isLightWidgetEnabled(int index)
    {
        LightGroupSettings<?> activeLightGroup = getLightGroupSetting();
        return !activeLightGroup.isLocked() && !activeLightGroup.getLightList().get(index).isLocked();
    }
}
