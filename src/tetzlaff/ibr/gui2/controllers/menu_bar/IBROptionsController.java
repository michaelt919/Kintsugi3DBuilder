package tetzlaff.ibr.gui2.controllers.menu_bar;//Created by alexk on 7/31/2017.

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Spinner;
import tetzlaff.ibr.rendering2.to_sort.IBRSettings2;
import tetzlaff.util.ShadingParameterMode;

import java.net.URL;
import java.util.ResourceBundle;

public class IBROptionsController implements Initializable, IBRSettings2 {

    @FXML private Spinner<Double> gammaSpinner;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
//        gammaSpinner.set
    }

    @Override
    public float getGamma() {
        return 0;
    }

    @Override
    public float getWeightExponent() {
        return 0;
    }

    @Override
    public float getIsotropyFactor() {
        return 0;
    }

    @Override
    public float getOcclusionBias() {
        return 0;
    }

    @Override
    public ShadingParameterMode getWeightMode() {
        return null;
    }

    @Override
    public boolean isOcclusionEnabled() {
        return false;
    }

    @Override
    public boolean isIBREnabled() {
        return false;
    }

    @Override
    public boolean isFresnelEnabled() {
        return false;
    }

    @Override
    public boolean isPBRGeometricAttenuationEnabled() {
        return false;
    }

    @Override
    public boolean isRelightingEnabled() {
        return false;
    }

    @Override
    public boolean areTexturesEnabled() {
        return false;
    }

    @Override
    public boolean areShadowsEnabled() {
        return false;
    }

    @Override
    public boolean areVisibleLightsEnabled() {
        return false;
    }

    @Override
    public void setGamma(float gamma) {

    }

    @Override
    public void setWeightExponent(float weightExponent) {

    }

    @Override
    public void setIsotropyFactor(float isotropyFactor) {

    }

    @Override
    public void setOcclusionEnabled(boolean occlusionEnabled) {

    }

    @Override
    public void setOcclusionBias(float occlusionBias) {

    }

    @Override
    public void setIBREnabled(boolean ibrEnabled) {

    }

    @Override
    public void setFresnelEnabled(boolean fresnelEnabled) {

    }

    @Override
    public void setPBRGeometricAttenuationEnabled(boolean pbrGeometricAttenuationEnabled) {

    }

    @Override
    public void setRelightingEnabled(boolean relightingEnabled) {

    }

    @Override
    public void setTexturesEnabled(boolean texturesEnabled) {

    }

    @Override
    public void setShadowsEnabled(boolean shadowsEnabled) {

    }

    @Override
    public void setVisibleLightsEnabled(boolean visibleLightsEnabled) {

    }

    @Override
    public void setWeightMode(ShadingParameterMode weightMode) {

    }
}
