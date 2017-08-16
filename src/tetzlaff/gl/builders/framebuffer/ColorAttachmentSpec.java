package tetzlaff.gl.builders.framebuffer;

import tetzlaff.gl.ColorFormat;

public class ColorAttachmentSpec extends AttachmentSpec
{
    public final ColorFormat internalFormat;

    private ColorAttachmentSpec(ColorFormat internalFormat)
    {
        this.internalFormat = internalFormat;
    }

    public static ColorAttachmentSpec createWithInternalFormat(ColorFormat internalFormat)
    {
        return new ColorAttachmentSpec(internalFormat);
    }

    @Override
    public ColorAttachmentSpec setMultisamples(int samples, boolean fixedSampleLocations)
    {
        super.setMultisamples(samples, fixedSampleLocations);
        return this;
    }

    @Override
    public ColorAttachmentSpec setMipmapsEnabled(boolean enabled)
    {
        super.setMipmapsEnabled(enabled);
        return this;
    }

    @Override
    public ColorAttachmentSpec setLinearFilteringEnabled(boolean enabled)
    {
        super.setLinearFilteringEnabled(enabled);
        return this;
    }
}
