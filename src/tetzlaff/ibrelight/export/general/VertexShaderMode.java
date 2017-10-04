package tetzlaff.ibrelight.export.general;

public enum VertexShaderMode
{
    TEXTURE_SPACE("Texture space"),
    CAMERA_SPACE("Camera space"),
    CUSTOM("Use custom vertex shader");

    private final String description;

    VertexShaderMode(String description)
    {
        this.description = description;
    }

    @Override
    public String toString()
    {
        return description;
    }
}
