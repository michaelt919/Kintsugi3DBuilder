package tetzlaff.ibr.javafx.models;//Created by alexk on 7/31/2017.

import javafx.beans.property.*;
import tetzlaff.ibr.core.RenderingMode;
import tetzlaff.ibr.core.SettingsModel;
import tetzlaff.ibr.javafx.util.StaticUtilities;
import tetzlaff.util.ShadingParameterMode;

public class JavaFXSettingsModel implements SettingsModel
{
    private final BooleanProperty occlusion = new SimpleBooleanProperty(true);
    private final BooleanProperty fresnel = new SimpleBooleanProperty(false);
    private final BooleanProperty pbrGeometricAttenuation = new SimpleBooleanProperty(false);
    private final BooleanProperty relighting = new SimpleBooleanProperty(true);
    private final BooleanProperty shadows = new SimpleBooleanProperty(false);
    private final BooleanProperty visibleLights = new SimpleBooleanProperty(true);
    private final BooleanProperty visibleLightWidgets = new SimpleBooleanProperty(false);
    private final BooleanProperty visibleCameraPose = new SimpleBooleanProperty(false);
    private final BooleanProperty visibleSavedCameraPose = new SimpleBooleanProperty(false);
    private final FloatProperty gamma = StaticUtilities.bound(1, 5, new SimpleFloatProperty(2.2f));
    private final FloatProperty weightExponent = StaticUtilities.bound(1, 100, new SimpleFloatProperty(16.0f));
    private final FloatProperty isotropyFactor = StaticUtilities.bound(0, 1, new SimpleFloatProperty(0.0f));
    private final FloatProperty occlusionBias = StaticUtilities.bound(0, 0.1, new SimpleFloatProperty(0.0025f));
    private final ObjectProperty<ShadingParameterMode> weightMode = new SimpleObjectProperty<>(ShadingParameterMode.PER_PIXEL);
    private final ObjectProperty<RenderingMode> renderingMode = new SimpleObjectProperty<>(RenderingMode.IMAGE_BASED);
    private final BooleanProperty is3DGridEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty compassEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty multisamplingEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty halfResolutionEnabled = new SimpleBooleanProperty(false);

    @Override
    public boolean areVisibleCameraPosesEnabled()
    {
        return visibleCameraPose.get();
    }

    public BooleanProperty visibleCameraPoseProperty()
    {
        return visibleCameraPose;
    }

    @Override
    public boolean areVisibleSavedCameraPosesEnabled()
    {
        return visibleSavedCameraPose.get();
    }

    public BooleanProperty visibleSavedCameraPoseProperty()
    {
        return visibleSavedCameraPose;
    }

    @Override
    public boolean is3DGridEnabled()
    {
        return is3DGridEnabled.get();
    }

    public BooleanProperty is3DGridEnabledProperty()
    {
        return is3DGridEnabled;
    }

    @Override
    public boolean isCompassEnabled()
    {
        return compassEnabled.get();
    }

    public BooleanProperty compassEnabledProperty()
    {
        return compassEnabled;
    }

    @Override
    public boolean isOcclusionEnabled()
    {
        return occlusion.get();
    }

    public BooleanProperty occlusionProperty()
    {
        return occlusion;
    }

    @Override
    public boolean isFresnelEnabled()
    {
        return fresnel.get();
    }

    public BooleanProperty fresnelProperty()
    {
        return fresnel;
    }

    @Override
    public boolean isPBRGeometricAttenuationEnabled()
    {
        return pbrGeometricAttenuation.get();
    }

    public BooleanProperty pbrGeometricAttenuationProperty()
    {
        return pbrGeometricAttenuation;
    }

    @Override
    public boolean isRelightingEnabled()
    {
        return relighting.get();
    }

    public BooleanProperty relightingProperty()
    {
        return relighting;
    }

    @Override
    public boolean areShadowsEnabled()
    {
        return shadows.get();
    }

    public BooleanProperty shadowsProperty()
    {
        return shadows;
    }

    @Override
    public boolean areVisibleLightsEnabled()
    {
        return visibleLights.get();
    }

    public BooleanProperty visibleLightsProperty()
    {
        return visibleLights;
    }

    @Override
    public boolean areLightWidgetsEnabled()
    {
        return visibleLightWidgets.get();
    }

    public BooleanProperty visibleLightWidgetsProperty()
    {
        return visibleLightWidgets;
    }

    @Override
    public float getGamma()
    {
        return gamma.get();
    }

    public FloatProperty gammaProperty()
    {
        return gamma;
    }

    @Override
    public float getWeightExponent()
    {
        return weightExponent.get();
    }

    public FloatProperty weightExponentProperty()
    {
        return weightExponent;
    }

    @Override
    public float getIsotropyFactor()
    {
        return isotropyFactor.get();
    }

    public FloatProperty isotropyFactorProperty()
    {
        return isotropyFactor;
    }

    @Override
    public float getOcclusionBias()
    {
        return occlusionBias.get();
    }

    public FloatProperty occlusionBiasProperty()
    {
        return occlusionBias;
    }

    @Override
    public ShadingParameterMode getWeightMode()
    {
        return weightMode.get();
    }

    public ObjectProperty<ShadingParameterMode> weightModeProperty()
    {
        return weightMode;
    }

    @Override
    public RenderingMode getRenderingMode()
    {
        return renderingMode.get();
    }

    public ObjectProperty<RenderingMode> renderingModeProperty()
    {
        return renderingMode;
    }

    @Override
    public boolean isHalfResolutionEnabled()
    {
        return halfResolutionEnabled.get();
    }

    public BooleanProperty halfResolutionEnabledProperty()
    {
        return this.halfResolutionEnabled;
    }

    @Override
    public boolean isMultisamplingEnabled()
    {
        return multisamplingEnabled.get();
    }

    public BooleanProperty multisamplingEnabledProperty()
    {
        return this.multisamplingEnabled;
    }

    @Override
    public void setGamma(float gamma)
    {
        this.gamma.set(gamma);
    }

    @Override
    public void setWeightExponent(float weightExponent)
    {
        this.weightExponent.set(weightExponent);
    }

    @Override
    public void setIsotropyFactor(float isotropyFactor)
    {
        this.isotropyFactor.set(isotropyFactor);
    }

    @Override
    public void setOcclusionEnabled(boolean occlusionEnabled)
    {
       this.occlusion.set(occlusionEnabled);
    }

    @Override
    public void setOcclusionBias(float occlusionBias)
    {
        this.occlusionBias.set(occlusionBias);
    }

    @Override
    public void setFresnelEnabled(boolean fresnelEnabled)
    {
        this.fresnel.set(fresnelEnabled);
    }

    @Override
    public void setPBRGeometricAttenuationEnabled(boolean pbrGeometricAttenuation)
    {
        this.pbrGeometricAttenuation.set(pbrGeometricAttenuation);
    }

    @Override
    public void setRelightingEnabled(boolean relightingEnabled)
    {
        this.relighting.set(relightingEnabled);
    }

    @Override
    public void set3DGridEnabled(boolean is3DGridEnabled)
    {
        this.is3DGridEnabled.set(is3DGridEnabled);
    }

    @Override
    public void setCompassEnabled(boolean compassEnabled)
    {
        this.compassEnabled.set(compassEnabled);
    }

    @Override
    public void setVisibleCameraPosesEnabled(boolean visibleCameraPosesEnabled)
    {
        this.visibleCameraPose.set(visibleCameraPosesEnabled);
    }

    @Override
    public void setVisibleSavedCameraPosesEnabled(boolean visibleSavedCameraPosesEnabled)
    {
        this.visibleSavedCameraPose.set(visibleSavedCameraPosesEnabled);
    }

    @Override
    public void setShadowsEnabled(boolean shadowsEnabled)
    {
        this.shadows.set(shadowsEnabled);
    }

    @Override
    public void setVisibleLightsEnabled(boolean visibleLightsEnabled)
    {
        this.visibleLights.set(visibleLightsEnabled);
    }

    @Override
    public void setLightWidgetsEnabled(boolean lightWidgetsEnabled)
    {
        this.visibleLightWidgets.set(lightWidgetsEnabled);
    }

    @Override
    public void setRenderingMode(RenderingMode renderingMode)
    {
        this.renderingMode.set(renderingMode);
    }

    @Override
    public void setWeightMode(ShadingParameterMode weightMode)
    {
        this.weightMode.set(weightMode);
    }

    @Override
    public void setHalfResolutionEnabled(boolean halfResEnabled)
    {
        this.halfResolutionEnabled.set(halfResEnabled);
    }

    @Override
    public void setMultisamplingEnabled(boolean multisamplingEnabled)
    {
        this.multisamplingEnabled.set(multisamplingEnabled);
    }
}