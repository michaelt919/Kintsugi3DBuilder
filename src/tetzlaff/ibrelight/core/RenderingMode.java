package tetzlaff.ibrelight.core;//Created by alexk on 8/2/2017.

public enum RenderingMode 
{
    NONE(false, false, false, false, false, false),
    WIREFRAME(false, false, false, false, false, false),
    LAMBERTIAN_SHADED(true, false, false, false, false, false),
    SPECULAR_SHADED(true, true, false, false, false, false),
    SOLID_TEXTURED(false, false, true, false, false, false),
    SPECULAR_NORMAL_TEXTURED(true, true, false, true, false, false),
    LAMBERTIAN_DIFFUSE_TEXTURED(true, false, true, true, false, false),
    MATERIAL_SHADED(true, true, true, true, true, false),
    IMAGE_BASED(true, true, false, false, false, true),
    IMAGE_BASED_WITH_MATERIALS(true, true, true, true, true, true);

    private final boolean useDiffuseShading;
    private final boolean useSpecularShading;
    private final boolean useDiffuseTexture;
    private final boolean useNormalTexture;
    private final boolean useSpecularTextures;
    private final boolean imageBased;

    RenderingMode(boolean useDiffuseShading, boolean useSpecularShading, boolean useDiffuseTexture, boolean useNormalTexture, boolean useSpecularTextures, boolean imageBased)
    {
        this.useDiffuseShading = useDiffuseShading;
        this.useSpecularShading = useSpecularShading;
        this.useDiffuseTexture = useDiffuseTexture;
        this.useNormalTexture = useNormalTexture;
        this.useSpecularTextures = useSpecularTextures;
        this.imageBased = imageBased;
    }

    public boolean useDiffuseShading()
    {
        return useDiffuseShading;
    }

    public boolean useSpecularShading()
    {
        return useSpecularShading;
    }

    public boolean useDiffuseTexture()
    {
        return useDiffuseTexture;
    }

    public boolean useNormalTexture()
    {
        return useNormalTexture;
    }

    public boolean useSpecularTextures()
    {
        return useSpecularTextures;
    }

    public boolean isImageBased()
    {
        return imageBased;
    }
}
