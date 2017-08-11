package tetzlaff.ibr.rendering2;//Created by alexk on 7/31/2017.

import tetzlaff.ibr.gui2.controllers.menu_bar.RenderingType;
import tetzlaff.util.ShadingParameterMode;

public interface IBRSettingsModel {
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
    public RenderingType getRenderingType();
    public void setGamma(float gamma);
    public void setWeightExponent(float weightExponent);
    public void setIsotropyFactor(float isotropyFactor);
    public void setOcclusionEnabled(boolean occlusionEnabled);
    public void setOcclusionBias(float occlusionBias);
    public void setIBREnabled(boolean ibrEnabled);
    public void setFresnelEnabled(boolean fresnelEnabled);
    public void setPBRGeometricAttenuationEnabled(boolean pBRGeometricAttenuation);
    public void setRelightingEnabled(boolean relightingEnabled);
    public void setD3GridEnabled(boolean d3GridEnabled);
    public void setCompassEnabled(boolean compassEnabled);
    public void setVisibleCameraPose(boolean visibleCameraPose);
    public void setVisibleSavedCameraPose(boolean visibleSavedCameraPose);
    public void setMaterialsForIBR(boolean materialsForIBR);
    public void setPhyMasking(boolean phyMasking);
    public void setTexturesEnabled(boolean texturesEnabled);
    public void setShadowsEnabled(boolean shadowsEnabled);
    public void setVisibleLightsEnabled(boolean visibleLightsEnabled);
    public void setRenderingType(RenderingType renderingType);
    public void setWeightMode(ShadingParameterMode weightMode);
}
