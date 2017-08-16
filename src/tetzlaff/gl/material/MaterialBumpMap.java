package tetzlaff.gl.material;

public class MaterialBumpMap extends MaterialScalarMap 
{
    private float bumpMultiplier;

    public MaterialBumpMap()
    {
        bumpMultiplier = 1.0f;
    }

    public float getBumpMultiplier()
    {
        return bumpMultiplier;
    }

    public void setBumpMultiplier(float bumpMultiplier)
    {
        this.bumpMultiplier = bumpMultiplier;
    }
}
