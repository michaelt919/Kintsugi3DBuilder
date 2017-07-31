package tetzlaff.ibr.rendering2.to_sort;//Created by alexk on 7/31/2017.

import tetzlaff.util.ShadingParameterMode;

public interface IBRSettings2 {
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
    public boolean areTexturesEnabled();
    public boolean areShadowsEnabled();
    public boolean areVisibleLightsEnabled();
}
