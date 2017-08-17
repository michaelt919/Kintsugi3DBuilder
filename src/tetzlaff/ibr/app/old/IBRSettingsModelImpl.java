package tetzlaff.ibr.app.old;

import tetzlaff.ibr.RenderingMode;
import tetzlaff.ibr.SettingsModel;
import tetzlaff.util.ShadingParameterMode;

public class IBRSettingsModelImpl implements SettingsModel
{
    private float gamma = 2.2f;
    private float weightExponent = 16.0f;
    private float isotropyFactor = 0.5f;
    private boolean occlusionEnabled = true;
    private float occlusionBias = 0.0025f;
    private boolean ibrEnabled = true;
    private boolean relightingEnabled = true;
    private boolean texturesEnabled = false;
    private boolean shadowsEnabled = false;
    private boolean fresnelEnabled = false;
    private boolean pbrGeometricAttenuationEnabled = false;
    private boolean visibleLightsEnabled = true;
    private ShadingParameterMode weightMode = ShadingParameterMode.PER_PIXEL;
    private boolean multisamplingEnabled = false;
    private boolean halfResolutionEnabled = false;

    public IBRSettingsModelImpl()
    {
    }

    @Override
    public float getGamma()
    {
        return this.gamma;
    }

    @Override
    public void setGamma(float gamma)
    {
        this.gamma = gamma;
    }

    @Override
    public float getWeightExponent()
    {
        return this.weightExponent;
    }

    @Override
    public void setWeightExponent(float weightExponent)
    {
        this.weightExponent = weightExponent;
    }

    @Override
    public float getIsotropyFactor()
    {
        return isotropyFactor;
    }

    @Override
    public void setIsotropyFactor(float isotropyFactor)
    {
        this.isotropyFactor = isotropyFactor;
    }

    @Override
    public boolean isOcclusionEnabled()
    {
        return this.occlusionEnabled;
    }

    @Override
    public void setOcclusionEnabled(boolean occlusionEnabled)
    {
        this.occlusionEnabled = occlusionEnabled;
    }

    @Override
    public float getOcclusionBias()
    {
        return this.occlusionBias;
    }

    @Override
    public void setOcclusionBias(float occlusionBias)
    {
        this.occlusionBias = occlusionBias;
    }

    @Override
    public boolean isIBREnabled()
    {
        return this.ibrEnabled;
    }

    @Override
    public void setIBREnabled(boolean ibrEnabled)
    {
        this.ibrEnabled = ibrEnabled;
    }

    @Override
    public boolean isFresnelEnabled()
    {
        return this.fresnelEnabled;
    }

    @Override
    public void setFresnelEnabled(boolean fresnelEnabled)
    {
        this.fresnelEnabled = fresnelEnabled;
    }

    @Override
    public boolean isPBRGeometricAttenuationEnabled()
    {
        return this.pbrGeometricAttenuationEnabled;
    }

    @Override
    public void setPBRGeometricAttenuationEnabled(boolean pbrGeometricAttenuationEnabled)
    {
        this.pbrGeometricAttenuationEnabled = pbrGeometricAttenuationEnabled;
    }

    @Override
    public boolean isRelightingEnabled()
    {
        return relightingEnabled;
    }

    @Override
    public void setRelightingEnabled(boolean relightingEnabled)
    {
        this.relightingEnabled = relightingEnabled;
    }

    @Override
    public boolean areTexturesEnabled()
    {
        return texturesEnabled;
    }

    @Override
    public void setTexturesEnabled(boolean texturesEnabled)
    {
        this.texturesEnabled = texturesEnabled;
    }

    @Override
    public boolean areShadowsEnabled()
    {
        return shadowsEnabled;
    }

    @Override
    public void setShadowsEnabled(boolean shadowsEnabled)
    {
        this.shadowsEnabled = shadowsEnabled;
    }

    @Override
    public boolean areVisibleLightsEnabled()
    {
        return this.visibleLightsEnabled;
    }

    @Override
    public void setVisibleLightsEnabled(boolean visibleLightsEnabled)
    {
        this.visibleLightsEnabled = visibleLightsEnabled;
    }

    @Override
    public ShadingParameterMode getWeightMode()
    {
        return weightMode;
    }

    @Override
    public void setWeightMode(ShadingParameterMode weightMode)
    {
        this.weightMode = weightMode;
    }

    @Override
    @Deprecated
    public RenderingMode getRenderingMode()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void setRenderingMode(RenderingMode renderingType)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean isD3GridEnabled()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean isCompassEnabled()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void setD3GridEnabled(boolean d3GridEnabled)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void setCompassEnabled(boolean compassEnabled)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean isVisibleCameraPose()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean isVisibleSavedCameraPose()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void setVisibleCameraPose(boolean visibleCameraPose)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void setVisibleSavedCameraPose(boolean visibleSavedCameraPose)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean isMaterialsForIBR()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean isPhyMasking()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void setMaterialsForIBR(boolean materialsForIBR)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void setPhyMasking(boolean phyMasking)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMultisamplingEnabled()
    {
        return multisamplingEnabled;
    }

    @Override
    public void setMultisamplingEnabled(boolean multisamplingEnabled)
    {
        this.multisamplingEnabled = multisamplingEnabled;
    }

    @Override
    public boolean isHalfResolutionEnabled()
    {
        return halfResolutionEnabled;
    }

    @Override
    public void setHalfResolutionEnabled(boolean halfResolutionEnabled)
    {
        this.halfResolutionEnabled = halfResolutionEnabled;
    }
}

