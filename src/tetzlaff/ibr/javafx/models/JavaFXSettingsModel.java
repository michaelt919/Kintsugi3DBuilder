package tetzlaff.ibr.javafx.models;//Created by alexk on 7/31/2017.

import javafx.beans.property.*;
import tetzlaff.ibr.ReadonlySettingsModel;
import tetzlaff.ibr.RenderingMode;
import tetzlaff.ibr.javafx.util.StaticUtilities;
import tetzlaff.ibr.tools.ToolType;
import tetzlaff.util.ShadingParameterMode;

public class JavaFXSettingsModel implements ReadonlySettingsModel
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
    private final BooleanProperty is3DGridEnabled = new SimpleBooleanProperty(false);
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
    public boolean isPhyMasking()
    {
        return phyMasking.get();
    }

    public BooleanProperty phyMaskingProperty()
    {
        return phyMasking;
    }

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
        return pBRGeometricAttenuation.get();
    }

    public BooleanProperty pBRGeometricAttenuationProperty()
    {
        return pBRGeometricAttenuation;
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
    public boolean areTexturesEnabled()
    {
        return textures.get();
    }

    public BooleanProperty texturesProperty()
    {
        return textures;
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

    @Override
    public boolean areLightWidgetsEnabled()
    {
        return JavaFXModels.getInstance().getToolModel().getTool() == ToolType.LIGHT;
    }

    public BooleanProperty visibleLightsProperty()
    {
        return visibleLights;
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
        return renderingType.get();
    }

    public ObjectProperty<RenderingMode> renderingModeProperty()
    {
        return renderingType;
    }

    @Override
    public boolean isIBREnabled()
    {
        return renderingType.getValue().equals(RenderingMode.IMAGE_BASED_RENDERING);
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
}