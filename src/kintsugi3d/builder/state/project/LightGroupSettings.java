package kintsugi3d.builder.state.project;

import java.util.List;

public interface LightGroupSettings<LightInstanceSettingType extends LightSettings>
{
    void addLight(int index, double targetX, double targetY, double targetZ);

    void removeLight(int index);

    LightInstanceSettingType constructLightInstanceSetting();

    int getLightCount();

    List<LightInstanceSettingType> getLightList();

    boolean isLocked();

    void setLocked(boolean locked);

    String getName();

    void setName(String name);
}
