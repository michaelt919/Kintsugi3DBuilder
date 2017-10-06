package tetzlaff.ibrelight.app.old;

import tetzlaff.ibrelight.core.RenderingMode;
import tetzlaff.models.SettingsModel;
import tetzlaff.util.ShadingParameterMode;

public class IBRSettingsModelImpl implements SettingsModel
{
    private float gamma = 2.2f;
    private float weightExponent = 16.0f;
    private float isotropyFactor = 0.5f;
    private boolean occlusionEnabled = true;
    private float occlusionBias = 0.0025f;
    private boolean relightingEnabled = true;
    private boolean shadowsEnabled = false;
    private boolean fresnelEnabled = false;
    private boolean pbrGeometricAttenuationEnabled = false;
    private boolean visibleLightsEnabled = true;
    private ShadingParameterMode weightMode = ShadingParameterMode.PER_PIXEL;
    private boolean multisamplingEnabled = false;
    private boolean halfResolutionEnabled = false;
    private boolean lightWidgetsEnabled = false;

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
    public void setPBRGeometricAttenuationEnabled(boolean pbrGeometricAttenuation)
    {
        this.pbrGeometricAttenuationEnabled = pbrGeometricAttenuation;
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
    public boolean areLightWidgetsEnabled()
    {
        return this.lightWidgetsEnabled;
    }

    @Override
    public void setVisibleLightsEnabled(boolean visibleLightsEnabled)
    {
        this.visibleLightsEnabled = visibleLightsEnabled;
    }

    @Override
    public void setLightWidgetsEnabled(boolean lightWidgetsEnabled)
    {
        this.lightWidgetsEnabled = lightWidgetsEnabled;
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
    public boolean is3DGridEnabled()
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
    public void set3DGridEnabled(boolean is3DGridEnabled)
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
    public boolean areVisibleCameraPosesEnabled()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean areVisibleSavedCameraPosesEnabled()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void setVisibleCameraPosesEnabled(boolean visibleCameraPosesEnabled)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void setVisibleSavedCameraPosesEnabled(boolean visibleSavedCameraPosesEnabled)
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

