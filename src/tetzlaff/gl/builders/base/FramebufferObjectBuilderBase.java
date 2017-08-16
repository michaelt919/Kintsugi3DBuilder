package tetzlaff.gl.builders.base;

import java.util.ArrayList;
import java.util.List;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.builders.TextureBuilder;
import tetzlaff.gl.builders.framebuffer.*;

public abstract class FramebufferObjectBuilderBase<ContextType extends Context<ContextType>> implements FramebufferObjectBuilder<ContextType>
{
    protected final ContextType context;
    protected final int width;
    protected final int height;

    private final List<TextureBuilder<ContextType, ? extends Texture2D<ContextType>>> colorAttachmentFormats =
            new ArrayList<TextureBuilder<ContextType, ? extends Texture2D<ContextType>>>();

    private TextureBuilder<? super ContextType, ? extends Texture2D<? super ContextType>> depthAttachmentBuilder = null;
    private TextureBuilder<? super ContextType, ? extends Texture2D<? super ContextType>> stencilAttachmentBuilder = null;
    private TextureBuilder<? super ContextType, ? extends Texture2D<? super ContextType>> depthStencilAttachmentBuilder = null;

    protected int getColorAttachmentCount()
    {
        return colorAttachmentFormats.size();
    }

    protected TextureBuilder<? super ContextType, ? extends Texture2D<? super ContextType>> getColorAttachmentBuilder(int index)
    {
        return colorAttachmentFormats.get(index);
    }

    protected boolean hasDepthAttachment()
    {
        return this.depthAttachmentBuilder != null;
    }

    protected boolean hasStencilAttachment()
    {
        return this.stencilAttachmentBuilder != null;
    }

    protected boolean hasCombinedDepthStencilAttachment()
    {
        return this.depthStencilAttachmentBuilder != null;
    }

    protected TextureBuilder<? super ContextType, ? extends Texture2D<? super ContextType>> getDepthAttachmentBuilder()
    {
        return this.depthAttachmentBuilder;
    }

    protected TextureBuilder<? super ContextType, ? extends Texture2D<? super ContextType>> getStencilAttachmentBuilder()
    {
        return this.stencilAttachmentBuilder;
    }

    protected TextureBuilder<? super ContextType, ? extends Texture2D<? super ContextType>> getDepthStencilAttachmentBuilder()
    {
        return this.depthStencilAttachmentBuilder;
    }

    protected FramebufferObjectBuilderBase(ContextType context, int width, int height)
    {
        this.context = context;
        this.width = width;
        this.height = height;
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addEmptyColorAttachment()
    {
        this.colorAttachmentFormats.add(null);
        return this;
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addColorAttachment()
    {
        return this.addColorAttachment(ColorFormat.RGBA8);
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addColorAttachment(ColorFormat format)
    {
        if (format == null)
        {
            this.colorAttachmentFormats.add(null);
        }
        else
        {
            this.colorAttachmentFormats.add(context.build2DColorTexture(this.width, this.height).setInternalFormat(format));
        }
        return this;
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addColorAttachment(ColorAttachmentSpec builder)
    {
        this.colorAttachmentFormats.add(context.build2DColorTexture(width, height)
                .setInternalFormat(builder.internalFormat)
                .setMultisamples(builder.getMultisamples(), builder.areMultisampleLocationsFixed())
                .setMipmapsEnabled(builder.areMipmapsEnabled())
                .setLinearFilteringEnabled(builder.isLinearFilteringEnabled()));
        return this;
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addEmptyColorAttachments(int count)
    {
        for (int i = 0; i < count; i++)
        {
            this.addEmptyColorAttachment();
        }
        return this;
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addColorAttachments(int count)
    {
        return this.addColorAttachments(ColorFormat.RGBA8, count);
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addColorAttachments(ColorFormat format, int count)
    {
        for (int i = 0; i < count; i++)
        {
            this.addColorAttachment(format);
        }
        return this;
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addColorAttachments(ColorAttachmentSpec builder, int count)
    {
        for (int i = 0; i < count; i++)
        {
            this.addColorAttachment(builder);
        }
        return this;
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addDepthAttachment()
    {
        return this.addDepthAttachment(16, false);
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addDepthAttachment(int precision, boolean floatingPoint)
    {
        if (!this.hasDepthAttachment())
        {
            this.depthAttachmentBuilder = context.build2DDepthTexture(this.width, this.height)
                                            .setInternalPrecision(precision)
                                            .setFloatingPointEnabled(floatingPoint);
            return this;
        }
        else
        {
            throw new IllegalStateException("A depth attachment already exists.");
        }
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addDepthAttachment(DepthAttachmentSpec builder)
    {
        if (!this.hasDepthAttachment() && !this.hasCombinedDepthStencilAttachment())
        {
            this.depthAttachmentBuilder = context.build2DDepthTexture(this.width, this.height)
                                            .setInternalPrecision(builder.precision)
                                            .setFloatingPointEnabled(builder.floatingPoint)
                                            .setMultisamples(builder.getMultisamples(), builder.areMultisampleLocationsFixed())
                                            .setMipmapsEnabled(builder.areMipmapsEnabled())
                                            .setLinearFilteringEnabled(builder.isLinearFilteringEnabled());
            return this;
        }
        else
        {
            throw new IllegalStateException("A depth attachment already exists.");
        }
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addStencilAttachment()
    {
        return this.addStencilAttachment(8);
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addStencilAttachment(int precision)
    {
        if (!this.hasStencilAttachment() && !this.hasCombinedDepthStencilAttachment())
        {
            this.stencilAttachmentBuilder = context.build2DStencilTexture(this.width, this.height).setInternalPrecision(precision);
            return this;
        }
        else
        {
            throw new IllegalStateException("A stencil attachment already exists.");
        }
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addStencilAttachment(StencilAttachmentSpec builder)
    {
        if (!this.hasStencilAttachment() && !this.hasCombinedDepthStencilAttachment())
        {
            this.stencilAttachmentBuilder = context.build2DStencilTexture(this.width, this.height)
                                                .setInternalPrecision(builder.precision)
                                                .setMultisamples(builder.getMultisamples(), builder.areMultisampleLocationsFixed())
                                                .setMipmapsEnabled(builder.areMipmapsEnabled())
                                                .setLinearFilteringEnabled(builder.isLinearFilteringEnabled());
            return this;
        }
        else
        {
            throw new IllegalStateException("A stencil attachment already exists.");
        }
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addCombinedDepthStencilAttachment()
    {
        if (!this.hasDepthAttachment() && !this.hasStencilAttachment() && !this.hasCombinedDepthStencilAttachment())
        {
            this.depthStencilAttachmentBuilder = context.build2DDepthStencilTexture(this.width, this.height);
            return this;
        }
        else
        {
            throw new IllegalStateException("A depth or stencil attachment already exists.");
        }
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addCombinedDepthStencilAttachment(boolean floatingPoint)
    {
        if (!this.hasDepthAttachment() && !this.hasStencilAttachment() && !this.hasCombinedDepthStencilAttachment())
        {
            this.depthStencilAttachmentBuilder = context.build2DDepthStencilTexture(this.width, this.height).setFloatingPointDepthEnabled(floatingPoint);
            return this;
        }
        else
        {
            throw new IllegalStateException("A depth or stencil attachment already exists.");
        }
    }

    @Override
    public FramebufferObjectBuilder<ContextType> addCombinedDepthStencilAttachment(DepthStencilAttachmentSpec builder)
    {
        if (!this.hasDepthAttachment() && !this.hasStencilAttachment() && !this.hasCombinedDepthStencilAttachment())
        {
            this.depthStencilAttachmentBuilder = context.build2DDepthStencilTexture(this.width, this.height)
                                                    .setFloatingPointDepthEnabled(builder.floatingPointDepth)
                                                    .setMultisamples(builder.getMultisamples(), builder.areMultisampleLocationsFixed())
                                                    .setMipmapsEnabled(builder.areMipmapsEnabled())
                                                    .setLinearFilteringEnabled(builder.isLinearFilteringEnabled());
            return this;
        }
        else
        {
            throw new IllegalStateException("A depth or stencil attachment already exists.");
        }
    }
}
