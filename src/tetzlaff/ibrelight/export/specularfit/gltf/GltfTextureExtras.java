package tetzlaff.ibrelight.export.specularfit.gltf;

import java.util.HashMap;
import java.util.Map;

public class GltfTextureExtras
{

    private Integer baseRes = null;

    private Map<Integer, Integer> lods = new HashMap<Integer, Integer>();

    public void setLodImageIndex(int resolution, int index)
    {
        lods.put(resolution, index);
    }

    public Integer getLodImageIndex(int resolution)
    {
        return lods.get(resolution);
    }

    public Map<Integer, Integer> getLods()
    {
        return lods;
    }

    public Integer getBaseRes()
    {
        return baseRes;
    }

    public void setBaseRes(Integer baseRes)
    {
        this.baseRes = baseRes;
    }
}
