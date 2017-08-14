package tetzlaff.ibr;

import tetzlaff.util.ShadingParameterMode;

public interface ReadonlyIBRSettingsModel 
{
	float getGamma();
    float getWeightExponent();
    float getIsotropyFactor();
    float getOcclusionBias();
    ShadingParameterMode getWeightMode();
    boolean isOcclusionEnabled();
    boolean isIBREnabled();
    boolean isFresnelEnabled();
    boolean isPBRGeometricAttenuationEnabled();
    boolean isRelightingEnabled();
    boolean isD3GridEnabled();
    boolean isCompassEnabled();
    boolean isVisibleCameraPose();
    boolean isVisibleSavedCameraPose();
    boolean isMaterialsForIBR();
    boolean isPhyMasking();
    boolean areTexturesEnabled();
    boolean areShadowsEnabled();
    boolean areVisibleLightsEnabled();
    RenderingMode getRenderingMode();
	boolean isHalfResolutionEnabled();
	boolean isMultisamplingEnabled();
}
