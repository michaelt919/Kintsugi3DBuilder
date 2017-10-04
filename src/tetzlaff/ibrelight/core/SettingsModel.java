package tetzlaff.ibrelight.core;//Created by alexk on 7/31/2017.

import tetzlaff.util.ShadingParameterMode;

public interface SettingsModel extends ReadonlySettingsModel
{
    void setGamma(float gamma);
    void setWeightExponent(float weightExponent);
    void setIsotropyFactor(float isotropyFactor);
    void setOcclusionEnabled(boolean occlusionEnabled);
    void setOcclusionBias(float occlusionBias);
    void setFresnelEnabled(boolean fresnelEnabled);
    void setPBRGeometricAttenuationEnabled(boolean pbrGeometricAttenuation);
    void setRelightingEnabled(boolean relightingEnabled);
    void set3DGridEnabled(boolean is3DGridEnabled);
    void setCompassEnabled(boolean compassEnabled);
    void setVisibleCameraPosesEnabled(boolean visibleCameraPosesEnabled);
    void setVisibleSavedCameraPosesEnabled(boolean visibleSavedCameraPosesEnabled);
    void setShadowsEnabled(boolean shadowsEnabled);
    void setVisibleLightsEnabled(boolean visibleLightsEnabled);
    void setLightWidgetsEnabled(boolean lightWidgetsEnabled);
    void setRenderingMode(RenderingMode renderingMode);
    void setWeightMode(ShadingParameterMode weightMode);
    void setHalfResolutionEnabled(boolean halfResolutionEnabled);
    void setMultisamplingEnabled(boolean multisamplingEnabled);
}
