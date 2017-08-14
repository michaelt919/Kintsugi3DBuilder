package tetzlaff.ibr;//Created by alexk on 7/31/2017.

import tetzlaff.util.ShadingParameterMode;

public interface IBRSettingsModel extends ReadonlyIBRSettingsModel
{
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
    public void setRenderingMode(RenderingMode renderingMode);
    public void setWeightMode(ShadingParameterMode weightMode);
}
