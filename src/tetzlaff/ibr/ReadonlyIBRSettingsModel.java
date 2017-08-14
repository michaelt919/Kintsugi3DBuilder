package tetzlaff.ibr;

import tetzlaff.util.ShadingParameterMode;

public interface ReadonlyIBRSettingsModel 
{
	public float getGamma();
    public float getWeightExponent();
    public float getIsotropyFactor();
    public float getOcclusionBias();
    public ShadingParameterMode getWeightMode();
    public boolean isOcclusionEnabled();
    public boolean isIBREnabled();
    public boolean isFresnelEnabled();
    public boolean isPBRGeometricAttenuationEnabled();
    public boolean isRelightingEnabled();
    public boolean isD3GridEnabled();
    public boolean isCompassEnabled();
    public boolean isVisibleCameraPose();
    public boolean isVisibleSavedCameraPose();
    public boolean isMaterialsForIBR();
    public boolean isPhyMasking();
    public boolean areTexturesEnabled();
    public boolean areShadowsEnabled();
    public boolean areVisibleLightsEnabled();
    public RenderingMode getRenderingMode();
}
