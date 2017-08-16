package tetzlaff.ibr.javafx.models;//Created by alexk on 7/31/2017.

import javafx.beans.property.*;
import tetzlaff.ibr.RenderingMode;
import tetzlaff.ibr.SettingsModel;
import tetzlaff.ibr.javafx.util.StaticUtilities;
import tetzlaff.util.ShadingParameterMode;

public class JavaFXSettingsModel implements SettingsModel
{
    private final BooleanProperty occlusion = new SimpleBooleanProperty(true);
    private final BooleanProperty fresnel = new SimpleBooleanProperty(false);
    private final BooleanProperty pBRGeometricAttenuation = new SimpleBooleanProperty(false);
    private final BooleanProperty relighting = new SimpleBooleanProperty(true);
    private final BooleanProperty textures = new SimpleBooleanProperty(false);
    private final BooleanProperty shadows = new SimpleBooleanProperty(false);
    private final BooleanProperty visibleLights = new SimpleBooleanProperty(true);
    private final BooleanProperty visibleCameraPose = new SimpleBooleanProperty(false);
    private final BooleanProperty visibleSavedCameraPose = new SimpleBooleanProperty(false);
    private final FloatProperty gamma = StaticUtilities.bound(1, 5, new SimpleFloatProperty(2.2f));
    private final FloatProperty weightExponent = StaticUtilities.bound(1, 100, new SimpleFloatProperty(16f));
    private final FloatProperty isotropyFactor = StaticUtilities.bound(0, 1, new SimpleFloatProperty(0.0f));
    private final FloatProperty occlusionBias = StaticUtilities.bound(0, 0.1, new SimpleFloatProperty(0.0025f));
    private final ObjectProperty<ShadingParameterMode> weightMode = new SimpleObjectProperty<>(ShadingParameterMode.PER_PIXEL);
    private final ObjectProperty<RenderingMode> renderingType = new SimpleObjectProperty<>(RenderingMode.IMAGE_BASED_RENDERING);
    private final BooleanProperty d3GridEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty compassEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty materialsForIBR = new SimpleBooleanProperty(false);
    private final BooleanProperty phyMasking = new SimpleBooleanProperty(false);
    private final BooleanProperty multisamplingEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty halfResolutionEnabled = new SimpleBooleanProperty(false);

    @Override
    public boolean isMaterialsForIBR()
    {
        return materialsForIBR.get();
    }

    public BooleanProperty materialsForIBRProperty()
    {
        return materialsForIBR;
    }

    @Override
    public void setMaterialsForIBR(boolean materialsForIBR)
    {
        this.materialsForIBR.set(materialsForIBR);
    }

    @Override
    public boolean isPhyMasking()
    {
        return phyMasking.get();
    }

    public BooleanProperty phyMaskingProperty()
    {
        return phyMasking;
    }

    @Override
    public void setPhyMasking(boolean phyMasking)
    {
        this.phyMasking.set(phyMasking);
    }

    @Override
    public boolean isVisibleCameraPose()
    {
        return visibleCameraPose.get();
    }

    public BooleanProperty visibleCameraPoseProperty()
    {
        return visibleCameraPose;
    }

    @Override
    public void setVisibleCameraPose(boolean visibleCameraPose)
    {
        this.visibleCameraPose.set(visibleCameraPose);
    }

    @Override
    public boolean isVisibleSavedCameraPose()
    {
        return visibleSavedCameraPose.get();
    }

    public BooleanProperty visibleSavedCameraPoseProperty()
    {
        return visibleSavedCameraPose;
    }

    @Override
    public void setVisibleSavedCameraPose(boolean visibleSavedCameraPose)
    {
        this.visibleSavedCameraPose.set(visibleSavedCameraPose);
    }

    @Override
    public boolean isD3GridEnabled()
    {
        return d3GridEnabled.get();
    }

    public BooleanProperty d3GridEnabledProperty()
    {
        return d3GridEnabled;
    }

    @Override
    public void setD3GridEnabled(boolean d3GridEnabled)
    {
        this.d3GridEnabled.set(d3GridEnabled);
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
    public void setCompassEnabled(boolean compassEnabled)
    {
        this.compassEnabled.set(compassEnabled);
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
    public void setOcclusionEnabled(boolean occlusion)
    {
        this.occlusion.set(occlusion);
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
    public void setFresnelEnabled(boolean fresnel)
    {
        this.fresnel.set(fresnel);
    }

    @Override
    public boolean isPBRGeometricAttenuationEnabled()
    {
        return pBRGeometricAttenuation.get();
    }

    public BooleanProperty pBRGeometricAttenuationProperty()
    {
        return pBRGeometricAttenuation;
    }

    @Override
    public void setPBRGeometricAttenuationEnabled(boolean pBRGeometricAttenuation)
    {
        this.pBRGeometricAttenuation.set(pBRGeometricAttenuation);
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
    public void setRelightingEnabled(boolean relighting)
    {
        this.relighting.set(relighting);
    }

    @Override
    public boolean areTexturesEnabled()
    {
        return textures.get();
    }

    public BooleanProperty texturesProperty()
    {
        return textures;
    }

    @Override
    public void setTexturesEnabled(boolean textures)
    {
        this.textures.set(textures);
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
    public void setShadowsEnabled(boolean shadows)
    {
        this.shadows.set(shadows);
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
    public void setVisibleLightsEnabled(boolean visibleLights)
    {
        this.visibleLights.set(visibleLights);
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
    public void setGamma(float gamma)
    {
        this.gamma.set(gamma);
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
    public void setWeightExponent(float weightExponent)
    {
        this.weightExponent.set(weightExponent);
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
    public void setIsotropyFactor(float isotropyFactor)
    {
        this.isotropyFactor.set(isotropyFactor);
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
    public void setOcclusionBias(float occlusionBias)
    {
        this.occlusionBias.set(occlusionBias);
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
    public void setWeightMode(ShadingParameterMode weightMode)
    {
        this.weightMode.set(weightMode);
    }

    @Override
    public RenderingMode getRenderingMode()
    {
        return renderingType.get();
    }

    public ObjectProperty<RenderingMode> renderingTypeProperty()
    {
        return renderingType;
    }

    @Override
    public void setRenderingMode(RenderingMode renderingType)
    {
        this.renderingType.set(renderingType);
    }

    @Override
    public boolean isIBREnabled()
    {
        return renderingType.getValue().equals(RenderingMode.IMAGE_BASED_RENDERING);
    }

    @Override
    public void setIBREnabled(boolean ibrEnabled)
    {
        if (ibrEnabled)
        {
            renderingType.setValue(RenderingMode.IMAGE_BASED_RENDERING);
        }
        else if (isIBREnabled())
        {//ibrEnabled == false
            renderingType.setValue(RenderingMode.NONE);
        }
    }

    @Override
    public boolean isHalfResolutionEnabled()
    {
        return halfResolutionEnabled.get();
    }

    @Override
    public void setHalfResolutionEnabled(boolean halfResEnabled)
    {
        this.halfResolutionEnabled.set(halfResEnabled);
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

    @Override
    public void setMultisamplingEnabled(boolean multisamplingEnabled)
    {
        this.multisamplingEnabled.set(multisamplingEnabled);
    }

    public BooleanProperty multisamplingEnabledProperty()
    {
        return this.multisamplingEnabled;
    }
}