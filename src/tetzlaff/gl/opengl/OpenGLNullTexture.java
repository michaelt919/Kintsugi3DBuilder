package tetzlaff.gl.opengl;

import tetzlaff.gl.core.SamplerType;
import tetzlaff.gl.core.TextureType;

public class OpenGLNullTexture extends OpenGLTexture
{
    private SamplerType type;

    OpenGLNullTexture(OpenGLContext context, SamplerType type)
    {
        super(context, TextureType.NULL);
        this.type = type;
    }

    @Override
    int getOpenGLTextureTarget()
    {
        return OpenGLTexture.translateSamplerType(type);
    }

    @Override
    public int getMipmapLevelCount()
    {
        return 0;
    }
}
