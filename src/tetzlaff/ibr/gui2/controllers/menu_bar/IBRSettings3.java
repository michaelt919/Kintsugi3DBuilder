package tetzlaff.ibr.gui2.controllers.menu_bar;//Created by alexk on 7/31/2017.

import javafx.beans.property.*;
import tetzlaff.ibr.rendering2.to_sort.IBRSettings2;
import tetzlaff.util.ShadingParameterMode;

public class IBRSettings3 implements IBRSettings2{

    public final BooleanProperty occlusion = new SimpleBooleanProperty(true);
    public final BooleanProperty iBR = new SimpleBooleanProperty(true);
    public final BooleanProperty fresnel = new SimpleBooleanProperty(false);
    public final BooleanProperty pBRGeometricAttenuation = new SimpleBooleanProperty(false);
    public final BooleanProperty relighting = new SimpleBooleanProperty(true);
    public final BooleanProperty textures = new SimpleBooleanProperty(false);
    public final BooleanProperty shadows = new SimpleBooleanProperty(false);
    public final BooleanProperty visibleLights = new SimpleBooleanProperty(true);
    public final FloatProperty gamma = new SimpleFloatProperty(2.2f);
    public final FloatProperty weightExponent = new SimpleFloatProperty(16f);
    public final FloatProperty isotropyFactor = new SimpleFloatProperty(0.5f);
    public final FloatProperty occlusionBias = new SimpleFloatProperty(0.0025f);
    public final ObjectProperty<ShadingParameterMode> weightMode = new SimpleObjectProperty<>(ShadingParameterMode.PER_PIXEL);


    @Override
    public float getGamma() {
        return gamma.get();
    }

    @Override
    public float getWeightExponent() {
        return weightExponent.get();
    }

    @Override
    public float getIsotropyFactor() {
        return isotropyFactor.get();
    }

    @Override
    public float getOcclusionBias() {
        return occlusionBias.get();
    }

    @Override
    public ShadingParameterMode getWeightMode() {
        return weightMode.get();
    }

    @Override
    public boolean isOcclusionEnabled() {
        return occlusion.get();
    }

    @Override
    public boolean isIBREnabled() {
        return iBR.get();
    }

    @Override
    public boolean isFresnelEnabled() {
        return fresnel.get();
    }

    @Override
    public boolean isPBRGeometricAttenuationEnabled() {
        return pBRGeometricAttenuation.get();
    }

    @Override
    public boolean isRelightingEnabled() {
        return relighting.get();
    }

    @Override
    public boolean areTexturesEnabled() {
        return textures.get();
    }

    @Override
    public boolean areShadowsEnabled() {
        return shadows.get();
    }

    @Override
    public boolean areVisibleLightsEnabled() {
        return visibleLights.get();
    }





    @Override @Deprecated
    public void setGamma(float gamma) {

    }

    @Override @Deprecated
    public void setWeightExponent(float weightExponent) {

    }

    @Override @Deprecated
    public void setIsotropyFactor(float isotropyFactor) {

    }

    @Override @Deprecated
    public void setOcclusionEnabled(boolean occlusionEnabled) {

    }

    @Override @Deprecated
    public void setOcclusionBias(float occlusionBias) {

    }

    @Override @Deprecated
    public void setIBREnabled(boolean ibrEnabled) {

    }

    @Override @Deprecated
    public void setFresnelEnabled(boolean fresnelEnabled) {

    }

    @Override @Deprecated
    public void setPBRGeometricAttenuationEnabled(boolean pbrGeometricAttenuationEnabled) {

    }

    @Override @Deprecated
    public void setRelightingEnabled(boolean relightingEnabled) {

    }

    @Override @Deprecated
    public void setTexturesEnabled(boolean texturesEnabled) {

    }

    @Override @Deprecated
    public void setShadowsEnabled(boolean shadowsEnabled) {

    }

    @Override @Deprecated
    public void setVisibleLightsEnabled(boolean visibleLightsEnabled) {

    }

    @Override @Deprecated
    public void setWeightMode(ShadingParameterMode weightMode) {

    }
}
