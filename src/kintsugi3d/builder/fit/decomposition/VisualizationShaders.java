package kintsugi3d.builder.fit.decomposition;

import kintsugi3d.builder.state.scene.UserShader;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class VisualizationShaders
{
    public static final String WEIGHT_MAP_GRAYSCALE = "rendermodes/weightmaps/weightmapSingle.frag";
    public static final String WEIGHT_MAP_SUPERIMPOSED = "rendermodes/weightmaps/weightmapOverlay.frag";
    public static final String BASIS_MATERIAL = "rendermodes/basisMaterialSingle.frag";
    public static final String BASIS_MATERIAL_WEIGHTED = "rendermodes/basisMaterialWeightedSingle.frag";

    public static UserShader getForBasisMaterial(String filename, int materialIndex)
    {
        Map<String, Optional<Object>> defines = new HashMap<>(1);
        defines.put("WEIGHTMAP_INDEX", Optional.of(materialIndex));
        return new UserShader(String.format("Palette material %d", materialIndex), filename, defines);
    }
}
