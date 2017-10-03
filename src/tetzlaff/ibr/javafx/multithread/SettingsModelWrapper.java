package tetzlaff.ibr.javafx.multithread;

import tetzlaff.ibr.core.RenderingMode;
import tetzlaff.ibr.core.SettingsModel;
import tetzlaff.ibr.javafx.util.MultithreadValue;
import tetzlaff.util.ShadingParameterMode;

public class SettingsModelWrapper implements SettingsModel
{
    private final MultithreadValue<Float> gamma;
    private final MultithreadValue<Float> weightExponent;
    private final MultithreadValue<Float> isotropyFactor;
    private final MultithreadValue<Boolean> occlusionEnabled;
    private final MultithreadValue<Float> occlusionBias;
    private final MultithreadValue<Boolean> fresnelEnabled;
    private final MultithreadValue<Boolean> pbrGeometricAttenuation;
    private final MultithreadValue<Boolean> relightingEnabled;
    private final MultithreadValue<Boolean> is3DGridEnabled;
    private final MultithreadValue<Boolean> compassEnabled;
    private final MultithreadValue<Boolean> visibleCameraPosesEnabled;
    private final MultithreadValue<Boolean> visibleSavedCameraPosesEnabled;
    private final MultithreadValue<Boolean> shadowsEnabled;
    private final MultithreadValue<Boolean> visibleLightsEnabled;
    private final MultithreadValue<Boolean> lightWidgetsEnabled;
    private final MultithreadValue<RenderingMode> renderingMode;
    private final MultithreadValue<ShadingParameterMode> weightMode;
    private final MultithreadValue<Boolean> halfResolutionEnabled;
    private final MultithreadValue<Boolean> multisamplingEnabled;

    public SettingsModelWrapper(SettingsModel baseModel)
    {
        this.gamma                          = MultithreadValue.createFromFunctions(  baseModel::getGamma,                            baseModel::setGamma);
        this.weightExponent                 = MultithreadValue.createFromFunctions(  baseModel::getWeightExponent,                   baseModel::setWeightExponent);
        this.isotropyFactor                 = MultithreadValue.createFromFunctions(  baseModel::getIsotropyFactor,                   baseModel::setIsotropyFactor);
        this.occlusionEnabled               = MultithreadValue.createFromFunctions(  baseModel::isOcclusionEnabled,                  baseModel::setOcclusionEnabled);
        this.occlusionBias                  = MultithreadValue.createFromFunctions(  baseModel::getOcclusionBias,                    baseModel::setOcclusionBias);
        this.fresnelEnabled                 = MultithreadValue.createFromFunctions(  baseModel::isFresnelEnabled,                    baseModel::setFresnelEnabled);
        this.pbrGeometricAttenuation        = MultithreadValue.createFromFunctions(  baseModel::isPBRGeometricAttenuationEnabled,    baseModel::setPBRGeometricAttenuationEnabled);
        this.relightingEnabled              = MultithreadValue.createFromFunctions(  baseModel::isRelightingEnabled,                 baseModel::setRelightingEnabled);
        this.is3DGridEnabled                = MultithreadValue.createFromFunctions(  baseModel::is3DGridEnabled,                     baseModel::set3DGridEnabled);
        this.compassEnabled                 = MultithreadValue.createFromFunctions(  baseModel::isCompassEnabled,                    baseModel::setCompassEnabled);
        this.visibleCameraPosesEnabled      = MultithreadValue.createFromFunctions(  baseModel::areVisibleCameraPosesEnabled,        baseModel::setVisibleCameraPosesEnabled);
        this.visibleSavedCameraPosesEnabled = MultithreadValue.createFromFunctions(  baseModel::areVisibleSavedCameraPosesEnabled,   baseModel::setVisibleSavedCameraPosesEnabled);
        this.shadowsEnabled                 = MultithreadValue.createFromFunctions(  baseModel::areShadowsEnabled,                   baseModel::setShadowsEnabled);
        this.visibleLightsEnabled           = MultithreadValue.createFromFunctions(  baseModel::areVisibleLightsEnabled,             baseModel::setVisibleLightsEnabled);
        this.lightWidgetsEnabled            = MultithreadValue.createFromFunctions(  baseModel::areLightWidgetsEnabled,              baseModel::setLightWidgetsEnabled);
        this.renderingMode                  = MultithreadValue.createFromFunctions(  baseModel::getRenderingMode,                    baseModel::setRenderingMode);
        this.weightMode                     = MultithreadValue.createFromFunctions(  baseModel::getWeightMode,                       baseModel::setWeightMode);
        this.halfResolutionEnabled          = MultithreadValue.createFromFunctions(  baseModel::isHalfResolutionEnabled,             baseModel::setHalfResolutionEnabled);
        this.multisamplingEnabled           = MultithreadValue.createFromFunctions(  baseModel::isMultisamplingEnabled,              baseModel::setMultisamplingEnabled);
    }

    @Override
    public float getGamma()
    {
        return gamma.getValue();
    }

    @Override
    public float getWeightExponent()
    {
        return weightExponent.getValue();
    }

    @Override
    public float getIsotropyFactor()
    {
        return isotropyFactor.getValue();
    }

    @Override
    public float getOcclusionBias()
    {
        return occlusionBias.getValue();
    }

    @Override
    public ShadingParameterMode getWeightMode()
    {
        return weightMode.getValue();
    }

    @Override
    public boolean isOcclusionEnabled()
    {
        return occlusionEnabled.getValue();
    }

    @Override
    public boolean isFresnelEnabled()
    {
        return fresnelEnabled.getValue();
    }

    @Override
    public boolean isPBRGeometricAttenuationEnabled()
    {
        return pbrGeometricAttenuation.getValue();
    }

    @Override
    public boolean isRelightingEnabled()
    {
        return relightingEnabled.getValue();
    }

    @Override
    public boolean is3DGridEnabled()
    {
        return is3DGridEnabled.getValue();
    }

    @Override
    public boolean isCompassEnabled()
    {
        return compassEnabled.getValue();
    }

    @Override
    public boolean areVisibleCameraPosesEnabled()
    {
        return visibleCameraPosesEnabled.getValue();
    }

    @Override
    public boolean areVisibleSavedCameraPosesEnabled()
    {
        return visibleSavedCameraPosesEnabled.getValue();
    }

    @Override
    public boolean areShadowsEnabled()
    {
        return shadowsEnabled.getValue();
    }

    @Override
    public boolean areVisibleLightsEnabled()
    {
        return visibleLightsEnabled.getValue();
    }

    @Override
    public boolean areLightWidgetsEnabled()
    {
        return lightWidgetsEnabled.getValue();
    }

    @Override
    public RenderingMode getRenderingMode()
    {
        return renderingMode.getValue();
    }

    @Override
    public boolean isHalfResolutionEnabled()
    {
        return halfResolutionEnabled.getValue();
    }

    @Override
    public boolean isMultisamplingEnabled()
    {
        return multisamplingEnabled.getValue();
    }

    @Override
    public void setGamma(float gamma)
    {
        this.gamma.setValue(gamma);
    }

    @Override
    public void setWeightExponent(float weightExponent)
    {
        this.weightExponent.setValue(weightExponent);
    }

    @Override
    public void setIsotropyFactor(float isotropyFactor)
    {
        this.isotropyFactor.setValue(isotropyFactor);
    }

    @Override
    public void setOcclusionEnabled(boolean occlusionEnabled)
    {
        this.occlusionEnabled.setValue(occlusionEnabled);
    }

    @Override
    public void setOcclusionBias(float occlusionBias)
    {
        this.occlusionBias.setValue(occlusionBias);
    }

    @Override
    public void setFresnelEnabled(boolean fresnelEnabled)
    {
        this.fresnelEnabled.setValue(fresnelEnabled);
    }

    @Override
    public void setPBRGeometricAttenuationEnabled(boolean pbrGeometricAttenuation)
    {
        this.pbrGeometricAttenuation.setValue(pbrGeometricAttenuation);
    }

    @Override
    public void setRelightingEnabled(boolean relightingEnabled)
    {
        this.relightingEnabled.setValue(relightingEnabled);
    }

    @Override
    public void set3DGridEnabled(boolean is3DGridEnabled)
    {
        this.is3DGridEnabled.setValue(is3DGridEnabled);
    }

    @Override
    public void setCompassEnabled(boolean compassEnabled)
    {
        this.compassEnabled.setValue(compassEnabled);
    }

    @Override
    public void setVisibleCameraPosesEnabled(boolean visibleCameraPosesEnabled)
    {
        this.visibleCameraPosesEnabled.setValue(visibleCameraPosesEnabled);
    }

    @Override
    public void setVisibleSavedCameraPosesEnabled(boolean visibleSavedCameraPosesEnabled)
    {
        this.visibleSavedCameraPosesEnabled.setValue(visibleSavedCameraPosesEnabled);
    }

    @Override
    public void setShadowsEnabled(boolean shadowsEnabled)
    {
        this.shadowsEnabled.setValue(shadowsEnabled);
    }

    @Override
    public void setVisibleLightsEnabled(boolean visibleLightsEnabled)
    {
        this.visibleLightsEnabled.setValue(visibleLightsEnabled);
    }

    @Override
    public void setLightWidgetsEnabled(boolean lightWidgetsEnabled)
    {
        this.lightWidgetsEnabled.setValue(lightWidgetsEnabled);
    }

    @Override
    public void setRenderingMode(RenderingMode renderingMode)
    {
        this.renderingMode.setValue(renderingMode);
    }

    @Override
    public void setWeightMode(ShadingParameterMode weightMode)
    {
        this.weightMode.setValue(weightMode);
    }

    @Override
    public void setHalfResolutionEnabled(boolean halfResolutionEnabled)
    {
        this.halfResolutionEnabled.setValue(halfResolutionEnabled);
    }

    @Override
    public void setMultisamplingEnabled(boolean multisamplingEnabled)
    {
        this.multisamplingEnabled.setValue(multisamplingEnabled);
    }
}
