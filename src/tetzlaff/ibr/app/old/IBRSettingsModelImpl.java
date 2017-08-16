package tetzlaff.ibr.app.old;

import tetzlaff.ibr.SettingsModel;
import tetzlaff.ibr.RenderingMode;
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

    public float getGamma()
    {
        return this.gamma;
    }

    public void setGamma(float gamma)
    {
        this.gamma = gamma;
    }

    public float getWeightExponent()
    {
        return this.weightExponent;
    }

    public void setWeightExponent(float weightExponent)
    {
        this.weightExponent = weightExponent;
    }

    public float getIsotropyFactor()
    {
        return isotropyFactor;
    }

    public void setIsotropyFactor(float isotropyFactor)
    {
        this.isotropyFactor = isotropyFactor;
    }

    public boolean isOcclusionEnabled()
    {
        return this.occlusionEnabled;
    }

    public void setOcclusionEnabled(boolean occlusionEnabled)
    {
        this.occlusionEnabled = occlusionEnabled;
    }

    public float getOcclusionBias()
    {
        return this.occlusionBias;
    }

    public void setOcclusionBias(float occlusionBias)
    {
        this.occlusionBias = occlusionBias;
    }

    public boolean isIBREnabled()
    {
        return this.ibrEnabled;
    }

    public void setIBREnabled(boolean ibrEnabled)
    {
        this.ibrEnabled = ibrEnabled;
    }

    public boolean isFresnelEnabled()
    {
        return this.fresnelEnabled;
    }

    public void setFresnelEnabled(boolean fresnelEnabled)
    {
        this.fresnelEnabled = fresnelEnabled;
    }

    public boolean isPBRGeometricAttenuationEnabled()
    {
        return this.pbrGeometricAttenuationEnabled;
    }

    public void setPBRGeometricAttenuationEnabled(boolean pbrGeometricAttenuationEnabled)
    {
        this.pbrGeometricAttenuationEnabled = pbrGeometricAttenuationEnabled;
    }

    public boolean isRelightingEnabled()
    {
        return relightingEnabled;
    }

    public void setRelightingEnabled(boolean relightingEnabled)
    {
        this.relightingEnabled = relightingEnabled;
    }

    public boolean areTexturesEnabled()
    {
        return texturesEnabled;
    }

    public void setTexturesEnabled(boolean texturesEnabled)
    {
        this.texturesEnabled = texturesEnabled;
    }

    public boolean areShadowsEnabled()
    {
        return shadowsEnabled;
    }

    public void setShadowsEnabled(boolean shadowsEnabled)
    {
        this.shadowsEnabled = shadowsEnabled;
    }

    public boolean areVisibleLightsEnabled()
    {
        return this.visibleLightsEnabled;
    }

    public void setVisibleLightsEnabled(boolean visibleLightsEnabled)
    {
        this.visibleLightsEnabled = visibleLightsEnabled;
    }

    public ShadingParameterMode getWeightMode()
    {
        return weightMode;
    }

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

