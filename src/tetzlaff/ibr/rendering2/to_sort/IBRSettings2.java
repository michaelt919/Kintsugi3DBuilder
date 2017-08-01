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


    @Deprecated
    public void setGamma(float gamma) ;
    @Deprecated
    public void setWeightExponent(float weightExponent) ;
    @Deprecated
    public void setIsotropyFactor(float isotropyFactor) ;
    @Deprecated
    public void setOcclusionEnabled(boolean occlusionEnabled) ;
    @Deprecated
    public void setOcclusionBias(float occlusionBias) ;
    @Deprecated
    public void setIBREnabled(boolean ibrEnabled) ;
    @Deprecated
    public void setFresnelEnabled(boolean fresnelEnabled) ;
    @Deprecated
    public void setPBRGeometricAttenuationEnabled(boolean pbrGeometricAttenuationEnabled) ;
    @Deprecated
    public void setRelightingEnabled(boolean relightingEnabled) ;
    @Deprecated
    public void setTexturesEnabled(boolean texturesEnabled) ;
    @Deprecated
    public void setShadowsEnabled(boolean shadowsEnabled) ;
    @Deprecated
    public void setVisibleLightsEnabled(boolean visibleLightsEnabled);
    @Deprecated
    public void setWeightMode(ShadingParameterMode weightMode) ;
}
