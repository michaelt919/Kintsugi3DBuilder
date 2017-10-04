package tetzlaff.ibrelight.core;

import tetzlaff.util.ShadingParameterMode;

public interface ReadonlySettingsModel 
{
    float getGamma();
    float getWeightExponent();
    float getIsotropyFactor();
    float getOcclusionBias();
    ShadingParameterMode getWeightMode();
    boolean isOcclusionEnabled();
    boolean isFresnelEnabled();
    boolean isPBRGeometricAttenuationEnabled();
    boolean isRelightingEnabled();
    boolean is3DGridEnabled();
    boolean isCompassEnabled();
    boolean areVisibleCameraPosesEnabled();
    boolean areVisibleSavedCameraPosesEnabled();
    boolean areShadowsEnabled();
    boolean areVisibleLightsEnabled();
    boolean areLightWidgetsEnabled();
    RenderingMode getRenderingMode();
    boolean isHalfResolutionEnabled();
    boolean isMultisamplingEnabled();
}
